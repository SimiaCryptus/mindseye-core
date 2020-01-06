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
import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.IterativeStopException;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.line.*;
import com.simiacryptus.mindseye.opt.orient.LBFGS;
import com.simiacryptus.mindseye.opt.orient.OrientationStrategy;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefString;
import com.simiacryptus.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public @RefAware
class IterativeTrainer extends ReferenceCountingBase {
  private static final Logger log = LoggerFactory.getLogger(IterativeTrainer.class);

  private final RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new RefHashMap<>();
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
    {
      Trainable temp_18_0001 = subject == null ? null : subject.addRef();
      this.subject = temp_18_0001 == null ? null : temp_18_0001.addRef();
      if (null != temp_18_0001)
        temp_18_0001.freeRef();
    }
    if (null != subject)
      subject.freeRef();
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = 0;
  }

  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  @Nonnull
  public IterativeTrainer setCurrentIteration(final AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
    return this.addRef();
  }

  public int getIterationsPerSample() {
    return iterationsPerSample;
  }

  @Nonnull
  public IterativeTrainer setIterationsPerSample(final int iterationsPerSample) {
    this.iterationsPerSample = iterationsPerSample;
    return this.addRef();
  }

  public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
    return lineSearchFactory;
  }

  @Nonnull
  public IterativeTrainer setLineSearchFactory(final Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    this.lineSearchFactory = lineSearchFactory;
    return this.addRef();
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  @Nonnull
  public IterativeTrainer setMaxIterations(final int maxIterations) {
    this.maxIterations = maxIterations;
    return this.addRef();
  }

  public TrainingMonitor getMonitor() {
    return monitor;
  }

  @Nonnull
  public IterativeTrainer setMonitor(final TrainingMonitor monitor) {
    this.monitor = monitor;
    return this.addRef();
  }

  public OrientationStrategy<?> getOrientation() {
    return orientation == null ? null : orientation.addRef();
  }

  @Nonnull
  public IterativeTrainer setOrientation(final OrientationStrategy<?> orientation) {
    {
      OrientationStrategy<?> temp_18_0002 = orientation == null ? null
          : orientation.addRef();
      if (null != this.orientation)
        this.orientation.freeRef();
      this.orientation = temp_18_0002 == null ? null : temp_18_0002.addRef();
      if (null != temp_18_0002)
        temp_18_0002.freeRef();
    }
    if (null != orientation)
      orientation.freeRef();
    return this.addRef();
  }

  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  @Nonnull
  public IterativeTrainer setTerminateThreshold(final double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
    return this.addRef();
  }

  public Duration getTimeout() {
    return timeout;
  }

  @Nonnull
  public IterativeTrainer setTimeout(final Duration timeout) {
    this.timeout = timeout;
    return this.addRef();
  }

  public static @SuppressWarnings("unused")
  IterativeTrainer[] addRefs(IterativeTrainer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(IterativeTrainer::addRef)
        .toArray((x) -> new IterativeTrainer[x]);
  }

  public static @SuppressWarnings("unused")
  IterativeTrainer[][] addRefs(IterativeTrainer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(IterativeTrainer::addRefs)
        .toArray((x) -> new IterativeTrainer[x][]);
  }

  @Nullable
  public PointSample measure() {
    @Nullable
    PointSample currentPoint = null;
    currentPoint = subject.measure(monitor);
    RefMap<UUID, Delta<UUID>> temp_18_0004 = currentPoint.delta
        .getMap();
    if (0 >= temp_18_0004.size()) {
      if (null != currentPoint)
        currentPoint.freeRef();
      throw new AssertionError("Nothing to optimize");
    }
    if (null != temp_18_0004)
      temp_18_0004.freeRef();
    double mean = currentPoint.getMean();
    if (!Double.isFinite(mean)) {
      if (monitor.onStepFail(new Step(currentPoint == null ? null : currentPoint.addRef(), currentIteration.get()))) {
        monitor.log(RefString.format("Retrying iteration %s", currentIteration.get()));
        if (null != currentPoint)
          currentPoint.freeRef();
        return measure();
      } else {
        monitor.log(RefString.format("Optimization terminated %s", currentIteration.get()));
        if (null != currentPoint)
          currentPoint.freeRef();
        throw new IterativeStopException(Double.toString(mean));
      }

    }
    return currentPoint;
  }

  public void shuffle() {
    long seed = com.simiacryptus.ref.wrappers.RefSystem.nanoTime();
    monitor.log(RefString.format("Reset training subject: " + seed));
    orientation.reset();
    subject.reseed(seed);
    if (subject.getLayer() instanceof DAGNetwork) {
      ((DAGNetwork) subject.getLayer()).shuffle(seed);
    }
  }

  public double run() {
    long startTime = com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis();
    final long timeoutMs = startTime + timeout.toMillis();
    long lastIterationTime = com.simiacryptus.ref.wrappers.RefSystem.nanoTime();
    shuffle();
    @Nullable
    PointSample currentPoint = measure();
    try {
mainLoop:
      while (timeoutMs > com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis() && terminateThreshold < currentPoint.getMean()
          && maxIterations > currentIteration.get()) {
        shuffle();
        currentPoint = null;
        currentPoint = measure();
        for (int subiteration = 0; subiteration < iterationsPerSample || iterationsPerSample <= 0; subiteration++) {
          if (timeoutMs < com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis()) {
            break mainLoop;
          }
          if (currentIteration.incrementAndGet() > maxIterations) {
            break mainLoop;
          }
          currentPoint = null;
          currentPoint = measure();
          @Nullable final PointSample _currentPoint = currentPoint == null ? null : currentPoint.addRef();
          @Nonnull final TimedResult<LineSearchCursor> timedOrientation = TimedResult
              .time(RefUtil.wrapInterface(
                  (UncheckedSupplier<LineSearchCursor>) () -> orientation
                      .orient(subject == null ? null : subject.addRef(),
                          _currentPoint == null ? null : _currentPoint.addRef(), monitor),
                  _currentPoint == null ? null : _currentPoint.addRef()));
          if (null != _currentPoint)
            _currentPoint.freeRef();
          final LineSearchCursor direction = timedOrientation.result.addRef();
          final CharSequence directionType = direction.getDirectionType();
          @Nullable final PointSample previous = currentPoint == null ? null : currentPoint.addRef();
          {
            @Nonnull final TimedResult<PointSample> timedLineSearch = TimedResult
                .time(RefUtil.wrapInterface(
                    (UncheckedSupplier<PointSample>) () -> step(
                        direction == null ? null : direction.addRef(), directionType,
                        previous == null ? null : previous.addRef()),
                    previous == null ? null : previous.addRef(), direction == null ? null : direction.addRef()));
            currentPoint = null;
            currentPoint = timedLineSearch.result.addRef();
            final long now = com.simiacryptus.ref.wrappers.RefSystem.nanoTime();
            final CharSequence perfString = RefString.format("Total: %.4f; Orientation: %.4f; Line Search: %.4f",
                (now - lastIterationTime) / 1e9, timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9);
            lastIterationTime = now;
            monitor.log(RefString.format("Fitness changed from %s to %s", previous.getMean(), currentPoint.getMean()));
            if (previous.getMean() <= currentPoint.getMean()) {
              if (previous.getMean() < currentPoint.getMean()) {
                monitor.log(RefString.format("Resetting Iteration %s", perfString));
                currentPoint = null;
                LineSearchPoint temp_18_0005 = direction.step(0, monitor);
                currentPoint = temp_18_0005.point.addRef();
                if (null != temp_18_0005)
                  temp_18_0005.freeRef();
              } else {
                monitor.log(RefString.format("Static Iteration %s", perfString));
              }

              monitor
                  .log(RefString.format("Iteration %s failed. Error: %s", currentIteration.get(), currentPoint.getMean()));
              monitor.log(RefString.format("Previous Error: %s -> %s", previous.getRate(), previous.getMean()));
              if (monitor
                  .onStepFail(new Step(currentPoint == null ? null : currentPoint.addRef(), currentIteration.get()))) {
                monitor.log(RefString.format("Retrying iteration %s", currentIteration.get()));

                break;
              } else {
                monitor.log(RefString.format("Optimization terminated %s", currentIteration.get()));
                break mainLoop;
              }
            } else {
              monitor.log(RefString.format("Iteration %s complete. Error: %s " + perfString, currentIteration.get(),
                  currentPoint.getMean()));
            }
            monitor
                .onStepComplete(new Step(currentPoint == null ? null : currentPoint.addRef(), currentIteration.get()));
          }
          if (null != previous)
            previous.freeRef();
          if (null != direction)
            direction.freeRef();
        }
      }
      if (subject.getLayer() instanceof DAGNetwork) {
        ((DAGNetwork) subject.getLayer()).clearNoise();
      }
      double temp_18_0003 = null == currentPoint ? Double.NaN : currentPoint.getMean();
      if (null != currentPoint)
        currentPoint.freeRef();
      return temp_18_0003;
    } catch (Throwable e) {
      monitor.log(RefString.format("Error %s", Util.toString(e)));
      throw new RuntimeException(e);
    } finally {
      monitor.log(RefString.format("Final threshold in iteration %s: %s (> %s) after %.3fs (< %.3fs)",
          currentIteration.get(), null == currentPoint ? null : currentPoint.getMean(), terminateThreshold,
          (com.simiacryptus.ref.wrappers.RefSystem.currentTimeMillis() - startTime) / 1000.0, timeout.toMillis() / 1000.0));
    }
  }

  @Nonnull
  public IterativeTrainer setTimeout(final int number, @Nonnull final TemporalUnit units) {
    timeout = Duration.of(number, units);
    return this.addRef();
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
      log.info(RefString.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = lineSearchFactory.apply(direction.getDirectionType());
      lineSearchStrategyMap.put(directionType, lineSearchStrategy);
    }
    @Nonnull final FailsafeLineSearchCursor wrapped = new FailsafeLineSearchCursor(direction == null ? null : direction,
        previous == null ? null : previous, monitor);
    RefUtil
        .freeRef(lineSearchStrategy.step(wrapped == null ? null : wrapped.addRef(), monitor));
    currentPoint = wrapped.getBest(monitor);
    wrapped.freeRef();
    return currentPoint;
  }

  public void _free() {
    if (null != orientation)
      orientation.freeRef();
    orientation = null;
    if (null != subject)
      subject.freeRef();
    if (null != lineSearchStrategyMap)
      lineSearchStrategyMap.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  IterativeTrainer addRef() {
    return (IterativeTrainer) super.addRef();
  }
}
