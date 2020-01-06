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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.FastRandom;
import com.simiacryptus.util.Util;
import com.simiacryptus.util.data.DoubleStatistics;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.IntFunction;

public @RefAware
class ValidatingTrainer extends ReferenceCountingBase {

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

  public ValidatingTrainer(@Nonnull final SampledTrainable trainingSubject,
                           @Nonnull final Trainable validationSubject) {
    {
      RefList<ValidatingTrainer.TrainingPhase> temp_07_0001 = new RefArrayList<TrainingPhase>(
          RefArrays.asList(new TrainingPhase(new PerformanceWrapper(
              trainingSubject == null ? null : trainingSubject.addRef(), ValidatingTrainer.this.addRef()))));
      regimen = temp_07_0001 == null ? null : temp_07_0001.addRef();
      if (null != temp_07_0001)
        temp_07_0001.freeRef();
    }
    {
      Trainable temp_07_0002 = new TrainableBase() {
        {
          validationSubject.addRef();
        }

        @NotNull
        @Override
        public Layer getLayer() {
          return validationSubject.getLayer();
        }

        @Override
        public PointSample measure(final TrainingMonitor monitor) {
          @Nonnull final TimedResult<PointSample> time = TimedResult.time(RefUtil.wrapInterface(
              (UncheckedSupplier<PointSample>) () -> validationSubject
                  .measure(monitor),
              validationSubject == null ? null : validationSubject.addRef()));
          validatingMeasurementTime.addAndGet(time.timeNanos);
          return time.result;
        }

        @Override
        public boolean reseed(final long seed) {
          return validationSubject.reseed(seed);
        }

        public void _free() {
          validationSubject.freeRef();
        }
      };
      this.validationSubject = temp_07_0002 == null ? null : temp_07_0002.addRef();
      if (null != temp_07_0002)
        temp_07_0002.freeRef();
    }
    validationSubject.freeRef();
    trainingSize = trainingSubject.getTrainingSize();
    trainingSubject.freeRef();
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = Double.NEGATIVE_INFINITY;
  }

  public double getAdjustmentFactor() {
    return adjustmentFactor;
  }

  @Nonnull
  public ValidatingTrainer setAdjustmentFactor(final double adjustmentFactor) {
    this.adjustmentFactor = adjustmentFactor;
    return this.addRef();
  }

  public double getAdjustmentTolerance() {
    return adjustmentTolerance;
  }

  @Nonnull
  public ValidatingTrainer setAdjustmentTolerance(final double adjustmentTolerance) {
    this.adjustmentTolerance = adjustmentTolerance;
    return this.addRef();
  }

  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  @Nonnull
  public ValidatingTrainer setCurrentIteration(final AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
    return this.addRef();
  }

  public int getDisappointmentThreshold() {
    return disappointmentThreshold;
  }

  public void setDisappointmentThreshold(final int disappointmentThreshold) {
    this.disappointmentThreshold = disappointmentThreshold;
  }

  public int getEpochIterations() {
    return epochIterations;
  }

  @Nonnull
  public ValidatingTrainer setEpochIterations(final int epochIterations) {
    this.epochIterations = epochIterations;
    return this.addRef();
  }

  public int getImprovmentStaleThreshold() {
    return improvmentStaleThreshold;
  }

  public void setImprovmentStaleThreshold(final int improvmentStaleThreshold) {
    this.improvmentStaleThreshold = improvmentStaleThreshold;
  }

  public int getMaxEpochIterations() {
    return maxEpochIterations;
  }

  @Nonnull
  public ValidatingTrainer setMaxEpochIterations(final int maxEpochIterations) {
    this.maxEpochIterations = maxEpochIterations;
    return this.addRef();
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  @Nonnull
  public ValidatingTrainer setMaxIterations(final int maxIterations) {
    this.maxIterations = maxIterations;
    return this.addRef();
  }

  public int getMaxTrainingSize() {
    return maxTrainingSize;
  }

  @Nonnull
  public ValidatingTrainer setMaxTrainingSize(final int maxTrainingSize) {
    this.maxTrainingSize = maxTrainingSize;
    return this.addRef();
  }

  public int getMinEpochIterations() {
    return minEpochIterations;
  }

  @Nonnull
  public ValidatingTrainer setMinEpochIterations(final int minEpochIterations) {
    this.minEpochIterations = minEpochIterations;
    return this.addRef();
  }

  public int getMinTrainingSize() {
    return minTrainingSize;
  }

  @Nonnull
  public ValidatingTrainer setMinTrainingSize(final int minTrainingSize) {
    this.minTrainingSize = minTrainingSize;
    return this.addRef();
  }

  public TrainingMonitor getMonitor() {
    return monitor;
  }

  @Nonnull
  public ValidatingTrainer setMonitor(final TrainingMonitor monitor) {
    this.monitor = monitor;
    return this.addRef();
  }

  public double getOvertrainingTarget() {
    return overtrainingTarget;
  }

  @Nonnull
  public ValidatingTrainer setOvertrainingTarget(final double overtrainingTarget) {
    this.overtrainingTarget = overtrainingTarget;
    return this.addRef();
  }

  public double getPessimism() {
    return pessimism;
  }

  @Nonnull
  public ValidatingTrainer setPessimism(final double pessimism) {
    this.pessimism = pessimism;
    return this.addRef();
  }

  @Nonnull
  public RefList<TrainingPhase> getRegimen() {
    return regimen == null ? null : regimen.addRef();
  }

  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  @Nonnull
  public ValidatingTrainer setTerminateThreshold(final double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
    return this.addRef();
  }

  public Duration getTimeout() {
    return timeout;
  }

  @Nonnull
  public ValidatingTrainer setTimeout(final Duration timeout) {
    this.timeout = timeout;
    return this.addRef();
  }

  public int getTrainingSize() {
    return trainingSize;
  }

  @Nonnull
  public ValidatingTrainer setTrainingSize(final int trainingSize) {
    this.trainingSize = trainingSize;
    return this.addRef();
  }

  public double getTrainingTarget() {
    return trainingTarget;
  }

  @Nonnull
  public ValidatingTrainer setTrainingTarget(final double trainingTarget) {
    this.trainingTarget = trainingTarget;
    return this.addRef();
  }

  @Nonnull
  public Trainable getValidationSubject() {
    return validationSubject == null ? null : validationSubject.addRef();
  }

  @Nonnull
  @Deprecated
  public ValidatingTrainer setLineSearchFactory(final Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    RefList<ValidatingTrainer.TrainingPhase> temp_07_0024 = getRegimen();
    ValidatingTrainer.TrainingPhase temp_07_0025 = temp_07_0024.get(0);
    RefUtil.freeRef(temp_07_0025.setLineSearchFactory(lineSearchFactory));
    if (null != temp_07_0025)
      temp_07_0025.freeRef();
    if (null != temp_07_0024)
      temp_07_0024.freeRef();
    return this.addRef();
  }

  @Nonnull
  @Deprecated
  public ValidatingTrainer setOrientation(final OrientationStrategy<?> orientation) {
    RefList<ValidatingTrainer.TrainingPhase> temp_07_0026 = getRegimen();
    ValidatingTrainer.TrainingPhase temp_07_0027 = temp_07_0026.get(0);
    RefUtil
        .freeRef(temp_07_0027.setOrientation(orientation == null ? null : orientation.addRef()));
    if (null != temp_07_0027)
      temp_07_0027.freeRef();
    if (null != temp_07_0026)
      temp_07_0026.freeRef();
    if (null != orientation)
      orientation.freeRef();
    return this.addRef();
  }

  public static @SuppressWarnings("unused")
  ValidatingTrainer[] addRefs(ValidatingTrainer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValidatingTrainer::addRef)
        .toArray((x) -> new ValidatingTrainer[x]);
  }

  public static @SuppressWarnings("unused")
  ValidatingTrainer[][] addRefs(ValidatingTrainer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValidatingTrainer::addRefs)
        .toArray((x) -> new ValidatingTrainer[x][]);
  }

  @Nonnull
  private static CharSequence getId(@Nonnull final DoubleBuffer<UUID> x) {
    String temp_07_0023 = x.key.toString();
    x.freeRef();
    return temp_07_0023;
  }

  public double run() {
    try {
      final long timeoutAt = com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis() + timeout.toMillis();
      if (validationSubject.getLayer() instanceof DAGNetwork) {
        ((DAGNetwork) validationSubject.getLayer()).visitLayers(layer -> {
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
        final long seed = com.simiacryptus.ref.wrappers.RefSystem.nanoTime();
        final RefList<EpochResult> epochResults = RefIntStream.range(0, regimen.size())
            .mapToObj(RefUtil.wrapInterface(
                (IntFunction<? extends ValidatingTrainer.EpochResult>) i -> {
                  RefList<ValidatingTrainer.TrainingPhase> temp_07_0028 = getRegimen();
                  final TrainingPhase phase = temp_07_0028.get(i);
                  if (null != temp_07_0028)
                    temp_07_0028.freeRef();
                  ValidatingTrainer.EpochResult temp_07_0012 = runPhase(
                      epochParams == null ? null : epochParams.addRef(), phase == null ? null : phase.addRef(), i,
                      seed);
                  if (null != phase)
                    phase.freeRef();
                  return temp_07_0012;
                }, epochParams == null ? null : epochParams.addRef()))
            .collect(RefCollectors.toList());
        regimen.freeRef();
        final EpochResult primaryPhase = epochResults.get(0);
        if (null != epochResults)
          epochResults.freeRef();
        iterationNumber += primaryPhase.iterations;
        final double trainingDelta = primaryPhase.currentPoint.getMean() / primaryPhase.priorMean;
        if (validationSubject.getLayer() instanceof DAGNetwork) {
          ((DAGNetwork) validationSubject.getLayer()).visitLayers(layer -> {
            if (layer instanceof StochasticComponent)
              ((StochasticComponent) layer).clearNoise();
            if (null != layer)
              layer.freeRef();
          });
        }
        final PointSample currentValidation = validationSubject.measure(monitor);
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
                monitor.log(RefString.format("Training converged after %s iterations", iterationNumber - lastImprovement));
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
        if (null != primaryPhase)
          primaryPhase.freeRef();
        epochParams.validation = currentValidation == null ? null : currentValidation.addRef();
        if (null != currentValidation)
          currentValidation.freeRef();
      }
      if (validationSubject.getLayer() instanceof DAGNetwork) {
        ((DAGNetwork) validationSubject.getLayer()).visitLayers(layer -> {
          if (layer instanceof StochasticComponent)
            ((StochasticComponent) layer).clearNoise();
          if (null != layer)
            layer.freeRef();
        });
      }
      double temp_07_0011 = epochParams.validation.getMean();
      epochParams.freeRef();
      return temp_07_0011;
    } catch (@Nonnull final Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public ValidatingTrainer setTimeout(final int number, @Nonnull final TemporalUnit units) {
    timeout = Duration.of(number, units);
    return this.addRef();
  }

  @Nonnull
  public ValidatingTrainer setTimeout(final int number, @Nonnull final TimeUnit units) {
    return setTimeout(number, Util.cvt(units));
  }

  public @SuppressWarnings("unused")
  void _free() {
    validationSubject.freeRef();
    regimen.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  ValidatingTrainer addRef() {
    return (ValidatingTrainer) super.addRef();
  }

  @Nonnull
  protected EpochResult runPhase(@Nonnull final EpochParams epochParams, @Nonnull final TrainingPhase phase,
                                 final int i, final long seed) {
    monitor.log(RefString.format("Phase %d: %s", i, phase));
    phase.trainingSubject.setTrainingSize(epochParams.trainingSize);
    monitor.log(RefString.format("resetAndMeasure; trainingSize=%s", epochParams.trainingSize));
    ValidatingTrainer temp_07_0029 = reset(phase == null ? null : phase.addRef(), seed);
    PointSample currentPoint = temp_07_0029.measure(phase == null ? null : phase.addRef());
    if (null != temp_07_0029)
      temp_07_0029.freeRef();
    final double pointMean = currentPoint.getMean();
    RefMap<UUID, Delta<UUID>> temp_07_0030 = currentPoint.delta
        .getMap();
    assert 0 < temp_07_0030.size() : "Nothing to optimize";
    if (null != temp_07_0030)
      temp_07_0030.freeRef();
    int step = 1;
    for (; step <= epochParams.iterations || epochParams.iterations <= 0; step++) {
      if (shouldHalt(monitor, epochParams.timeoutMs)) {
        ValidatingTrainer.EpochResult temp_07_0014 = new EpochResult(false, pointMean,
            currentPoint == null ? null : currentPoint.addRef(), step);
        if (null != currentPoint)
          currentPoint.freeRef();
        epochParams.freeRef();
        phase.freeRef();
        return temp_07_0014;
      }
      final long startTime = com.simiacryptus.ref.wrappers.RefSystem.nanoTime();
      final long prevGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
          .mapToLong(x -> x.getCollectionTime()).sum();
      @Nonnull final StepResult epoch = runStep(currentPoint == null ? null : currentPoint.addRef(),
          phase == null ? null : phase.addRef());
      final long newGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
          .mapToLong(x -> x.getCollectionTime()).sum();
      final long endTime = com.simiacryptus.ref.wrappers.RefSystem.nanoTime();
      final CharSequence performance = RefString.format(
          "%s in %.3f seconds; %.3f in orientation, %.3f in gc, %.3f in line search; %.3f trainAll time",
          epochParams.trainingSize, (endTime - startTime) / 1e9, epoch.performance[0], (newGcTime - prevGcTime) / 1e3,
          epoch.performance[1], trainingMeasurementTime.getAndSet(0) / 1e9);
      currentPoint = epoch.currentPoint.setRate(0.0);
      if (epoch.previous.getMean() <= epoch.currentPoint.getMean()) {
        monitor.log(RefString.format("Iteration %s failed, aborting. Error: %s (%s)", currentIteration.get(),
            epoch.currentPoint.getMean(), performance));
        ValidatingTrainer.EpochResult temp_07_0015 = new EpochResult(false, pointMean,
            currentPoint == null ? null : currentPoint.addRef(), step);
        if (null != currentPoint)
          currentPoint.freeRef();
        epoch.freeRef();
        epochParams.freeRef();
        phase.freeRef();
        return temp_07_0015;
      } else {
        monitor.log(RefString.format("Iteration %s complete. Error: %s (%s)", currentIteration.get(),
            epoch.currentPoint.getMean(), performance));
      }
      epoch.freeRef();
      monitor.onStepComplete(new Step(currentPoint == null ? null : currentPoint.addRef(), currentIteration.get()));
    }
    phase.freeRef();
    epochParams.freeRef();
    ValidatingTrainer.EpochResult temp_07_0013 = new EpochResult(true, pointMean,
        currentPoint == null ? null : currentPoint.addRef(), step);
    if (null != currentPoint)
      currentPoint.freeRef();
    return temp_07_0013;
  }

  @Nonnull
  protected StepResult runStep(@Nonnull final PointSample previousPoint, @Nonnull final TrainingPhase phase) {
    currentIteration.incrementAndGet();
    @Nonnull final TimedResult<LineSearchCursor> timedOrientation = TimedResult
        .time(RefUtil.wrapInterface(
            (UncheckedSupplier<LineSearchCursor>) () -> phase.orientation
                .orient(phase.trainingSubject.addRef(), previousPoint == null ? null : previousPoint.addRef(), monitor),
            previousPoint == null ? null : previousPoint.addRef(), phase == null ? null : phase.addRef()));
    final LineSearchCursor direction = timedOrientation.result.addRef();
    final CharSequence directionType = direction.getDirectionType();
    LineSearchStrategy lineSearchStrategy;
    if (phase.lineSearchStrategyMap.containsKey(directionType)) {
      lineSearchStrategy = phase.lineSearchStrategyMap.get(directionType);
    } else {
      monitor.log(RefString.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = phase.lineSearchFactory.apply(direction.getDirectionType());
      phase.lineSearchStrategyMap.put(directionType, lineSearchStrategy);
    }
    phase.freeRef();
    @Nonnull final TimedResult<PointSample> timedLineSearch = TimedResult.time(RefUtil
        .wrapInterface((UncheckedSupplier<PointSample>) () -> {
          @Nonnull final FailsafeLineSearchCursor cursor = new FailsafeLineSearchCursor(
              direction == null ? null : direction.addRef(), previousPoint == null ? null : previousPoint.addRef(),
              monitor);
          RefUtil
              .freeRef(lineSearchStrategy.step(cursor == null ? null : cursor.addRef(), monitor));
          PointSample temp_07_0031 = cursor.getBest(monitor);
          PointSample temp_07_0016 = temp_07_0031.restore();
          if (null != temp_07_0031)
            temp_07_0031.freeRef();
          cursor.freeRef();
          //cursor.step(restore.rate, monitor);
          return temp_07_0016;
        }, previousPoint == null ? null : previousPoint.addRef(), direction == null ? null : direction.addRef()));
    if (null != direction)
      direction.freeRef();
    final PointSample bestPoint = timedLineSearch.result.addRef();
    if (bestPoint.getMean() > previousPoint.getMean()) {
      IllegalStateException temp_07_0018 = new IllegalStateException(
          bestPoint.getMean() + " > " + previousPoint.getMean());
      if (null != bestPoint)
        bestPoint.freeRef();
      previousPoint.freeRef();
      throw temp_07_0018;
    }
    monitor.log(
        compare(previousPoint == null ? null : previousPoint.addRef(), bestPoint == null ? null : bestPoint.addRef()));
    ValidatingTrainer.StepResult temp_07_0017 = new StepResult(
        previousPoint == null ? null : previousPoint, bestPoint == null ? null : bestPoint.addRef(),
        new double[]{timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9});
    if (null != bestPoint)
      bestPoint.freeRef();
    return temp_07_0017;
  }

  protected boolean shouldHalt(@Nonnull final TrainingMonitor monitor, final long timeoutMs) {
    com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis();
    if (timeoutMs < com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis()) {
      monitor.log("Training timeout");
      return true;
    } else if (currentIteration.get() > maxIterations) {
      monitor.log("Training iteration overflow");
      return true;
    } else {
      return false;
    }
  }

  private String compare(@Nonnull final PointSample previousPoint, @Nonnull final PointSample nextPoint) {
    @Nonnull final StateSet<UUID> nextWeights = nextPoint.weights.addRef();
    nextPoint.freeRef();
    @Nonnull final StateSet<UUID> prevWeights = previousPoint.weights.addRef();
    previousPoint.freeRef();
    RefMap<State<UUID>, RefList<State<UUID>>> temp_07_0032 = prevWeights
        .stream().collect(RefCollectors.groupingBy(x -> {
          return x;
        }, RefCollectors.toList()));
    RefSet<Map.Entry<State<UUID>, RefList<State<UUID>>>> temp_07_0033 = temp_07_0032
        .entrySet();
    RefMap<State<UUID>, String> temp_07_0036 = temp_07_0033
        .stream().collect(RefCollectors.toMap(x -> {
          State<UUID> temp_07_0020 = x.getKey();
          if (null != x)
            RefUtil.freeRef(x);
          return temp_07_0020;
        }, RefUtil.wrapInterface(
            (Function<? super Map.Entry<State<UUID>, RefList<State<UUID>>>, ? extends String>) list -> {
              RefList<State<UUID>> temp_07_0034 = list
                  .getValue();
              final RefList<Double> doubleList = temp_07_0034.stream()
                  .map(RefUtil.wrapInterface(
                      (Function<? super State<UUID>, ? extends Double>) prevWeight -> {
                        RefMap<UUID, State<UUID>> temp_07_0035 = nextWeights
                            .getMap();
                        final DoubleBuffer<UUID> dirDelta = temp_07_0035.get(prevWeight.key);
                        if (null != temp_07_0035)
                          temp_07_0035.freeRef();
                        final double numerator = prevWeight.deltaStatistics().rms();
                        if (null != prevWeight)
                          prevWeight.freeRef();
                        final double denominator = null == dirDelta ? 0 : dirDelta.deltaStatistics().rms();
                        if (null != dirDelta)
                          dirDelta.freeRef();
                        return numerator / (0 == denominator ? 1 : denominator);
                      }, nextWeights == null ? null : nextWeights.addRef()))
                  .collect(RefCollectors.toList());
              if (null != temp_07_0034)
                temp_07_0034.freeRef();
              if (null != list)
                RefUtil.freeRef(list);
              if (1 == doubleList.size()) {
                String temp_07_0022 = Double.toString(doubleList.get(0));
                if (null != doubleList)
                  doubleList.freeRef();
                return temp_07_0022;
              }
              String temp_07_0021 = new DoubleStatistics()
                  .accept(doubleList.stream().mapToDouble(x -> x).toArray()).toString();
              if (null != doubleList)
                doubleList.freeRef();
              return temp_07_0021;
            }, nextWeights == null ? null : nextWeights)));
    String temp_07_0019 = RefString.format("Overall network state change: %s", temp_07_0036);
    if (null != temp_07_0036)
      temp_07_0036.freeRef();
    if (null != temp_07_0033)
      temp_07_0033.freeRef();
    if (null != temp_07_0032)
      temp_07_0032.freeRef();
    prevWeights.freeRef();
    return temp_07_0019;
  }

  private PointSample measure(@Nonnull final TrainingPhase phase) {
    int retries = 0;
    try {
      do {
        if (10 < retries++) {
          phase.freeRef();
          throw new IterativeStopException();
        }
        final PointSample currentPoint = phase.trainingSubject.measure(monitor);
        if (Double.isFinite(currentPoint.getMean())) {
          phase.freeRef();
          return currentPoint;
        }
        if (null != currentPoint)
          currentPoint.freeRef();
        phase.orientation.reset();
      } while (true);
    } finally {
      phase.freeRef();
    }
  }

  @Nonnull
  private ValidatingTrainer reset(@Nonnull final TrainingPhase phase, final long seed) {
    if (!phase.trainingSubject.reseed(seed)) {
      phase.freeRef();
      throw new IterativeStopException();
    }
    phase.orientation.reset();
    phase.trainingSubject.reseed(seed);
    if (phase.trainingSubject.getLayer() instanceof DAGNetwork) {
      ((DAGNetwork) phase.trainingSubject.getLayer()).shuffle(StochasticComponent.random.get().nextLong());
    }
    phase.freeRef();
    return this.addRef();
  }

  public static @RefAware
  class TrainingPhase extends ReferenceCountingBase {
    private Function<CharSequence, LineSearchStrategy> lineSearchFactory = (s) -> new ArmijoWolfeSearch();
    private RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new RefHashMap<>();
    private OrientationStrategy<?> orientation = new LBFGS();
    private SampledTrainable trainingSubject;

    public TrainingPhase(final SampledTrainable trainingSubject) {
      setTrainingSubject(trainingSubject == null ? null : trainingSubject.addRef());
      if (null != trainingSubject)
        trainingSubject.freeRef();
    }

    public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
      return lineSearchFactory;
    }

    @Nonnull
    public TrainingPhase setLineSearchFactory(final Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
      this.lineSearchFactory = lineSearchFactory;
      return this.addRef();
    }

    public RefMap<CharSequence, LineSearchStrategy> getLineSearchStrategyMap() {
      return lineSearchStrategyMap == null ? null : lineSearchStrategyMap.addRef();
    }

    @Nonnull
    public TrainingPhase setLineSearchStrategyMap(
        final RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap) {
      {
        RefMap<CharSequence, LineSearchStrategy> temp_07_0003 = lineSearchStrategyMap == null
            ? null
            : lineSearchStrategyMap.addRef();
        if (null != this.lineSearchStrategyMap)
          this.lineSearchStrategyMap.freeRef();
        this.lineSearchStrategyMap = temp_07_0003 == null ? null : temp_07_0003.addRef();
        if (null != temp_07_0003)
          temp_07_0003.freeRef();
      }
      if (null != lineSearchStrategyMap)
        lineSearchStrategyMap.freeRef();
      return this.addRef();
    }

    public OrientationStrategy<?> getOrientation() {
      return orientation == null ? null : orientation.addRef();
    }

    @Nonnull
    public TrainingPhase setOrientation(final OrientationStrategy<?> orientation) {
      {
        OrientationStrategy<?> temp_07_0004 = orientation == null ? null
            : orientation.addRef();
        if (null != this.orientation)
          this.orientation.freeRef();
        this.orientation = temp_07_0004 == null ? null : temp_07_0004.addRef();
        if (null != temp_07_0004)
          temp_07_0004.freeRef();
      }
      if (null != orientation)
        orientation.freeRef();
      return this.addRef();
    }

    public SampledTrainable getTrainingSubject() {
      return trainingSubject == null ? null : trainingSubject.addRef();
    }

    @Nonnull
    public void setTrainingSubject(final SampledTrainable trainingSubject) {
      {
        SampledTrainable temp_07_0005 = trainingSubject == null ? null
            : trainingSubject.addRef();
        if (null != this.trainingSubject)
          this.trainingSubject.freeRef();
        this.trainingSubject = temp_07_0005 == null ? null : temp_07_0005.addRef();
        if (null != temp_07_0005)
          temp_07_0005.freeRef();
      }
      if (null != trainingSubject)
        trainingSubject.freeRef();
    }

    public static @SuppressWarnings("unused")
    TrainingPhase[] addRefs(TrainingPhase[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(TrainingPhase::addRef)
          .toArray((x) -> new TrainingPhase[x]);
    }

    @Nonnull
    @Override
    public String toString() {
      return "TrainingPhase{" + "trainingSubject=" + trainingSubject + ", orientation=" + orientation + '}';
    }

    public @SuppressWarnings("unused")
    void _free() {
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

    public @Override
    @SuppressWarnings("unused")
    TrainingPhase addRef() {
      return (TrainingPhase) super.addRef();
    }
  }

  private static @RefAware
  class EpochParams extends ReferenceCountingBase {
    final long timeoutMs;
    int iterations;
    int trainingSize;
    PointSample validation;

    private EpochParams(final long timeoutMs, final int iterations, final int trainingSize,
                        final PointSample validation) {
      this.timeoutMs = timeoutMs;
      this.iterations = iterations;
      this.trainingSize = trainingSize;
      {
        PointSample temp_07_0006 = validation == null ? null : validation.addRef();
        if (null != this.validation)
          this.validation.freeRef();
        this.validation = temp_07_0006 == null ? null : temp_07_0006.addRef();
        if (null != temp_07_0006)
          temp_07_0006.freeRef();
      }
      if (null != validation)
        validation.freeRef();
    }

    public static @SuppressWarnings("unused")
    EpochParams[] addRefs(EpochParams[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(EpochParams::addRef)
          .toArray((x) -> new EpochParams[x]);
    }

    public @SuppressWarnings("unused")
    void _free() {
      if (null != validation)
        validation.freeRef();
      validation = null;
    }

    public @Override
    @SuppressWarnings("unused")
    EpochParams addRef() {
      return (EpochParams) super.addRef();
    }

  }

  private static @RefAware
  class EpochResult extends ReferenceCountingBase {

    final boolean continueTraining;
    final PointSample currentPoint;
    final int iterations;
    final double priorMean;

    public EpochResult(final boolean continueTraining, final double priorMean, final PointSample currentPoint,
                       final int iterations) {
      this.priorMean = priorMean;
      {
        PointSample temp_07_0007 = currentPoint == null ? null : currentPoint.addRef();
        this.currentPoint = temp_07_0007 == null ? null : temp_07_0007.addRef();
        if (null != temp_07_0007)
          temp_07_0007.freeRef();
      }
      if (null != currentPoint)
        currentPoint.freeRef();
      this.continueTraining = continueTraining;
      this.iterations = iterations;
    }

    public static @SuppressWarnings("unused")
    EpochResult[] addRefs(EpochResult[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(EpochResult::addRef)
          .toArray((x) -> new EpochResult[x]);
    }

    public @SuppressWarnings("unused")
    void _free() {
      if (null != currentPoint)
        currentPoint.freeRef();
    }

    public @Override
    @SuppressWarnings("unused")
    EpochResult addRef() {
      return (EpochResult) super.addRef();
    }

  }

  private static @RefAware
  class PerformanceWrapper extends TrainableWrapper<SampledTrainable>
      implements SampledTrainable {

    private final ValidatingTrainer parent;

    public PerformanceWrapper(final SampledTrainable trainingSubject, ValidatingTrainer parent) {
      super(trainingSubject);
      if (null != trainingSubject)
        trainingSubject.freeRef();
      {
        ValidatingTrainer temp_07_0008 = parent == null ? null : parent.addRef();
        this.parent = temp_07_0008 == null ? null : temp_07_0008.addRef();
        if (null != temp_07_0008)
          temp_07_0008.freeRef();
      }
      if (null != parent)
        parent.freeRef();
    }

    @Override
    public int getTrainingSize() {
      SampledTrainable temp_07_0038 = getInner();
      int temp_07_0037 = temp_07_0038.getTrainingSize();
      if (null != temp_07_0038)
        temp_07_0038.freeRef();
      return temp_07_0037;
    }

    @Nonnull
    @Override
    public void setTrainingSize(final int trainingSize) {
      SampledTrainable temp_07_0039 = getInner();
      temp_07_0039.setTrainingSize(trainingSize);
      if (null != temp_07_0039)
        temp_07_0039.freeRef();
    }

    public static @SuppressWarnings("unused")
    PerformanceWrapper[] addRefs(PerformanceWrapper[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(PerformanceWrapper::addRef)
          .toArray((x) -> new PerformanceWrapper[x]);
    }

    @Nonnull
    @Override
    public SampledCachedTrainable<? extends SampledTrainable> cached() {
      return new SampledCachedTrainable<>(this.addRef());
    }

    @Override
    public PointSample measure(final TrainingMonitor monitor) {
      @Nonnull final TimedResult<PointSample> time = TimedResult.time(() -> {
        return getInner().measure(monitor);
      });
      parent.trainingMeasurementTime.addAndGet(time.timeNanos);
      return time.result;
    }

    public @SuppressWarnings("unused")
    void _free() {
      if (null != parent)
        parent.freeRef();
    }

    public @Override
    @SuppressWarnings("unused")
    PerformanceWrapper addRef() {
      return (PerformanceWrapper) super.addRef();
    }

  }

  private static @RefAware
  class StepResult extends ReferenceCountingBase {
    final PointSample currentPoint;
    final double[] performance;
    final PointSample previous;

    public StepResult(final PointSample previous, final PointSample currentPoint, final double[] performance) {
      {
        PointSample temp_07_0009 = currentPoint == null ? null : currentPoint.addRef();
        this.currentPoint = temp_07_0009 == null ? null : temp_07_0009.addRef();
        if (null != temp_07_0009)
          temp_07_0009.freeRef();
      }
      if (null != currentPoint)
        currentPoint.freeRef();
      {
        PointSample temp_07_0010 = previous == null ? null : previous.addRef();
        this.previous = temp_07_0010 == null ? null : temp_07_0010.addRef();
        if (null != temp_07_0010)
          temp_07_0010.freeRef();
      }
      if (null != previous)
        previous.freeRef();
      this.performance = performance;
    }

    public static @SuppressWarnings("unused")
    StepResult[] addRefs(StepResult[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(StepResult::addRef)
          .toArray((x) -> new StepResult[x]);
    }

    public @SuppressWarnings("unused")
    void _free() {
      if (null != previous)
        previous.freeRef();
      if (null != currentPoint)
        currentPoint.freeRef();
    }

    public @Override
    @SuppressWarnings("unused")
    StepResult addRef() {
      return (StepResult) super.addRef();
    }

  }
}
