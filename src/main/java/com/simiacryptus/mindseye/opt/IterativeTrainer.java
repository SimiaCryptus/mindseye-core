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
import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.IterativeStopException;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.line.*;
import com.simiacryptus.mindseye.opt.orient.LBFGS;
import com.simiacryptus.mindseye.opt.orient.OrientationStrategy;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefString;
import com.simiacryptus.ref.wrappers.RefSystem;
import com.simiacryptus.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * This class represents an Iterative Trainer.
 *
 * @param subject            the trainable subject
 * @param lineSearchFactory  the line search factory
 * @param maxIterations      the maximum number of iterations
 * @param monitor            the training monitor
 * @param orientation        the orientation strategy
 * @param terminateThreshold the termination threshold
 * @param timeout            the timeout
 * @docgenVersion 9
 */
public class IterativeTrainer extends ReferenceCountingBase {
  private static final Logger log = LoggerFactory.getLogger(IterativeTrainer.class);

  private final RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new RefHashMap<>();
  @Nullable
  private final Trainable subject;
  private AtomicInteger currentIteration = new AtomicInteger(0);
  private int iterationsPerSample = 100;
  private Function<CharSequence, LineSearchStrategy> lineSearchFactory = s -> new ArmijoWolfeSearch();
  private int maxIterations = Integer.MAX_VALUE;
  private TrainingMonitor monitor = new TrainingMonitor();
  @Nullable
  private OrientationStrategy<?> orientation = new LBFGS();
  private double terminateThreshold;
  private Duration timeout;

  /**
   * Instantiates a new Iterative trainer.
   *
   * @param subject the subject
   */
  public IterativeTrainer(@Nullable final Trainable subject) {
    this.subject = subject;
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = 0;
  }

  /**
   * Returns the current iteration as an AtomicInteger.
   *
   * @docgenVersion 9
   */
  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  /**
   * Sets the current iteration.
   *
   * @param currentIteration the current iteration
   * @docgenVersion 9
   */
  public void setCurrentIteration(AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
  }

  /**
   * Returns the number of iterations per sample.
   *
   * @docgenVersion 9
   */
  public int getIterationsPerSample() {
    return iterationsPerSample;
  }

  /**
   * Sets the number of iterations per sample.
   *
   * @param iterationsPerSample the number of iterations per sample
   * @docgenVersion 9
   */
  public void setIterationsPerSample(int iterationsPerSample) {
    this.iterationsPerSample = iterationsPerSample;
  }

  /**
   * Returns the line search factory.
   *
   * @docgenVersion 9
   */
  public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
    return lineSearchFactory;
  }

  /**
   * Sets the line search factory to the given function.
   *
   * @param lineSearchFactory the function to use for creating line search strategies
   * @docgenVersion 9
   */
  public void setLineSearchFactory(Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    this.lineSearchFactory = lineSearchFactory;
  }

  /**
   * Returns the maximum number of iterations.
   *
   * @docgenVersion 9
   */
  public int getMaxIterations() {
    return maxIterations;
  }

  /**
   * Sets the maximum number of iterations.
   *
   * @param maxIterations the maximum number of iterations
   * @docgenVersion 9
   */
  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  /**
   * Returns the training monitor.
   *
   * @docgenVersion 9
   */
  public TrainingMonitor getMonitor() {
    return monitor;
  }

  /**
   * Sets the training monitor.
   *
   * @param monitor the training monitor
   * @docgenVersion 9
   */
  public void setMonitor(TrainingMonitor monitor) {
    this.monitor = monitor;
  }

  /**
   * @return the orientation, or null if none has been set
   * @docgenVersion 9
   */
  @Nullable
  public OrientationStrategy<?> getOrientation() {
    return orientation == null ? null : orientation.addRef();
  }

  /**
   * Sets the orientation.
   *
   * @param orientation the new orientation
   * @docgenVersion 9
   */
  public void setOrientation(@Nullable OrientationStrategy<?> orientation) {
    if (null != this.orientation)
      this.orientation.freeRef();
    this.orientation = orientation;
  }

  /**
   * Returns the terminate threshold.
   *
   * @docgenVersion 9
   */
  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  /**
   * Sets the terminate threshold.
   *
   * @param terminateThreshold the new terminate threshold
   * @docgenVersion 9
   */
  public void setTerminateThreshold(double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
  }

  /**
   * Returns the timeout.
   *
   * @docgenVersion 9
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Sets the timeout to the specified duration.
   *
   * @param timeout the timeout duration
   * @docgenVersion 9
   */
  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * @return a PointSample, or null if no measurement could be taken
   * @docgenVersion 9
   */
  @Nullable
  public PointSample measure() {
    assert subject != null;
    final PointSample currentPoint = subject.measure(monitor);
    int size = currentPoint.delta.size();
    if (0 >= size) {
      currentPoint.freeRef();
      throw new AssertionError("Nothing to optimize");
    }
    double mean = currentPoint.getMean();
    if (!Double.isFinite(mean)) {
      if (monitor.onStepFail(new Step(currentPoint, currentIteration.get()))) {
        monitor.log(RefString.format("Retrying iteration %s", currentIteration.get()));
        return measure();
      } else {
        monitor.log(RefString.format("Optimization terminated %s", currentIteration.get()));
        throw new IterativeStopException(Double.toString(mean));
      }
    }
    return currentPoint;
  }

  /**
   * This method shuffles the deck of cards.
   *
   * @docgenVersion 9
   */
  public void shuffle() {
    long seed = RefSystem.nanoTime();
    monitor.log(RefString.format("Reset training subject: " + seed));
    assert orientation != null;
    orientation.reset();
    assert subject != null;
    subject.reseed(seed);
    Layer layer = subject.getLayer();
    try {
      if (layer instanceof DAGNetwork) {
        ((DAGNetwork) layer).shuffle(seed);
      }
    } finally {
      if (null != layer) layer.freeRef();
    }
  }

  /**
   * Runs the training process and returns the result.
   *
   * @docgenVersion 9
   */
  public TrainingResult run() {
    long startTime = RefSystem.currentTimeMillis();
    final long timeoutMs = startTime + timeout.toMillis();
    long lastIterationTime = RefSystem.nanoTime();
    TrainingResult.TerminationCause terminationCause = TrainingResult.TerminationCause.Completed;
    shuffle();
    @Nullable
    PointSample currentPoint = measure();
    try {
      assert currentPoint != null;
      mainLoop:
      while (timeoutMs > RefSystem.currentTimeMillis()
          && terminateThreshold < currentPoint.getMean() && maxIterations > currentIteration.get()) {
        shuffle();
        if (null != currentPoint) currentPoint.freeRef();
        currentPoint = measure();
        for (int subiteration = 0; subiteration < iterationsPerSample || iterationsPerSample <= 0; subiteration++) {
          if (timeoutMs < RefSystem.currentTimeMillis()) {
            terminationCause = TrainingResult.TerminationCause.Timeout;
            break mainLoop;
          }
          if (currentIteration.incrementAndGet() > maxIterations) {
            terminationCause = TrainingResult.TerminationCause.Completed;
            break mainLoop;
          }
          if (null != currentPoint) currentPoint.freeRef();
          currentPoint = measure();
          @Nullable final PointSample _currentPoint = currentPoint == null ? null : currentPoint.addRef();
          @Nonnull final TimedResult<LineSearchCursor> timedOrientation = TimedResult.time(RefUtil.wrapInterface(
              (UncheckedSupplier<LineSearchCursor>) () -> {
                return orientation.orient(
                    subject == null ? null : subject.addRef(),
                    _currentPoint == null ? null : _currentPoint.addRef(),
                    monitor
                );
              }, _currentPoint));
          final LineSearchCursor direction = timedOrientation.getResult();
          final CharSequence directionType = direction.getDirectionType();
          @Nullable final PointSample previous = currentPoint == null ? null : currentPoint.addRef();
          try {
            @Nonnull final TimedResult<PointSample> timedLineSearch = TimedResult.time(RefUtil.wrapInterface(
                (UncheckedSupplier<PointSample>) () -> step(
                    direction.addRef(),
                    directionType,
                    previous == null ? null : previous.addRef()),
                previous == null ? null : previous.addRef(),
                direction.addRef()
            ));
            if (null != currentPoint) currentPoint.freeRef();
            currentPoint = timedLineSearch.getResult();
            final long now = RefSystem.nanoTime();
            final CharSequence perfString = RefString.format("Total: %.4f; Orientation: %.4f; Line Search: %.4f",
                (now - lastIterationTime) / 1e9, timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9);
            timedLineSearch.freeRef();
            lastIterationTime = now;
            assert previous != null;
            monitor.log(RefString.format("Fitness changed from %s to %s", previous.getMean(), currentPoint.getMean()));
            if (previous.getMean() <= currentPoint.getMean()) {
              if (previous.getMean() < currentPoint.getMean()) {
                monitor.log(RefString.format("Resetting Iteration %s", perfString));
                LineSearchPoint temp_18_0005 = direction.step(0, monitor);
                assert temp_18_0005 != null;
                if (null != currentPoint) currentPoint.freeRef();
                currentPoint = temp_18_0005.getPoint();
                temp_18_0005.freeRef();
              } else {
                monitor.log(RefString.format("Static Iteration %s", perfString));
              }

              monitor.log(
                  RefString.format("Iteration %s failed. Error: %s", currentIteration.get(), currentPoint.getMean()));
              monitor.log(RefString.format("Previous Error: %s -> %s", previous.getRate(), previous.getMean()));
              if (monitor.onStepFail(new Step(currentPoint.addRef(), currentIteration.get()))) {
                monitor.log(RefString.format("Retrying iteration %s", currentIteration.get()));
                break;
              } else {
                monitor.log(RefString.format("Optimization terminated %s", currentIteration.get()));
                terminationCause = TrainingResult.TerminationCause.Failed;
                break mainLoop;
              }
            } else {
              monitor.log(RefString.format("Iteration %s complete. Error: %s " + perfString, currentIteration.get(),
                  currentPoint.getMean()));
            }
            monitor.onStepComplete(new Step(currentPoint.addRef(), currentIteration.get()));
          } finally {
            timedOrientation.freeRef();
            previous.freeRef();
            direction.freeRef();
          }
        }
      }
      assert subject != null;
      Layer subjectLayer = subject.getLayer();
      if (subjectLayer instanceof DAGNetwork) {
        ((DAGNetwork) subjectLayer).clearNoise();
      }
      if (null != subjectLayer) subjectLayer.freeRef();
      return new TrainingResult(null == currentPoint ? Double.NaN : currentPoint.getMean(), terminationCause, currentIteration.get());
    } catch (Throwable e) {
      monitor.log(RefString.format("Error %s", Util.toString(e)));
      throw Util.throwException(e);
    } finally {
      monitor.log(RefString.format("Final threshold in iteration %s: %s (> %s) after %.3fs (< %.3fs)",
          currentIteration.get(), null == currentPoint ? null : currentPoint.getMean(), terminateThreshold,
          (RefSystem.currentTimeMillis() - startTime) / 1000.0,
          timeout.toMillis() / 1000.0));
      if (null != currentPoint) currentPoint.freeRef();
    }
  }

  /**
   * Sets the timeout to the given number of units.
   *
   * @param number the number of units
   * @param units  the units of time
   * @docgenVersion 9
   */
  public void setTimeout(int number, @Nonnull TemporalUnit units) {
    timeout = Duration.of(number, units);
  }

  /**
   * Sets the timeout for this request.
   *
   * @param number the timeout value
   * @param units  the timeout units
   * @docgenVersion 9
   */
  public void setTimeout(int number, @Nonnull TimeUnit units) {
    setTimeout(number, Util.cvt(units));
  }

  /**
   * @param direction     the direction to step in
   * @param directionType the type of direction (e.g. "gradient")
   * @param previous      the previous point sample
   * @return the new point sample, or null if no step could be taken
   * @docgenVersion 9
   */
  @Nullable
  public PointSample step(@Nonnull final LineSearchCursor direction, final CharSequence directionType,
                          @Nonnull final PointSample previous) {
    LineSearchStrategy lineSearchStrategy;
    if (lineSearchStrategyMap.containsKey(directionType)) {
      lineSearchStrategy = lineSearchStrategyMap.get(directionType);
    } else {
      log.info(RefString.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = lineSearchFactory.apply(direction.getDirectionType());
      RefUtil.freeRef(lineSearchStrategyMap.put(directionType, lineSearchStrategy));
    }
    @Nonnull final FailsafeLineSearchCursor wrapped = new FailsafeLineSearchCursor(direction,
        previous, monitor);
    assert lineSearchStrategy != null;
    try {
      RefUtil.freeRef(lineSearchStrategy.step(wrapped.addRef(), monitor));
      return wrapped.getBest();
    } finally {
      wrapped.freeRef();
    }
  }

  /**
   * Frees resources used by this object.
   * This method calls the super method, then frees resources used by this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != orientation)
      orientation.freeRef();
    orientation = null;
    if (null != subject)
      subject.freeRef();
    lineSearchStrategyMap.freeRef();
  }

  /**
   * Adds a reference to this trainer.
   *
   * @return the trainer with the added reference
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  IterativeTrainer addRef() {
    return (IterativeTrainer) super.addRef();
  }
}
