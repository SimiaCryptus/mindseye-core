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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class IterativeTrainer extends ReferenceCountingBase {
  private static final Logger log = LoggerFactory.getLogger(IterativeTrainer.class);

  private final RefMap<CharSequence, LineSearchStrategy> lineSearchStrategyMap = new RefHashMap<>();
  @Nullable
  private final Trainable subject;
  private AtomicInteger currentIteration = new AtomicInteger(0);
  private int iterationsPerSample = 100;
  private Function<CharSequence, LineSearchStrategy> lineSearchFactory = (s) -> new ArmijoWolfeSearch();
  private int maxIterations = Integer.MAX_VALUE;
  private TrainingMonitor monitor = new TrainingMonitor();
  @Nullable
  private OrientationStrategy<?> orientation = new LBFGS();
  private double terminateThreshold;
  private Duration timeout;

  public IterativeTrainer(@Nullable final Trainable subject) {
    Trainable temp_18_0001 = subject == null ? null : subject.addRef();
    this.subject = temp_18_0001 == null ? null : temp_18_0001.addRef();
    if (null != temp_18_0001)
      temp_18_0001.freeRef();
    if (null != subject)
      subject.freeRef();
    timeout = Duration.of(5, ChronoUnit.MINUTES);
    terminateThreshold = 0;
  }

  public AtomicInteger getCurrentIteration() {
    return currentIteration;
  }

  public void setCurrentIteration(AtomicInteger currentIteration) {
    this.currentIteration = currentIteration;
  }

  public int getIterationsPerSample() {
    return iterationsPerSample;
  }

  public void setIterationsPerSample(int iterationsPerSample) {
    this.iterationsPerSample = iterationsPerSample;
  }

  public Function<CharSequence, LineSearchStrategy> getLineSearchFactory() {
    return lineSearchFactory;
  }

  public void setLineSearchFactory(Function<CharSequence, LineSearchStrategy> lineSearchFactory) {
    this.lineSearchFactory = lineSearchFactory;
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  public TrainingMonitor getMonitor() {
    return monitor;
  }

  public void setMonitor(TrainingMonitor monitor) {
    this.monitor = monitor;
  }

  @Nullable
  public OrientationStrategy<?> getOrientation() {
    return orientation == null ? null : orientation.addRef();
  }

  public void setOrientation(@Nullable OrientationStrategy<?> orientation) {
    OrientationStrategy<?> temp_18_0002 = orientation == null ? null : orientation.addRef();
    if (null != this.orientation)
      this.orientation.freeRef();
    this.orientation = temp_18_0002 == null ? null : temp_18_0002.addRef();
    if (null != temp_18_0002)
      temp_18_0002.freeRef();
    if (null != orientation)
      orientation.freeRef();
  }

  public double getTerminateThreshold() {
    return terminateThreshold;
  }

  public void setTerminateThreshold(double terminateThreshold) {
    this.terminateThreshold = terminateThreshold;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  @Nullable
  public PointSample measure() {
    @Nullable
    PointSample currentPoint = null;
    assert subject != null;
    if (null != currentPoint) currentPoint.freeRef();
    currentPoint = subject.measure(monitor);
    RefMap<UUID, Delta<UUID>> temp_18_0004 = currentPoint.delta.getMap();
    if (0 >= temp_18_0004.size()) {
      temp_18_0004.freeRef();
      currentPoint.freeRef();
      throw new AssertionError("Nothing to optimize");
    }
    temp_18_0004.freeRef();
    double mean = currentPoint.getMean();
    if (!Double.isFinite(mean)) {
      if (monitor.onStepFail(new Step(currentPoint.addRef(), currentIteration.get()))) {
        monitor.log(RefString.format("Retrying iteration %s", currentIteration.get()));
        currentPoint.freeRef();
        return measure();
      } else {
        monitor.log(RefString.format("Optimization terminated %s", currentIteration.get()));
        currentPoint.freeRef();
        throw new IterativeStopException(Double.toString(mean));
      }
    }
    return currentPoint;
  }

  public void shuffle() {
    long seed = RefSystem.nanoTime();
    monitor.log(RefString.format("Reset training subject: " + seed));
    assert orientation != null;
    orientation.reset();
    assert subject != null;
    subject.reseed(seed);
    Layer layer = subject.getLayer();
    if (layer instanceof DAGNetwork) {
      ((DAGNetwork) layer).shuffle(seed);
    }
    layer.freeRef();
  }

  public double run() {
    long startTime = RefSystem.currentTimeMillis();
    final long timeoutMs = startTime + timeout.toMillis();
    long lastIterationTime = RefSystem.nanoTime();
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
            break mainLoop;
          }
          if (currentIteration.incrementAndGet() > maxIterations) {
            break mainLoop;
          }
          if (null != currentPoint) currentPoint.freeRef();
          currentPoint = measure();
          @Nullable final PointSample _currentPoint = currentPoint == null ? null : currentPoint.addRef();
          @Nonnull final TimedResult<LineSearchCursor> timedOrientation = TimedResult.time(RefUtil.wrapInterface(
              (UncheckedSupplier<LineSearchCursor>) () -> {
                assert orientation != null;
                return orientation.orient(subject == null ? null : subject.addRef(),
                    _currentPoint == null ? null : _currentPoint.addRef(), monitor);
              },
              _currentPoint == null ? null : _currentPoint.addRef()));
          if (null != _currentPoint)
            _currentPoint.freeRef();
          final LineSearchCursor direction = timedOrientation.getResult();
          final CharSequence directionType = direction.getDirectionType();
          @Nullable final PointSample previous = currentPoint == null ? null : currentPoint.addRef();
          @Nonnull final TimedResult<PointSample> timedLineSearch = TimedResult.time(RefUtil.wrapInterface(
              (UncheckedSupplier<PointSample>) () -> step(direction.addRef(), directionType,
                  previous == null ? null : previous.addRef()),
              previous == null ? null : previous.addRef(), direction.addRef()));
          if (null != currentPoint) currentPoint.freeRef();
          currentPoint = timedLineSearch.getResult();
          final long now = RefSystem.nanoTime();
          final CharSequence perfString = RefString.format("Total: %.4f; Orientation: %.4f; Line Search: %.4f",
              (now - lastIterationTime) / 1e9, timedOrientation.timeNanos / 1e9, timedLineSearch.timeNanos / 1e9);
          timedLineSearch.freeRef();
          timedOrientation.freeRef();
          lastIterationTime = now;
          assert previous != null;
          monitor.log(RefString.format("Fitness changed from %s to %s", previous.getMean(), currentPoint.getMean()));
          if (previous.getMean() <= currentPoint.getMean()) {
            if (previous.getMean() < currentPoint.getMean()) {
              monitor.log(RefString.format("Resetting Iteration %s", perfString));
              LineSearchPoint temp_18_0005 = direction.step(0, monitor);
              assert temp_18_0005 != null;
              assert temp_18_0005.point != null;
              if (null != currentPoint) currentPoint.freeRef();
              currentPoint = temp_18_0005.point.addRef();
              temp_18_0005.freeRef();
            } else {
              monitor.log(RefString.format("Static Iteration %s", perfString));
            }

            monitor.log(
                RefString.format("Iteration %s failed. Error: %s", currentIteration.get(), currentPoint.getMean()));
            monitor.log(RefString.format("Previous Error: %s -> %s", previous.getRate(), previous.getMean()));
            if (monitor
                .onStepFail(new Step(currentPoint.addRef(), currentIteration.get()))) {
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
          monitor.onStepComplete(new Step(currentPoint.addRef(), currentIteration.get()));
          previous.freeRef();
          direction.freeRef();
        }
      }
      assert subject != null;
      Layer subjectLayer = subject.getLayer();
      if (subjectLayer instanceof DAGNetwork) {
        ((DAGNetwork) subjectLayer).clearNoise();
      }
      subjectLayer.freeRef();
      return null == currentPoint ? Double.NaN : currentPoint.getMean();
    } catch (Throwable e) {
      monitor.log(RefString.format("Error %s", Util.toString(e)));
      throw new RuntimeException(e);
    } finally {
      monitor.log(RefString.format("Final threshold in iteration %s: %s (> %s) after %.3fs (< %.3fs)",
          currentIteration.get(), null == currentPoint ? null : currentPoint.getMean(), terminateThreshold,
          (RefSystem.currentTimeMillis() - startTime) / 1000.0,
          timeout.toMillis() / 1000.0));
      if (null != currentPoint) currentPoint.freeRef();
    }
  }

  public void setTimeout(int number, @Nonnull TemporalUnit units) {
    timeout = Duration.of(number, units);
  }

  public void setTimeout(int number, @Nonnull TimeUnit units) {
    setTimeout(number, Util.cvt(units));
  }

  @Nullable
  public PointSample step(@Nonnull final LineSearchCursor direction, final CharSequence directionType,
                          @Nonnull final PointSample previous) {
    LineSearchStrategy lineSearchStrategy;
    if (lineSearchStrategyMap.containsKey(directionType)) {
      lineSearchStrategy = lineSearchStrategyMap.get(directionType);
    } else {
      log.info(RefString.format("Constructing line search parameters: %s", directionType));
      lineSearchStrategy = lineSearchFactory.apply(direction.getDirectionType());
      lineSearchStrategyMap.put(directionType, lineSearchStrategy);
    }
    @Nonnull final FailsafeLineSearchCursor wrapped = new FailsafeLineSearchCursor(direction,
        previous, monitor);
    assert lineSearchStrategy != null;
    RefUtil.freeRef(lineSearchStrategy.step(wrapped.addRef(), monitor));
    PointSample currentPoint = wrapped.getBest(monitor);
    wrapped.freeRef();
    return currentPoint;
  }

  public void _free() {
    super._free();
    if (null != orientation)
      orientation.freeRef();
    orientation = null;
    if (null != subject)
      subject.freeRef();
    lineSearchStrategyMap.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  IterativeTrainer addRef() {
    return (IterativeTrainer) super.addRef();
  }
}
