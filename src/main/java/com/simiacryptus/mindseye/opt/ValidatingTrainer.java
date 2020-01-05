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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

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
    regimen = new RefArrayList<TrainingPhase>(RefArrays
        .asList(new TrainingPhase(new PerformanceWrapper(trainingSubject, ValidatingTrainer.this))));
    this.validationSubject = new TrainableBase() {
      @NotNull
      @Override
      public Layer getLayer() {
        return validationSubject.getLayer();
      }

      @Override
      public PointSample measure(final TrainingMonitor monitor) {
        @Nonnull final TimedResult<PointSample> time = TimedResult.time(() -> validationSubject.measure(monitor));
        validatingMeasurementTime.addAndGet(time.timeNanos);
        return time.result;
      }

      @Override
      public boolean reseed(final long seed) {
        return validationSubject.reseed(seed);
      }

      public void _free() {
      }
    };
    trainingSize = trainingSubject.getTrainingSize();
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = Double.NEGATIVE_INFINITY;
  }

  public double getAdjustmentFactor() {
    return adjustmentFactor;
  }

  @Nonnull
  public ValidatingTrainer setAdjustmentFactor(final double adjustmentFactor) {
    this.adjustmentFactor = adjustmentFactor;
    return this;
  }

  public double getAdjustmentTolerance() {
    return adjustmentTolerance;
  }

  @Nonnull
  public ValidatingTrainer setAdjustmentTolerance(final double adjustmentTolerance) {
    this.adjustmentTolerance = adjustmentTolerance;
    return this;
  }

  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  @Nonnull
  public ValidatingTrainer setCurrentIteration(final AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
    return this;
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
    return this;
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
    return this;
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  @Nonnull
  public ValidatingTrainer setMaxIterations(final int maxIterations) {
    this.maxIterations = maxIterations;
    return this;
  }

  public int getMaxTrainingSize() {
    return maxTrainingSize;
  }

  @Nonnull
  public ValidatingTrainer setMaxTrainingSize(final int maxTrainingSize) {
    this.maxTrainingSize = maxTrainingSize;
    return this;
  }

  public int getMinEpochIterations() {
    return minEpochIterations;
  }

  @Nonnull
  public ValidatingTrainer setMinEpochIterations(final int minEpochIterations) {
    this.minEpochIterations = minEpochIterations;
    return this;
  }

  public int getMinTrainingSize() {
    return minTrainingSize;
  }

  @Nonnull
  public ValidatingTrainer setMinTrainingSize(final int minTrainingSize) {
    this.minTrainingSize = minTrainingSize;
    return this;
  }

  public TrainingMonitor getMonitor() {
    return monitor;
  }

  @Nonnull
  public ValidatingTrainer setMonitor(final TrainingMonitor monitor) {
    this.monitor = monitor;
    return this;
  }

  public double getOvertrainingTarget() {
    return overtrainingTarget;
  }

  @Nonnull
  public ValidatingTrainer setOvertrainingTarget(final double overtrainingTarget) {
    this.overtrainingTarget = overtrainingTarget;
    return this;
  }

  public double getPessimism() {
    return pessimism;
  }

  @Nonnull
  public ValidatingTrainer setPessimism(final double pessimism) {
    this.pessimism = pessimism;
    return this;
  }

  @Nonnull
  public RefList<TrainingPhase> getRegimen() {
    return regimen;
  }

  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  @Nonnull
  public ValidatingTrainer setTerminateThreshold(final double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
    return this;
  }

  public Duration getTimeout() {
    return timeout;
  }

  @Nonnull
  public ValidatingTrainer setTimeout(final Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public int getTrainingSize() {
    return trainingSize;
  }

  @Nonnull
  public ValidatingTrainer setTrainingSize(final int trainingSize) {
    this.trainingSize = trainingSize;
    return this;
  }

  public double getTrainingTarget() {
    return trainingTarget;
  }

  @Nonnull
  public ValidatingTrainer setTrainingTarget(final double trainingTarget) {
    this.trainingTarget = trainingTarget;
    return this;
  }

  @Nonnull
  public Trainable getValidationSubject() {
    return validationSubject;
  }

  @Nonnull
  @Deprecated
  public ValidatingTrainer setLineSearchFactory(final Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    getRegimen().get(0).setLineSearchFactory(lineSearchFactory);
    return this;
  }

  @Nonnull
  @Deprecated
  public ValidatingTrainer setOrientation(final OrientationStrategy<?> orientation) {
    getRegimen().get(0).setOrientation(orientation);
    return this;
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
    return x.key.toString();
  }

  public double run() {
    try {
      final long timeoutAt = System.currentTimeMillis() + timeout.toMillis();
      if (validationSubject.getLayer() instanceof DAGNetwork) {
        ((DAGNetwork) validationSubject.getLayer()).visitLayers(layer -> {
          if (layer instanceof StochasticComponent)
            ((StochasticComponent) layer).clearNoise();
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
        monitor.log(String.format("Epoch parameters: %s, %s", epochParams.trainingSize, epochParams.iterations));
        @Nonnull final RefList<TrainingPhase> regimen = getRegimen();
        final long seed = System.nanoTime();
        final RefList<EpochResult> epochResults = RefIntStream
            .range(0, regimen.size()).mapToObj(i -> {
              final TrainingPhase phase = getRegimen().get(i);
              return runPhase(epochParams, phase, i, seed);
            }).collect(RefCollectors.toList());
        final EpochResult primaryPhase = epochResults.get(0);
        iterationNumber += primaryPhase.iterations;
        final double trainingDelta = primaryPhase.currentPoint.getMean() / primaryPhase.priorMean;
        if (validationSubject.getLayer() instanceof DAGNetwork) {
          ((DAGNetwork) validationSubject.getLayer()).visitLayers(layer -> {
            if (layer instanceof StochasticComponent)
              ((StochasticComponent) layer).clearNoise();
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
        monitor.log(String.format(
            "Epoch %d result apply %s iterations, %s/%s samples: {validation *= 2^%.5f; training *= 2^%.3f; Overtraining = %.2f}, {itr*=%.2f, len*=%.2f} %s since improvement; %.4f validation time",
            ++epochNumber, primaryPhase.iterations, epochParams.trainingSize, getMaxTrainingSize(),
            Math.log(validationDelta) / Math.log(2), Math.log(trainingDelta) / Math.log(2), overtraining, adj1, adj2,
            iterationNumber - lastImprovement, validatingMeasurementTime.getAndSet(0) / 1e9));
        if (!primaryPhase.continueTraining) {
          monitor.log(String.format("Training %d runPhase halted", epochNumber));
          break;
        }
        if (epochParams.trainingSize >= getMaxTrainingSize()) {
          final double roll = FastRandom.INSTANCE.random();
          if (roll > Math.pow(2 - validationDelta, pessimism)) {
            monitor.log(String.format("Training randomly converged: %3f", roll));
            break;
          } else {
            if (iterationNumber - lastImprovement > improvmentStaleThreshold) {
              if (disappointments.incrementAndGet() > getDisappointmentThreshold()) {
                monitor.log(String.format("Training converged after %s iterations", iterationNumber - lastImprovement));
                break;
              } else {
                monitor.log(String.format("Training failed to converged on %s attempt after %s iterations",
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
        epochParams.validation = currentValidation;
      }
      if (validationSubject.getLayer() instanceof DAGNetwork) {
        ((DAGNetwork) validationSubject.getLayer()).visitLayers(layer -> {
          if (layer instanceof StochasticComponent)
            ((StochasticComponent) layer).clearNoise();
        });
      }
      return epochParams.validation.getMean();
    } catch (@Nonnull final Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public ValidatingTrainer setTimeout(final int number, @Nonnull final TemporalUnit units) {
    timeout = Duration.of(number, units);
    return this;
  }

  @Nonnull
  public ValidatingTrainer setTimeout(final int number, @Nonnull final TimeUnit units) {
    return setTimeout(number, Util.cvt(units));
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  ValidatingTrainer addRef() {
    return (ValidatingTrainer) super.addRef();
  }

  @Nonnull
  protected EpochResult runPhase(@Nonnull final EpochParams epochParams, @Nonnull final TrainingPhase phase,
                                 final int i, final long seed) {
    monitor.log(String.format("Phase %d: %s", i, phase));
    phase.trainingSubject.setTrainingSize(epochParams.trainingSize);
    monitor.log(String.format("resetAndMeasure; trainingSize=%s", epochParams.trainingSize));
    PointSample currentPoint = reset(phase, seed).measure(phase);
    final double pointMean = currentPoint.getMean();
    assert 0 < currentPoint.delta.getMap().size() : "Nothing to optimize";
    int step = 1;
    for (; step <= epochParams.iterations || epochParams.iterations <= 0; step++) {
      if (shouldHalt(monitor, epochParams.timeoutMs)) {
        return new EpochResult(false, pointMean, currentPoint, step);
      }
      final long startTime = System.nanoTime();
      final long prevGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
          .mapToLong(x -> x.getCollectionTime()).sum();
      @Nonnull final StepResult epoch = runStep(currentPoint, phase);
      final long newGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
          .mapToLong(x -> x.getCollectionTime()).sum();
      final long endTime = System.nanoTime();
      final CharSequence performance = String.format(
          "%s in %.3f seconds; %.3f in orientation, %.3f in gc, %.3f in line search; %.3f trainAll time",
          epochParams.trainingSize, (endTime - startTime) / 1e9, epoch.performance[0], (newGcTime - prevGcTime) / 1e3,
          epoch.performance[1], trainingMeasurementTime.getAndSet(0) / 1e9);
      currentPoint = epoch.currentPoint.setRate(0.0);
      if (epoch.previous.getMean() <= epoch.currentPoint.getMean()) {
        monitor.log(String.format("Iteration %s failed, aborting. Error: %s (%s)", currentIteration.get(),
            epoch.currentPoint.getMean(), performance));
        return new EpochResult(false, pointMean, currentPoint, step);
      } else {
        monitor.log(String.format("Iteration %s complete. Error: %s (%s)", currentIteration.get(),
            epoch.currentPoint.getMean(), performance));
      }
      monitor.onStepComplete(new Step(currentPoint, currentIteration.get()));
    }
    return new EpochResult(true, pointMean, currentPoint, step);
  }

  @Nonnull
  protected StepResult runStep(@Nonnull final PointSample previousPoint, @Nonnull final TrainingPhase phase) {
    currentIteration.incrementAndGet();
    @Nonnull final TimedResult<LineSearchCursor> timedOrientation = TimedResult
        .time(() -> phase.orientation.orient(phase.trainingSubject, previousPoint, monitor));
    final LineSearchCursor direction = timedOrientation.result;
    final CharSequence directionType = direction.getDirectionType();
    LineSearchStrategy lineSearchStrategy;
    if (phase.lineSearchStrategyMap.containsKey(directionType)) {
      lineSearchStrategy = phase.lineSearchStrategyMap.get(directionType);
    } else {
      monitor.log(String.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = phase.lineSearchFactory.apply(direction.getDirectionType());
      phase.lineSearchStrategyMap.put(directionType, lineSearchStrategy);
    }
    @Nonnull final TimedResult<PointSample> timedLineSearch = TimedResult.time(() -> {
      @Nonnull final FailsafeLineSearchCursor cursor = new FailsafeLineSearchCursor(direction, previousPoint, monitor);
      lineSearchStrategy.step(cursor, monitor);
      //cursor.step(restore.rate, monitor);
      return cursor.getBest(monitor).restore();
    });
    final PointSample bestPoint = timedLineSearch.result;
    if (bestPoint.getMean() > previousPoint.getMean()) {
      throw new IllegalStateException(bestPoint.getMean() + " > " + previousPoint.getMean());
    }
    monitor.log(compare(previousPoint, bestPoint));
    return new StepResult(previousPoint, bestPoint,
        new double[]{timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9});
  }

  protected boolean shouldHalt(@Nonnull final TrainingMonitor monitor, final long timeoutMs) {
    System.currentTimeMillis();
    if (timeoutMs < System.currentTimeMillis()) {
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
    @Nonnull final StateSet<UUID> nextWeights = nextPoint.weights;
    @Nonnull final StateSet<UUID> prevWeights = previousPoint.weights;
    return String.format("Overall network state change: %s",
        prevWeights.stream()
            .collect(RefCollectors.groupingBy(x -> x,
                RefCollectors.toList()))
            .entrySet().stream().collect(RefCollectors.toMap(x -> x.getKey(), list -> {
          final RefList<Double> doubleList = list.getValue().stream()
              .map(prevWeight -> {
                final DoubleBuffer<UUID> dirDelta = nextWeights.getMap().get(prevWeight.key);
                final double numerator = prevWeight.deltaStatistics().rms();
                final double denominator = null == dirDelta ? 0 : dirDelta.deltaStatistics().rms();
                return numerator / (0 == denominator ? 1 : denominator);
              }).collect(RefCollectors.toList());
          if (1 == doubleList.size())
            return Double.toString(doubleList.get(0));
          return new DoubleStatistics().accept(doubleList.stream().mapToDouble(x -> x).toArray()).toString();
        })));
  }

  private PointSample measure(@Nonnull final TrainingPhase phase) {
    int retries = 0;
    do {
      if (10 < retries++)
        throw new IterativeStopException();
      final PointSample currentPoint = phase.trainingSubject.measure(monitor);
      if (Double.isFinite(currentPoint.getMean()))
        return currentPoint;
      phase.orientation.reset();
    } while (true);
  }

  @Nonnull
  private ValidatingTrainer reset(@Nonnull final TrainingPhase phase, final long seed) {
    if (!phase.trainingSubject.reseed(seed))
      throw new IterativeStopException();
    phase.orientation.reset();
    phase.trainingSubject.reseed(seed);
    if (phase.trainingSubject.getLayer() instanceof DAGNetwork) {
      ((DAGNetwork) phase.trainingSubject.getLayer()).shuffle(StochasticComponent.random.get().nextLong());
    }
    return this;
  }

  public static @RefAware
  class TrainingPhase extends ReferenceCountingBase {
    private Function<CharSequence, LineSearchStrategy> lineSearchFactory = (s) -> new ArmijoWolfeSearch();
    private RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new RefHashMap<>();
    private OrientationStrategy<?> orientation = new LBFGS();
    private SampledTrainable trainingSubject;

    public TrainingPhase(final SampledTrainable trainingSubject) {
      setTrainingSubject(trainingSubject);
    }

    public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
      return lineSearchFactory;
    }

    @Nonnull
    public TrainingPhase setLineSearchFactory(final Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
      this.lineSearchFactory = lineSearchFactory;
      return this;
    }

    public RefMap<CharSequence, LineSearchStrategy> getLineSearchStrategyMap() {
      return lineSearchStrategyMap;
    }

    @Nonnull
    public TrainingPhase setLineSearchStrategyMap(
        final RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap) {
      this.lineSearchStrategyMap = lineSearchStrategyMap;
      return this;
    }

    public OrientationStrategy<?> getOrientation() {
      return orientation;
    }

    @Nonnull
    public TrainingPhase setOrientation(final OrientationStrategy<?> orientation) {
      this.orientation = orientation;
      return this;
    }

    public SampledTrainable getTrainingSubject() {
      return trainingSubject;
    }

    @Nonnull
    public void setTrainingSubject(final SampledTrainable trainingSubject) {
      this.trainingSubject = trainingSubject;
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
      this.validation = validation;
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
      this.currentPoint = currentPoint;
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
      this.parent = parent;
    }

    @Override
    public int getTrainingSize() {
      return getInner().getTrainingSize();
    }

    @Nonnull
    @Override
    public void setTrainingSize(final int trainingSize) {
      getInner().setTrainingSize(trainingSize);
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
      return new SampledCachedTrainable<>(this);
    }

    @Override
    public PointSample measure(final TrainingMonitor monitor) {
      @Nonnull final TimedResult<PointSample> time = TimedResult.time(() -> getInner().measure(monitor));
      parent.trainingMeasurementTime.addAndGet(time.timeNanos);
      return time.result;
    }

    public @SuppressWarnings("unused")
    void _free() {
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
      this.currentPoint = currentPoint;
      this.previous = previous;
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
    }

    public @Override
    @SuppressWarnings("unused")
    StepResult addRef() {
      return (StepResult) super.addRef();
    }

  }
}
