/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.mindseye.opt;

import com.simiacryptus.lang.TimedResult;
import com.simiacryptus.lang.UncheckedSupplier;
import com.simiacryptus.mindseye.eval.*;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.layers.StochasticComponent;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.line.ArmijoWolfeSearch;
import com.simiacryptus.mindseye.opt.line.FailsafeLineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchStrategy;
import com.simiacryptus.mindseye.opt.orient.LBFGS;
import com.simiacryptus.mindseye.opt.orient.OrientationStrategy;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.FastRandom;
import com.simiacryptus.util.Util;
import com.simiacryptus.util.data.DoubleStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * The type Validating trainer.
 */
public class ValidatingTrainer extends ReferenceCountingBase {

  private final AtomicInteger disappointments = new AtomicInteger(0);
  @Nonnull
  private final RefList<TrainingPhase> regimen;
  private final AtomicLong trainingMeasurementTime = new AtomicLong(0);
  private final AtomicLong validatingMeasurementTime = new AtomicLong(0);
  @Nonnull
  private final Trainable validationSubject;
  private double adjustmentFactor = 0.5;
  private double adjustmentTolerance = 0.1;
  private AtomicInteger currentIteration = new AtomicInteger(0);
  private int disappointmentThreshold = 0;
  private int epochIterations = 1;
  private int improvmentStaleThreshold = 3;
  private int maxEpochIterations = 20;
  private int maxIterations = Integer.MAX_VALUE;
  private int maxTrainingSize = Integer.MAX_VALUE;
  private int minEpochIterations = 1;
  private int minTrainingSize = 100;
  private TrainingMonitor monitor = new TrainingMonitor();
  private double overtrainingTarget = 2;
  private double pessimism = 10;
  private double terminateThreshold;
  private Duration timeout;
  private int trainingSize = 10000;
  private double trainingTarget = 0.7;

  /**
   * Instantiates a new Validating trainer.
   *
   * @param trainingSubject   the training subject
   * @param validationSubject the validation subject
   */
  public ValidatingTrainer(@Nonnull final SampledTrainable trainingSubject,
                           @Nonnull final Trainable validationSubject) {
    RefList<TrainingPhase> temp_07_0001 = new RefArrayList<TrainingPhase>(RefArrays
        .asList(new TrainingPhase(new PerformanceWrapper(trainingSubject.addRef(),
            ValidatingTrainer.this.addRef()))));
    regimen = temp_07_0001.addRef();
    temp_07_0001.freeRef();
    Trainable temp_07_0002 = new TrainableBase() {
      {
        validationSubject.addRef();
      }

      @Override
      public Layer getLayer() {
        return validationSubject.getLayer();
      }

      @Override
      public PointSample measure(final TrainingMonitor monitor) {
        @Nonnull final TimedResult<PointSample> time = TimedResult.time(RefUtil.wrapInterface(
            (UncheckedSupplier<PointSample>) () -> validationSubject.measure(monitor), validationSubject.addRef()));
        validatingMeasurementTime.addAndGet(time.timeNanos);
        PointSample result = time.getResult();
        time.freeRef();
        return result;
      }

      @Override
      public boolean reseed(final long seed) {
        return validationSubject.reseed(seed);
      }

      public void _free() {
        super._free();
        validationSubject.freeRef();
      }
    };
    this.validationSubject = temp_07_0002.addRef();
    temp_07_0002.freeRef();
    validationSubject.freeRef();
    trainingSize = trainingSubject.getTrainingSize();
    trainingSubject.freeRef();
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = Double.NEGATIVE_INFINITY;
  }

  /**
   * Gets adjustment factor.
   *
   * @return the adjustment factor
   */
  public double getAdjustmentFactor() {
    return adjustmentFactor;
  }

  /**
   * Sets adjustment factor.
   *
   * @param adjustmentFactor the adjustment factor
   */
  public void setAdjustmentFactor(double adjustmentFactor) {
    this.adjustmentFactor = adjustmentFactor;
  }

  /**
   * Gets adjustment tolerance.
   *
   * @return the adjustment tolerance
   */
  public double getAdjustmentTolerance() {
    return adjustmentTolerance;
  }

  /**
   * Sets adjustment tolerance.
   *
   * @param adjustmentTolerance the adjustment tolerance
   */
  public void setAdjustmentTolerance(double adjustmentTolerance) {
    this.adjustmentTolerance = adjustmentTolerance;
  }

  /**
   * Gets current iteration.
   *
   * @return the current iteration
   */
  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  /**
   * Sets current iteration.
   *
   * @param currentIteration the current iteration
   */
  public void setCurrentIteration(AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
  }

  /**
   * Gets disappointment threshold.
   *
   * @return the disappointment threshold
   */
  public int getDisappointmentThreshold() {
    return disappointmentThreshold;
  }

  /**
   * Sets disappointment threshold.
   *
   * @param disappointmentThreshold the disappointment threshold
   */
  public void setDisappointmentThreshold(final int disappointmentThreshold) {
    this.disappointmentThreshold = disappointmentThreshold;
  }

  /**
   * Gets epoch iterations.
   *
   * @return the epoch iterations
   */
  public int getEpochIterations() {
    return epochIterations;
  }

  /**
   * Sets epoch iterations.
   *
   * @param epochIterations the epoch iterations
   */
  public void setEpochIterations(int epochIterations) {
    this.epochIterations = epochIterations;
  }

  /**
   * Gets improvment stale threshold.
   *
   * @return the improvment stale threshold
   */
  public int getImprovmentStaleThreshold() {
    return improvmentStaleThreshold;
  }

  /**
   * Sets improvment stale threshold.
   *
   * @param improvmentStaleThreshold the improvment stale threshold
   */
  public void setImprovmentStaleThreshold(final int improvmentStaleThreshold) {
    this.improvmentStaleThreshold = improvmentStaleThreshold;
  }

  /**
   * Gets max epoch iterations.
   *
   * @return the max epoch iterations
   */
  public int getMaxEpochIterations() {
    return maxEpochIterations;
  }

  /**
   * Sets max epoch iterations.
   *
   * @param maxEpochIterations the max epoch iterations
   */
  public void setMaxEpochIterations(int maxEpochIterations) {
    this.maxEpochIterations = maxEpochIterations;
  }

  /**
   * Gets max iterations.
   *
   * @return the max iterations
   */
  public int getMaxIterations() {
    return maxIterations;
  }

  /**
   * Sets max iterations.
   *
   * @param maxIterations the max iterations
   */
  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  /**
   * Gets max training size.
   *
   * @return the max training size
   */
  public int getMaxTrainingSize() {
    return maxTrainingSize;
  }

  /**
   * Sets max training size.
   *
   * @param maxTrainingSize the max training size
   */
  public void setMaxTrainingSize(int maxTrainingSize) {
    this.maxTrainingSize = maxTrainingSize;
  }

  /**
   * Gets min epoch iterations.
   *
   * @return the min epoch iterations
   */
  public int getMinEpochIterations() {
    return minEpochIterations;
  }

  /**
   * Sets min epoch iterations.
   *
   * @param minEpochIterations the min epoch iterations
   */
  public void setMinEpochIterations(int minEpochIterations) {
    this.minEpochIterations = minEpochIterations;
  }

  /**
   * Gets min training size.
   *
   * @return the min training size
   */
  public int getMinTrainingSize() {
    return minTrainingSize;
  }

  /**
   * Sets min training size.
   *
   * @param minTrainingSize the min training size
   */
  public void setMinTrainingSize(int minTrainingSize) {
    this.minTrainingSize = minTrainingSize;
  }

  /**
   * Gets monitor.
   *
   * @return the monitor
   */
  public TrainingMonitor getMonitor() {
    return monitor;
  }

  /**
   * Sets monitor.
   *
   * @param monitor the monitor
   */
  public void setMonitor(TrainingMonitor monitor) {
    this.monitor = monitor;
  }

  /**
   * Gets overtraining target.
   *
   * @return the overtraining target
   */
  public double getOvertrainingTarget() {
    return overtrainingTarget;
  }

  /**
   * Sets overtraining target.
   *
   * @param overtrainingTarget the overtraining target
   */
  public void setOvertrainingTarget(double overtrainingTarget) {
    this.overtrainingTarget = overtrainingTarget;
  }

  /**
   * Gets pessimism.
   *
   * @return the pessimism
   */
  public double getPessimism() {
    return pessimism;
  }

  /**
   * Sets pessimism.
   *
   * @param pessimism the pessimism
   */
  public void setPessimism(double pessimism) {
    this.pessimism = pessimism;
  }

  /**
   * Gets regimen.
   *
   * @return the regimen
   */
  @Nonnull
  public RefList<TrainingPhase> getRegimen() {
    return regimen.addRef();
  }

  /**
   * Gets terminate threshold.
   *
   * @return the terminate threshold
   */
  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  /**
   * Sets terminate threshold.
   *
   * @param terminateThreshold the terminate threshold
   */
  public void setTerminateThreshold(double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
  }

  /**
   * Gets timeout.
   *
   * @return the timeout
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Sets timeout.
   *
   * @param timeout the timeout
   */
  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * Gets training size.
   *
   * @return the training size
   */
  public int getTrainingSize() {
    return trainingSize;
  }

  /**
   * Sets training size.
   *
   * @param trainingSize the training size
   */
  public void setTrainingSize(int trainingSize) {
    this.trainingSize = trainingSize;
  }

  /**
   * Gets training target.
   *
   * @return the training target
   */
  public double getTrainingTarget() {
    return trainingTarget;
  }

  /**
   * Sets training target.
   *
   * @param trainingTarget the training target
   */
  public void setTrainingTarget(double trainingTarget) {
    this.trainingTarget = trainingTarget;
  }

  /**
   * Gets validation subject.
   *
   * @return the validation subject
   */
  @Nonnull
  public Trainable getValidationSubject() {
    return validationSubject.addRef();
  }

  /**
   * Sets line search factory.
   *
   * @param lineSearchFactory the line search factory
   */
  public void setLineSearchFactory(Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    RefList<TrainingPhase> temp_07_0024 = getRegimen();
    TrainingPhase temp_07_0025 = temp_07_0024.get(0);
    temp_07_0025.setLineSearchFactory(lineSearchFactory);
    temp_07_0025.freeRef();
    temp_07_0024.freeRef();
  }

  /**
   * Sets orientation.
   *
   * @param orientation the orientation
   */
  public void setOrientation(@Nullable OrientationStrategy<?> orientation) {
    RefList<TrainingPhase> temp_07_0026 = getRegimen();
    TrainingPhase temp_07_0027 = temp_07_0026.get(0);
    final OrientationStrategy<?> orientation1 = orientation == null ? null : orientation.addRef();
    temp_07_0027.setOrientation(orientation1);
    temp_07_0027.freeRef();
    temp_07_0026.freeRef();
    if (null != orientation)
      orientation.freeRef();
  }

  @Nonnull
  private static CharSequence getId(@Nonnull final DoubleBuffer<UUID> x) {
    String temp_07_0023 = x.key.toString();
    x.freeRef();
    return temp_07_0023;
  }

  /**
   * Run double.
   *
   * @return the double
   */
  public double run() {
    Layer validationSubjectLayer = validationSubject.getLayer();
    try {
      final long timeoutAt = RefSystem.currentTimeMillis() + timeout.toMillis();
      if (validationSubjectLayer instanceof DAGNetwork) {
        ((DAGNetwork) validationSubjectLayer).visitLayers(layer -> {
          if (layer instanceof StochasticComponent)
            ((StochasticComponent) layer).clearNoise();
          if (null != layer)
            layer.freeRef();
        });
      }
      @Nonnull final EpochParams epochParams = new EpochParams(timeoutAt, epochIterations, getTrainingSize(),
          validationSubject.measure(monitor));
      int epochNumber = 0;
      int iterationNumber = 0;
      int lastImprovement = 0;
      double lowestValidation = Double.POSITIVE_INFINITY;
      while (true) {
        if (shouldHalt(monitor, timeoutAt)) {
          monitor.log("Training halted");
          break;
        }
        monitor.log(RefString.format("Epoch parameters: %s, %s", epochParams.trainingSize, epochParams.iterations));
        @Nonnull final RefList<TrainingPhase> regimen = getRegimen();
        final long seed = RefSystem.nanoTime();
        final RefList<EpochResult> epochResults = RefIntStream.range(0, regimen.size())
            .mapToObj(RefUtil.wrapInterface((IntFunction<? extends ValidatingTrainer.EpochResult>) i -> {
              RefList<ValidatingTrainer.TrainingPhase> temp_07_0028 = getRegimen();
              final TrainingPhase phase = temp_07_0028.get(i);
              temp_07_0028.freeRef();
              ValidatingTrainer.EpochResult temp_07_0012 = runPhase(epochParams.addRef(),
                  phase == null ? null : phase.addRef(), i, seed);
              if (null != phase)
                phase.freeRef();
              return temp_07_0012;
            }, epochParams.addRef())).collect(RefCollectors.toList());
        regimen.freeRef();
        final EpochResult primaryPhase = epochResults.get(0);
        epochResults.freeRef();
        iterationNumber += primaryPhase.iterations;
        assert primaryPhase.currentPoint != null;
        final double trainingDelta = primaryPhase.currentPoint.getMean() / primaryPhase.priorMean;
        if (validationSubjectLayer instanceof DAGNetwork) {
          ((DAGNetwork) validationSubjectLayer).visitLayers(layer -> {
            if (layer instanceof StochasticComponent)
              ((StochasticComponent) layer).clearNoise();
            if (null != layer)
              layer.freeRef();
          });
        }
        final PointSample currentValidation = validationSubject.measure(monitor);
        assert epochParams.validation != null;
        final double overtraining = Math.log(trainingDelta)
            / Math.log(currentValidation.getMean() / epochParams.validation.getMean());
        final double validationDelta = currentValidation.getMean() / epochParams.validation.getMean();
        final double adj1 = Math.pow(Math.log(getTrainingTarget()) / Math.log(validationDelta), adjustmentFactor);
        final double adj2 = Math.pow(overtraining / getOvertrainingTarget(), adjustmentFactor);
        final double validationMean = currentValidation.getMean();
        if (validationMean < lowestValidation) {
          lowestValidation = validationMean;
          lastImprovement = iterationNumber;
        }
        monitor.log(RefString.format(
            "Epoch %d result apply %s iterations, %s/%s samples: {validation *= 2^%.5f; training *= 2^%.3f; Overtraining = %.2f}, {itr*=%.2f, len*=%.2f} %s since improvement; %.4f validation time",
            ++epochNumber, primaryPhase.iterations, epochParams.trainingSize, getMaxTrainingSize(),
            Math.log(validationDelta) / Math.log(2), Math.log(trainingDelta) / Math.log(2), overtraining, adj1, adj2,
            iterationNumber - lastImprovement, validatingMeasurementTime.getAndSet(0) / 1e9));
        if (!primaryPhase.continueTraining) {
          monitor.log(RefString.format("Training %d runPhase halted", epochNumber));
          break;
        }
        if (epochParams.trainingSize >= getMaxTrainingSize()) {
          final double roll = FastRandom.INSTANCE.random();
          if (roll > Math.pow(2 - validationDelta, pessimism)) {
            monitor.log(RefString.format("Training randomly converged: %3f", roll));
            break;
          } else {
            if (iterationNumber - lastImprovement > improvmentStaleThreshold) {
              if (disappointments.incrementAndGet() > getDisappointmentThreshold()) {
                monitor
                    .log(RefString.format("Training converged after %s iterations", iterationNumber - lastImprovement));
                break;
              } else {
                monitor.log(RefString.format("Training failed to converged on %s attempt after %s iterations",
                    disappointments.get(), iterationNumber - lastImprovement));
              }
            } else {
              disappointments.set(0);
            }
          }
        }
        if (validationDelta < 1.0 && trainingDelta < 1.0) {
          if (adj1 < 1 - adjustmentTolerance || adj1 > 1 + adjustmentTolerance) {
            epochParams.iterations = Math.max(getMinEpochIterations(),
                Math.min(getMaxEpochIterations(), (int) (primaryPhase.iterations * adj1)));
          }
          if (adj2 < 1 + adjustmentTolerance || adj2 > 1 - adjustmentTolerance) {
            epochParams.trainingSize = Math.max(0,
                Math.min(
                    Math.max(getMinTrainingSize(),
                        Math.min(getMaxTrainingSize(), (int) (epochParams.trainingSize * adj2))),
                    epochParams.trainingSize));
          }
        } else {
          epochParams.trainingSize = Math.max(0,
              Math.min(Math.max(getMinTrainingSize(), Math.min(getMaxTrainingSize(), epochParams.trainingSize * 5)),
                  epochParams.trainingSize));
          epochParams.iterations = 1;
        }
        primaryPhase.freeRef();
        epochParams.validation = currentValidation.addRef();
        currentValidation.freeRef();
      }
      if (validationSubjectLayer instanceof DAGNetwork) {
        ((DAGNetwork) validationSubjectLayer).visitLayers(layer -> {
          if (layer instanceof StochasticComponent)
            ((StochasticComponent) layer).clearNoise();
          if (null != layer)
            layer.freeRef();
        });
      }
      assert epochParams.validation != null;
      double temp_07_0011 = epochParams.validation.getMean();
      epochParams.freeRef();
      return temp_07_0011;
    } catch (@Nonnull final Throwable e) {
      throw Util.throwException(e);
    } finally {
      if (null != validationSubjectLayer) validationSubjectLayer.freeRef();
    }
  }

  /**
   * Sets timeout.
   *
   * @param number the number
   * @param units  the units
   */
  public void setTimeout(int number, @Nonnull TemporalUnit units) {
    timeout = Duration.of(number, units);
  }

  /**
   * Sets timeout.
   *
   * @param number the number
   * @param units  the units
   */
  public void setTimeout(int number, @Nonnull TimeUnit units) {
    setTimeout(number, Util.cvt(units));
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    validationSubject.freeRef();
    regimen.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ValidatingTrainer addRef() {
    return (ValidatingTrainer) super.addRef();
  }

  /**
   * Run phase epoch result.
   *
   * @param epochParams the epoch params
   * @param phase       the phase
   * @param i           the
   * @param seed        the seed
   * @return the epoch result
   */
  @Nonnull
  protected EpochResult runPhase(@Nonnull final EpochParams epochParams, @Nonnull final TrainingPhase phase,
                                 final int i, final long seed) {
    monitor.log(RefString.format("Phase %d: %s", i, phase.addRef()));
    assert phase.trainingSubject != null;
    phase.trainingSubject.setTrainingSize(epochParams.trainingSize);
    monitor.log(RefString.format("resetAndMeasure; trainingSize=%s", epochParams.trainingSize));
    reset(phase.addRef(), seed);
    ValidatingTrainer temp_07_0029 = this.addRef();
    PointSample currentPoint = temp_07_0029.measure(phase.addRef());
    temp_07_0029.freeRef();
    final double pointMean = currentPoint.getMean();
    assert 0 < currentPoint.delta.size() : "Nothing to optimize";
    int step = 1;
    for (; step <= epochParams.iterations || epochParams.iterations <= 0; step++) {
      if (shouldHalt(monitor, epochParams.timeoutMs)) {
        ValidatingTrainer.EpochResult temp_07_0014 = new EpochResult(false, pointMean,
            currentPoint.addRef(), step);
        currentPoint.freeRef();
        epochParams.freeRef();
        phase.freeRef();
        return temp_07_0014;
      }
      final long startTime = RefSystem.nanoTime();
      final long prevGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
          .mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
      @Nonnull final StepResult epoch = runStep(currentPoint.addRef(),
          phase.addRef());
      final long newGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
          .mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
      final long endTime = RefSystem.nanoTime();
      final CharSequence performance = RefString.format(
          "%s in %.3f seconds; %.3f in orientation, %.3f in gc, %.3f in line search; %.3f trainAll time",
          epochParams.trainingSize, (endTime - startTime) / 1e9, epoch.performance[0], (newGcTime - prevGcTime) / 1e3,
          epoch.performance[1], trainingMeasurementTime.getAndSet(0) / 1e9);
      currentPoint.freeRef();
      epoch.currentPoint.setRate(0.0);
      currentPoint = epoch.currentPoint.addRef();
      if (epoch.previous.getMean() <= epoch.currentPoint.getMean()) {
        monitor.log(RefString.format("Iteration %s failed, aborting. Error: %s (%s)", currentIteration.get(),
            epoch.currentPoint.getMean(), performance));
        epoch.freeRef();
        epochParams.freeRef();
        phase.freeRef();
        return new EpochResult(false, pointMean, currentPoint, step);
      } else {
        monitor.log(RefString.format("Iteration %s complete. Error: %s (%s)", currentIteration.get(),
            epoch.currentPoint.getMean(), performance));
      }
      epoch.freeRef();
      monitor.onStepComplete(new Step(currentPoint.addRef(), currentIteration.get()));
    }
    phase.freeRef();
    epochParams.freeRef();
    return new EpochResult(true, pointMean, currentPoint, step);
  }

  /**
   * Run step step result.
   *
   * @param previousPoint the previous point
   * @param phase         the phase
   * @return the step result
   */
  @Nonnull
  protected StepResult runStep(@Nonnull final PointSample previousPoint, @Nonnull final TrainingPhase phase) {
    currentIteration.incrementAndGet();
    @Nonnull final TimedResult<LineSearchCursor> timedOrientation = TimedResult.time(RefUtil.wrapInterface(
        (UncheckedSupplier<LineSearchCursor>) () -> {
          assert phase.trainingSubject != null;
          assert phase.orientation != null;
          return phase.orientation.orient(phase.trainingSubject.addRef(),
              previousPoint.addRef(), monitor);
        },
        previousPoint.addRef(), phase.addRef()));
    final LineSearchCursor direction = timedOrientation.getResult();
    final CharSequence directionType = direction.getDirectionType();
    LineSearchStrategy lineSearchStrategy;
    assert phase.lineSearchStrategyMap != null;
    if (phase.lineSearchStrategyMap.containsKey(directionType)) {
      lineSearchStrategy = phase.lineSearchStrategyMap.get(directionType);
    } else {
      monitor.log(RefString.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = phase.lineSearchFactory.apply(direction.getDirectionType());
      RefUtil.freeRef(phase.lineSearchStrategyMap.put(directionType, lineSearchStrategy));
    }
    phase.freeRef();
    @Nonnull final TimedResult<PointSample> timedLineSearch = TimedResult
        .time(RefUtil.wrapInterface((UncheckedSupplier<PointSample>) () -> {
          @Nonnull final FailsafeLineSearchCursor cursor = new FailsafeLineSearchCursor(
              direction.addRef(), previousPoint.addRef(),
              monitor);
          assert lineSearchStrategy != null;
          RefUtil.freeRef(lineSearchStrategy.step(cursor.addRef(), monitor));
          PointSample temp_07_0031 = cursor.getBest();
          assert temp_07_0031 != null;
          temp_07_0031.restore();
          PointSample temp_07_0016 = temp_07_0031.addRef();
          temp_07_0031.freeRef();
          cursor.freeRef();
          //cursor.step(restore.rate, monitor);
          return temp_07_0016;
        }, previousPoint.addRef(), direction.addRef()));
    direction.freeRef();
    final PointSample bestPoint = timedLineSearch.getResult();
    if (bestPoint.getMean() > previousPoint.getMean()) {
      IllegalStateException temp_07_0018 = new IllegalStateException(
          bestPoint.getMean() + " > " + previousPoint.getMean());
      bestPoint.freeRef();
      previousPoint.freeRef();
      timedOrientation.freeRef();
      timedLineSearch.freeRef();
      throw temp_07_0018;
    }
    monitor.log(
        compare(previousPoint.addRef(), bestPoint.addRef()));
    ValidatingTrainer.StepResult temp_07_0017 = new StepResult(previousPoint,
        bestPoint.addRef(),
        new double[]{timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9});
    timedLineSearch.freeRef();
    timedOrientation.freeRef();
    bestPoint.freeRef();
    return temp_07_0017;
  }

  /**
   * Should halt boolean.
   *
   * @param monitor   the monitor
   * @param timeoutMs the timeout ms
   * @return the boolean
   */
  protected boolean shouldHalt(@Nonnull final TrainingMonitor monitor, final long timeoutMs) {
    RefSystem.currentTimeMillis();
    if (timeoutMs < RefSystem.currentTimeMillis()) {
      monitor.log("Training timeout");
      return true;
    } else if (currentIteration.get() > maxIterations) {
      monitor.log("Training iteration overflow");
      return true;
    } else {
      return false;
    }
  }

  @Nonnull
  private String compare(@Nonnull final PointSample previousPoint, @Nonnull final PointSample nextPoint) {
    @Nonnull final StateSet<UUID> nextWeights = nextPoint.weights.addRef();
    nextPoint.freeRef();
    @Nonnull final StateSet<UUID> prevWeights = previousPoint.weights.addRef();
    previousPoint.freeRef();
    RefMap<State<UUID>, RefList<State<UUID>>> temp_07_0032 = prevWeights.stream()
        .collect(RefCollectors.groupingBy(x -> {
          return x;
        }, RefCollectors.toList()));
    RefSet<Map.Entry<State<UUID>, RefList<State<UUID>>>> temp_07_0033 = temp_07_0032.entrySet();
    RefMap<State<UUID>, String> temp_07_0036 = temp_07_0033.stream().collect(RefCollectors.toMap(x -> {
      State<UUID> temp_07_0020 = x.getKey();
      RefUtil.freeRef(x);
      return temp_07_0020;
    }, RefUtil
        .wrapInterface((Function<? super Map.Entry<State<UUID>, RefList<State<UUID>>>, ? extends String>) list -> {
          RefList<State<UUID>> temp_07_0034 = list.getValue();
          final double[] doubleList = temp_07_0034.stream()
              .mapToDouble(RefUtil.wrapInterface(prevWeight -> {
                final DoubleBuffer<UUID> dirDelta = nextWeights.get(prevWeight.key);
                final double numerator = prevWeight.deltaStatistics().rms();
                prevWeight.freeRef();
                final double denominator = null == dirDelta ? 0 : dirDelta.deltaStatistics().rms();
                if (null != dirDelta)
                  dirDelta.freeRef();
                return numerator / (0 == denominator ? 1 : denominator);
              }, nextWeights.addRef())).toArray();
          temp_07_0034.freeRef();
          RefUtil.freeRef(list);
          if (1 == doubleList.length) {
            return Double.toString(doubleList[0]);
          }
          return new DoubleStatistics().accept(doubleList)
              .toString();
        }, nextWeights)));
    String temp_07_0019 = RefString.format("Overall network state change: %s", temp_07_0036);
    temp_07_0033.freeRef();
    temp_07_0032.freeRef();
    prevWeights.freeRef();
    return temp_07_0019;
  }

  private PointSample measure(@Nonnull final TrainingPhase phase) {
    int retries = 0;
    try {
      do {
        if (10 < retries++) {
          throw new IterativeStopException();
        }
        assert phase.trainingSubject != null;
        final PointSample currentPoint = phase.trainingSubject.measure(monitor);
        if (Double.isFinite(currentPoint.getMean())) {
          return currentPoint;
        }
        currentPoint.freeRef();
        assert phase.orientation != null;
        phase.orientation.reset();
      } while (true);
    } finally {
      phase.freeRef();
    }
  }

  private void reset(@Nonnull TrainingPhase phase, long seed) {
    assert phase.trainingSubject != null;
    if (!phase.trainingSubject.reseed(seed)) {
      phase.freeRef();
      throw new IterativeStopException();
    }
    assert phase.orientation != null;
    phase.orientation.reset();
    phase.trainingSubject.reseed(seed);
    Layer trainingSubjectLayer = phase.trainingSubject.getLayer();
    if (trainingSubjectLayer instanceof DAGNetwork) {
      ((DAGNetwork) trainingSubjectLayer).shuffle(StochasticComponent.random.get().nextLong());
    }
    if (null != trainingSubjectLayer) trainingSubjectLayer.freeRef();
    phase.freeRef();
  }

  /**
   * The type Training phase.
   */
  public static class TrainingPhase extends ReferenceCountingBase {
    private Function<CharSequence, LineSearchStrategy> lineSearchFactory = s -> new ArmijoWolfeSearch();
    @Nullable
    private RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new RefHashMap<>();
    @Nullable
    private OrientationStrategy<?> orientation = new LBFGS();
    @Nullable
    private SampledTrainable trainingSubject;

    /**
     * Instantiates a new Training phase.
     *
     * @param trainingSubject the training subject
     */
    public TrainingPhase(@Nullable final SampledTrainable trainingSubject) {
      setTrainingSubject(trainingSubject == null ? null : trainingSubject.addRef());
      if (null != trainingSubject)
        trainingSubject.freeRef();
    }

    /**
     * Gets line search factory.
     *
     * @return the line search factory
     */
    public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
      return lineSearchFactory;
    }

    /**
     * Sets line search factory.
     *
     * @param lineSearchFactory the line search factory
     */
    public void setLineSearchFactory(Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
      this.lineSearchFactory = lineSearchFactory;
    }

    /**
     * Gets line search strategy map.
     *
     * @return the line search strategy map
     */
    @Nullable
    public RefMap<CharSequence, LineSearchStrategy> getLineSearchStrategyMap() {
      return lineSearchStrategyMap == null ? null : lineSearchStrategyMap.addRef();
    }

    /**
     * Sets line search strategy map.
     *
     * @param lineSearchStrategyMap the line search strategy map
     */
    public void setLineSearchStrategyMap(@Nullable RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap) {
      RefMap<CharSequence, LineSearchStrategy> temp_07_0003 = lineSearchStrategyMap == null ? null
          : lineSearchStrategyMap.addRef();
      if (null != this.lineSearchStrategyMap)
        this.lineSearchStrategyMap.freeRef();
      this.lineSearchStrategyMap = temp_07_0003 == null ? null : temp_07_0003.addRef();
      if (null != temp_07_0003)
        temp_07_0003.freeRef();
      if (null != lineSearchStrategyMap)
        lineSearchStrategyMap.freeRef();
    }

    /**
     * Gets orientation.
     *
     * @return the orientation
     */
    @Nullable
    public OrientationStrategy<?> getOrientation() {
      return orientation == null ? null : orientation.addRef();
    }

    /**
     * Sets orientation.
     *
     * @param orientation the orientation
     */
    public void setOrientation(@Nullable OrientationStrategy<?> orientation) {
      OrientationStrategy<?> temp_07_0004 = orientation == null ? null : orientation.addRef();
      if (null != this.orientation)
        this.orientation.freeRef();
      this.orientation = temp_07_0004 == null ? null : temp_07_0004.addRef();
      if (null != temp_07_0004)
        temp_07_0004.freeRef();
      if (null != orientation)
        orientation.freeRef();
    }

    /**
     * Gets training subject.
     *
     * @return the training subject
     */
    @Nullable
    public SampledTrainable getTrainingSubject() {
      return trainingSubject == null ? null : trainingSubject.addRef();
    }

    /**
     * Sets training subject.
     *
     * @param trainingSubject the training subject
     */
    public void setTrainingSubject(@Nullable final SampledTrainable trainingSubject) {
      SampledTrainable temp_07_0005 = trainingSubject == null ? null : trainingSubject.addRef();
      if (null != this.trainingSubject)
        this.trainingSubject.freeRef();
      this.trainingSubject = temp_07_0005 == null ? null : temp_07_0005.addRef();
      if (null != temp_07_0005)
        temp_07_0005.freeRef();
      if (null != trainingSubject)
        trainingSubject.freeRef();
    }

    @Nonnull
    @Override
    public String toString() {
      return "TrainingPhase{" + "trainingSubject=" + trainingSubject + ", orientation=" + orientation + '}';
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      if (null != trainingSubject)
        trainingSubject.freeRef();
      trainingSubject = null;
      if (null != orientation)
        orientation.freeRef();
      orientation = null;
      if (null != lineSearchStrategyMap)
        lineSearchStrategyMap.freeRef();
      lineSearchStrategyMap = null;
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    TrainingPhase addRef() {
      return (TrainingPhase) super.addRef();
    }
  }

  private static class EpochParams extends ReferenceCountingBase {
    /**
     * The Timeout ms.
     */
    final long timeoutMs;
    /**
     * The Iterations.
     */
    int iterations;
    /**
     * The Training size.
     */
    int trainingSize;
    /**
     * The Validation.
     */
    @Nullable
    PointSample validation;

    private EpochParams(final long timeoutMs, final int iterations, final int trainingSize,
                        @Nullable final PointSample validation) {
      this.timeoutMs = timeoutMs;
      this.iterations = iterations;
      this.trainingSize = trainingSize;
      PointSample temp_07_0006 = validation == null ? null : validation.addRef();
      this.validation = temp_07_0006 == null ? null : temp_07_0006.addRef();
      if (null != temp_07_0006)
        temp_07_0006.freeRef();
      if (null != validation)
        validation.freeRef();
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      if (null != validation)
        validation.freeRef();
      validation = null;
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    EpochParams addRef() {
      return (EpochParams) super.addRef();
    }
  }

  private static class EpochResult extends ReferenceCountingBase {

    /**
     * The Continue training.
     */
    final boolean continueTraining;
    /**
     * The Current point.
     */
    @Nullable
    final PointSample currentPoint;
    /**
     * The Iterations.
     */
    final int iterations;
    /**
     * The Prior mean.
     */
    final double priorMean;

    /**
     * Instantiates a new Epoch result.
     *
     * @param continueTraining the continue training
     * @param priorMean        the prior mean
     * @param currentPoint     the current point
     * @param iterations       the iterations
     */
    public EpochResult(final boolean continueTraining, final double priorMean, @Nullable final PointSample currentPoint,
                       final int iterations) {
      this.priorMean = priorMean;
      PointSample temp_07_0007 = currentPoint == null ? null : currentPoint.addRef();
      this.currentPoint = temp_07_0007 == null ? null : temp_07_0007.addRef();
      if (null != temp_07_0007)
        temp_07_0007.freeRef();
      if (null != currentPoint)
        currentPoint.freeRef();
      this.continueTraining = continueTraining;
      this.iterations = iterations;
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      if (null != currentPoint)
        currentPoint.freeRef();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    EpochResult addRef() {
      return (EpochResult) super.addRef();
    }
  }

  private static class PerformanceWrapper extends TrainableWrapper<SampledTrainable> implements SampledTrainable {

    @Nonnull
    private final ValidatingTrainer parent;

    /**
     * Instantiates a new Performance wrapper.
     *
     * @param trainingSubject the training subject
     * @param parent          the parent
     */
    public PerformanceWrapper(final SampledTrainable trainingSubject, @Nullable ValidatingTrainer parent) {
      super(trainingSubject);
      ValidatingTrainer temp_07_0008 = parent == null ? null : parent.addRef();
      this.parent = temp_07_0008 == null ? null : temp_07_0008.addRef();
      if (null != temp_07_0008)
        temp_07_0008.freeRef();
      if (null != parent)
        parent.freeRef();
    }

    @Override
    public int getTrainingSize() {
      SampledTrainable temp_07_0038 = getInner();
      assert temp_07_0038 != null;
      int temp_07_0037 = temp_07_0038.getTrainingSize();
      temp_07_0038.freeRef();
      return temp_07_0037;
    }

    @Override
    public void setTrainingSize(final int trainingSize) {
      SampledTrainable inner = getInner();
      assert inner != null;
      inner.setTrainingSize(trainingSize);
      inner.freeRef();
    }

    @Nonnull
    @Override
    public SampledCachedTrainable<? extends SampledTrainable> cached() {
      return new SampledCachedTrainable<>(this.addRef());
    }

    @Override
    public PointSample measure(final TrainingMonitor monitor) {
      @Nonnull final TimedResult<PointSample> time = TimedResult.time(() -> {
        SampledTrainable inner = getInner();
        assert inner != null;
        PointSample measure = inner.measure(monitor);
        inner.freeRef();
        return measure;
      });
      parent.trainingMeasurementTime.addAndGet(time.timeNanos);
      PointSample result = time.getResult();
      time.freeRef();
      return result;
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      parent.freeRef();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    PerformanceWrapper addRef() {
      return (PerformanceWrapper) super.addRef();
    }
  }

  private static class StepResult extends ReferenceCountingBase {
    /**
     * The Current point.
     */
    final PointSample currentPoint;
    /**
     * The Performance.
     */
    final double[] performance;
    /**
     * The Previous.
     */
    final PointSample previous;

    /**
     * Instantiates a new Step result.
     *
     * @param previous     the previous
     * @param currentPoint the current point
     * @param performance  the performance
     */
    public StepResult(final PointSample previous, final PointSample currentPoint, final double[] performance) {
      this.currentPoint = currentPoint;
      this.previous = previous;
      this.performance = performance;
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      if (null != previous)
        previous.freeRef();
      if (null != currentPoint)
        currentPoint.freeRef();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    StepResult addRef() {
      return (StepResult) super.addRef();
    }
  }
}
