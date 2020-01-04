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
import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.IterativeStopException;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.line.ArmijoWolfeSearch;
import com.simiacryptus.mindseye.opt.line.FailsafeLineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchStrategy;
import com.simiacryptus.mindseye.opt.orient.LBFGS;
import com.simiacryptus.mindseye.opt.orient.OrientationStrategy;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefMap;

public @com.simiacryptus.ref.lang.RefAware class IterativeTrainer extends ReferenceCountingBase {
  private static final Logger log = LoggerFactory.getLogger(IterativeTrainer.class);

  private final com.simiacryptus.ref.wrappers.RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
  private final Trainable subject;
  private AtomicInteger currentIteration = new AtomicInteger(0);
  private int iterationsPerSample = 100;
  private Function<CharSequence, LineSearchStrategy> lineSearchFactory = (s) -> new ArmijoWolfeSearch();
  private int maxIterations = Integer.MAX_VALUE;
  private TrainingMonitor monitor = new TrainingMonitor();
  private OrientationStrategy<?> orientation = new LBFGS();
  private double terminateThreshold;
  private Duration timeout;

  public IterativeTrainer(final Trainable subject) {
    this.subject = subject;
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = 0;
  }

  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  @Nonnull
  public IterativeTrainer setCurrentIteration(final AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
    return this;
  }

  public int getIterationsPerSample() {
    return iterationsPerSample;
  }

  @Nonnull
  public IterativeTrainer setIterationsPerSample(final int iterationsPerSample) {
    this.iterationsPerSample = iterationsPerSample;
    return this;
  }

  public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
    return lineSearchFactory;
  }

  @Nonnull
  public IterativeTrainer setLineSearchFactory(final Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    this.lineSearchFactory = lineSearchFactory;
    return this;
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  @Nonnull
  public IterativeTrainer setMaxIterations(final int maxIterations) {
    this.maxIterations = maxIterations;
    return this;
  }

  public TrainingMonitor getMonitor() {
    return monitor;
  }

  @Nonnull
  public IterativeTrainer setMonitor(final TrainingMonitor monitor) {
    this.monitor = monitor;
    return this;
  }

  public OrientationStrategy<?> getOrientation() {
    return orientation;
  }

  @Nonnull
  public IterativeTrainer setOrientation(final OrientationStrategy<?> orientation) {
    this.orientation = orientation;
    return this;
  }

  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  @Nonnull
  public IterativeTrainer setTerminateThreshold(final double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
    return this;
  }

  public Duration getTimeout() {
    return timeout;
  }

  @Nonnull
  public IterativeTrainer setTimeout(final Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  @Nullable
  public PointSample measure() {
    @Nullable
    PointSample currentPoint = null;
    currentPoint = subject.measure(monitor);
    if (0 >= currentPoint.delta.getMap().size()) {
      throw new AssertionError("Nothing to optimize");
    }
    double mean = currentPoint.getMean();
    if (!Double.isFinite(mean)) {
      if (monitor.onStepFail(new Step(currentPoint, currentIteration.get()))) {
        monitor.log(String.format("Retrying iteration %s", currentIteration.get()));
        return measure();
      } else {
        monitor.log(String.format("Optimization terminated %s", currentIteration.get()));
        throw new IterativeStopException(Double.toString(mean));
      }

    }
    return currentPoint;
  }

  public void shuffle() {
    long seed = System.nanoTime();
    monitor.log(String.format("Reset training subject: " + seed));
    orientation.reset();
    subject.reseed(seed);
    if (subject.getLayer() instanceof DAGNetwork) {
      ((DAGNetwork) subject.getLayer()).shuffle(seed);
    }
  }

  public double run() {
    long startTime = System.currentTimeMillis();
    final long timeoutMs = startTime + timeout.toMillis();
    long lastIterationTime = System.nanoTime();
    shuffle();
    @Nullable
    PointSample currentPoint = measure();
    try {
      mainLoop: while (timeoutMs > System.currentTimeMillis() && terminateThreshold < currentPoint.getMean()
          && maxIterations > currentIteration.get()) {
        shuffle();
        currentPoint = null;
        currentPoint = measure();
        for (int subiteration = 0; subiteration < iterationsPerSample || iterationsPerSample <= 0; subiteration++) {
          if (timeoutMs < System.currentTimeMillis()) {
            break mainLoop;
          }
          if (currentIteration.incrementAndGet() > maxIterations) {
            break mainLoop;
          }
          currentPoint = null;
          currentPoint = measure();
          @Nullable
          final PointSample _currentPoint = currentPoint;
          @Nonnull
          final TimedResult<LineSearchCursor> timedOrientation = TimedResult
              .time(() -> orientation.orient(subject, _currentPoint, monitor));
          final LineSearchCursor direction = timedOrientation.result;
          final CharSequence directionType = direction.getDirectionType();
          @Nullable
          final PointSample previous = currentPoint;
          {
            @Nonnull
            final TimedResult<PointSample> timedLineSearch = TimedResult
                .time(() -> step(direction, directionType, previous));
            currentPoint = null;
            currentPoint = timedLineSearch.result;
            final long now = System.nanoTime();
            final CharSequence perfString = String.format("Total: %.4f; Orientation: %.4f; Line Search: %.4f",
                (now - lastIterationTime) / 1e9, timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9);
            lastIterationTime = now;
            monitor.log(String.format("Fitness changed from %s to %s", previous.getMean(), currentPoint.getMean()));
            if (previous.getMean() <= currentPoint.getMean()) {
              if (previous.getMean() < currentPoint.getMean()) {
                monitor.log(String.format("Resetting Iteration %s", perfString));
                currentPoint = null;
                currentPoint = direction.step(0, monitor).point;
              } else {
                monitor.log(String.format("Static Iteration %s", perfString));
              }

              monitor
                  .log(String.format("Iteration %s failed. Error: %s", currentIteration.get(), currentPoint.getMean()));
              monitor.log(String.format("Previous Error: %s -> %s", previous.getRate(), previous.getMean()));
              if (monitor.onStepFail(new Step(currentPoint, currentIteration.get()))) {
                monitor.log(String.format("Retrying iteration %s", currentIteration.get()));

                break;
              } else {
                monitor.log(String.format("Optimization terminated %s", currentIteration.get()));
                break mainLoop;
              }
            } else {
              monitor.log(String.format("Iteration %s complete. Error: %s " + perfString, currentIteration.get(),
                  currentPoint.getMean()));
            }
            monitor.onStepComplete(new Step(currentPoint, currentIteration.get()));
          }
        }
      }
      if (subject.getLayer() instanceof DAGNetwork) {
        ((DAGNetwork) subject.getLayer()).clearNoise();
      }
      return null == currentPoint ? Double.NaN : currentPoint.getMean();
    } catch (Throwable e) {
      monitor.log(String.format("Error %s", Util.toString(e)));
      throw new RuntimeException(e);
    } finally {
      monitor.log(String.format("Final threshold in iteration %s: %s (> %s) after %.3fs (< %.3fs)",
          currentIteration.get(), null == currentPoint ? null : currentPoint.getMean(), terminateThreshold,
          (System.currentTimeMillis() - startTime) / 1000.0, timeout.toMillis() / 1000.0));
    }
  }

  @Nonnull
  public IterativeTrainer setTimeout(final int number, @Nonnull final TemporalUnit units) {
    timeout = Duration.of(number, units);
    return this;
  }

  @Nonnull
  public IterativeTrainer setTimeout(final int number, @Nonnull final TimeUnit units) {
    return setTimeout(number, Util.cvt(units));
  }

  public PointSample step(@Nonnull final LineSearchCursor direction, final CharSequence directionType,
      @Nonnull final PointSample previous) {
    PointSample currentPoint;
    LineSearchStrategy lineSearchStrategy;
    if (lineSearchStrategyMap.containsKey(directionType)) {
      lineSearchStrategy = lineSearchStrategyMap.get(directionType);
    } else {
      log.info(String.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = lineSearchFactory.apply(direction.getDirectionType());
      lineSearchStrategyMap.put(directionType, lineSearchStrategy);
    }
    @Nonnull
    final FailsafeLineSearchCursor wrapped = new FailsafeLineSearchCursor(direction, previous, monitor);
    lineSearchStrategy.step(wrapped, monitor);
    currentPoint = wrapped.getBest(monitor);
    return currentPoint;
  }

  public void _free() {
  }

  public @Override @SuppressWarnings("unused") IterativeTrainer addRef() {
    return (IterativeTrainer) super.addRef();
  }

  public static @SuppressWarnings("unused") IterativeTrainer[] addRefs(IterativeTrainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(IterativeTrainer::addRef)
        .toArray((x) -> new IterativeTrainer[x]);
  }

  public static @SuppressWarnings("unused") IterativeTrainer[][] addRefs(IterativeTrainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(IterativeTrainer::addRefs)
        .toArray((x) -> new IterativeTrainer[x][]);
  }
}
