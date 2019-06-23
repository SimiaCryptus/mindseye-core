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

package com.simiacryptus.mindseye.opt.orient;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.DoubleBufferSet;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchPoint;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class LBFGS extends OrientationStrategyBase<SimpleLineSearchCursor> {

  private final TreeSet<PointSample> history = new TreeSet<>(Comparator.comparing(x -> -x.getMean()));

  protected boolean verbose = true;
  private int maxHistory = 30;
  private int minHistory = 3;

  private static boolean isFinite(@Nonnull final DoubleBufferSet<?, ?> delta) {
    return delta.stream().parallel().flatMapToDouble(y -> Arrays.stream(y.getDelta())).allMatch(d -> Double.isFinite(d));
  }

  public void addToHistory(@Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor) {
    if (!LBFGS.isFinite(measurement.delta)) {
      if (verbose) {
        monitor.log("Corrupt evalInputDelta measurement");
      }
    } else if (!LBFGS.isFinite(measurement.weights)) {
      if (verbose) {
        monitor.log("Corrupt weights measurement");
      }
    } else {
      boolean isFound = history.stream().filter(x -> x.getMean() <= measurement.getMean()).findAny().isPresent();
      if (!isFound) {
        @Nonnull final PointSample copyFull = measurement.copyFull();
        if (verbose) {
          monitor.log(String.format("Adding measurement %s to history. Total: %s", Long.toHexString(System.identityHashCode(copyFull)), history.size()));
        }
        if (!history.add(copyFull)) {
          copyFull.freeRef();
        }
      } else if (verbose) {
        monitor.log(String.format("Non-optimal measurement %s < %s. Total: %s", measurement.sum, history.stream().mapToDouble(x -> x.sum).min().orElse(Double.POSITIVE_INFINITY), history.size()));
      }
    }
  }

  @Nonnull
  private SimpleLineSearchCursor cursor(final Trainable subject, @Nonnull final PointSample measurement, final String type, final DeltaSet<UUID> result) {
    return new SimpleLineSearchCursor(subject, measurement, result) {
      @Override
      public LineSearchPoint step(final double t, @Nonnull final TrainingMonitor monitor) {
        final LineSearchPoint measure = super.step(t, monitor);
        addToHistory(measure.point, monitor);
        return measure;
      }
    }.setDirectionType(type);
  }

  public int getMaxHistory() {
    return maxHistory;
  }

  @Nonnull
  public LBFGS setMaxHistory(final int maxHistory) {
    this.maxHistory = maxHistory;
    return this;
  }

  public int getMinHistory() {
    return minHistory;
  }

  @Nonnull
  public LBFGS setMinHistory(final int minHistory) {
    this.minHistory = minHistory;
    return this;
  }

  @Nullable
  protected DeltaSet<UUID> lbfgs(@Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor, @Nonnull final List<PointSample> history) {
    @Nonnull final DeltaSet<UUID> result = measurement.delta.scale(-1);
    if (history.size() > minHistory) {
      if (lbfgs(measurement, monitor, history, result)) {
        setHistory(monitor, history);
        return result;
      } else {
        result.freeRef();
        monitor.log("Orientation rejected. Popping history element from " + history.stream().map(x -> String.format("%s", x.getMean())).reduce((a, b) -> a + ", " + b).get());
        return lbfgs(measurement, monitor, history.subList(0, history.size() - 1));
      }
    } else {
      result.freeRef();
      monitor.log(String.format("LBFGS Accumulation History: %s points", history.size()));
      return null;
    }
  }

  private LBFGS setHistory(@Nonnull final TrainingMonitor monitor, @Nonnull final List<PointSample> history) {
    if (history.size() == this.history.size() && history.stream().filter(x -> !this.history.contains(x)).count() == 0)
      return this;
    if (verbose) {
      monitor.log(String.format("Overwriting history with %s points", history.size()));
    }
    synchronized (this.history) {
      history.forEach(ReferenceCountingBase::addRef);
      this.history.forEach(ReferenceCountingBase::freeRef);
      this.history.clear();
      this.history.addAll(history);
    }
    return this;
  }

  private boolean lbfgs(@Nonnull PointSample measurement, @Nonnull TrainingMonitor monitor, @Nonnull List<PointSample> history, @Nonnull DeltaSet<UUID> direction) {
    @Nonnull DeltaSet<UUID> p = null;
    try {
      p = measurement.delta.copy();
      if (!p.stream().parallel().allMatch(y -> Arrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d)))) {
        throw new IllegalStateException("Non-finite value");
      }
      @Nonnull final double[] alphas = new double[history.size()];
      for (int i = history.size() - 2; i >= 0; i--) {
        @Nonnull final DeltaSet<UUID> sd = history.get(i + 1).weights.subtract(history.get(i).weights);
        @Nonnull final DeltaSet<UUID> yd = history.get(i + 1).delta.subtract(history.get(i).delta);
        final double denominator = sd.dot(yd);
        if (0 == denominator) {
          sd.freeRef();
          yd.freeRef();
          throw new IllegalStateException("Orientation vanished.");
        }
        alphas[i] = p.dot(sd) / denominator;
        sd.freeRef();
        DeltaSet<UUID> scale = yd.scale(alphas[i]);
        yd.freeRef();
        {
          DeltaSet<UUID> subtract = p.subtract(scale);
          p.freeRef();
          scale.freeRef();
          p = subtract;
        }
        if ((!p.stream().parallel().allMatch(y -> Arrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d))))) {
          throw new IllegalStateException("Non-finite value");
        }
      }
      @Nonnull final DeltaSet<UUID> sk = history.get(history.size() - 1).weights.subtract(history.get(history.size() - 2).weights);
      @Nonnull final DeltaSet<UUID> yk = history.get(history.size() - 1).delta.subtract(history.get(history.size() - 2).delta);
      {
        double dot = sk.dot(yk);
        sk.freeRef();
        double f = dot / yk.dot(yk);
        yk.freeRef();
        DeltaSet<UUID> scale = p.scale(f);
        p.freeRef();
        p = scale;
      }
      if (!p.stream().parallel().allMatch(y -> Arrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d)))) {
        throw new IllegalStateException("Non-finite value");
      }
      for (int i = 0; i < history.size() - 1; i++) {
        @Nonnull final DeltaSet<UUID> sd = history.get(i + 1).weights.subtract(history.get(i).weights);
        @Nonnull final DeltaSet<UUID> yd = history.get(i + 1).delta.subtract(history.get(i).delta);
        final double beta = p.dot(yd) / sd.dot(yd);
        yd.freeRef();
        {
          DeltaSet<UUID> scale = sd.scale(alphas[i] - beta);
          DeltaSet<UUID> add = p.add(scale);
          scale.freeRef();
          p.freeRef();
          p = add;
        }
        sd.freeRef();
        if (!p.stream().parallel().allMatch(y -> Arrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d)))) {
          throw new IllegalStateException("Non-finite value");
        }
      }
      boolean accept = measurement.delta.dot(p) < 0;
      if (accept) {
        monitor.log("Accepted: " + new Stats(direction, p));
        copy(p, direction);
      } else {
        monitor.log("Rejected: " + new Stats(direction, p));
      }
      return accept;
    } catch (Throwable e) {
      monitor.log(String.format("LBFGS Orientation Error: %s", e.getMessage()));
      return false;
    } finally {
      if (null != p) p.freeRef();
    }
  }

  private void copy(@Nonnull DeltaSet<UUID> from, @Nonnull DeltaSet<UUID> to) {
    for (@Nonnull final Map.Entry<UUID, Delta<UUID>> e : to.getMap().entrySet()) {
      @Nullable final double[] delta = from.getMap().get(e.getKey()).getDelta();
      Arrays.setAll(e.getValue().getDelta(), j -> delta[j]);
    }
  }

  @Override
  public SimpleLineSearchCursor orient(final Trainable subject, @Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor) {

//    if (getClass().desiredAssertionStatus()) {
//      double verify = subject.measureStyle(monitor).getMean();
//      double input = measurement.getMean();
//      boolean isDifferent = Math.abs(verify - input) > 1e-2;
//      if (isDifferent) throw new AssertionError(String.format("Invalid input point: %s != %s", verify, input));
//      monitor.log(String.format("Verified input point: %s == %s", verify, input));
//    }

    addToHistory(measurement, monitor);
    @Nonnull final List<PointSample> history = Arrays.asList(this.history.toArray(new PointSample[]{}));
    @Nullable final DeltaSet<UUID> result = lbfgs(measurement, monitor, history);
    SimpleLineSearchCursor returnValue;
    if (null == result) {
      @Nonnull DeltaSet<UUID> scale = measurement.delta.scale(-1);
      returnValue = cursor(subject, measurement, "GD", scale);
      scale.freeRef();
    } else {
      returnValue = cursor(subject, measurement, "LBFGS", result);
      result.freeRef();
    }
    while (this.history.size() > (null == result ? minHistory : maxHistory)) {
      @Nullable final PointSample remove = this.history.pollFirst();
      if (verbose) {
        monitor.log(String.format("Removed measurement %s to history. Total: %s", Long.toHexString(System.identityHashCode(remove)), history.size()));
      }
      remove.freeRef();
    }

//    if (getClass().desiredAssertionStatus()) {
//      double verify = returnValue.step(0, monitor).point.getMean();
//      double input = measurement.getMean();
//      boolean isDifferent = Math.abs(verify - input) > 1e-2;
//      if (isDifferent) throw new AssertionError(String.format("Invalid lfbgs cursor: %s != %s", verify, input));
//      monitor.log(String.format("Verified lfbgs cursor: %s == %s", verify, input));
//    }

    return returnValue;
  }

  @Override
  public synchronized void reset() {
    history.forEach(ReferenceCountingBase::freeRef);
    history.clear();
  }

  @Override
  protected void _free() {
    history.forEach(ReferenceCountingBase::freeRef);
    history.clear();
  }

  private class Stats {
    private final double mag;
    private final double magGrad;
    private final double dot;
    private final List<CharSequence> anglesPerLayer;

    public Stats(@Nonnull DeltaSet<UUID> gradient, @Nonnull DeltaSet<UUID> quasinewton) {
      mag = Math.sqrt(quasinewton.dot(quasinewton));
      magGrad = Math.sqrt(gradient.dot(gradient));
      dot = gradient.dot(quasinewton) / (mag * magGrad);
      anglesPerLayer = gradient.getMap().entrySet().stream()
          //.filter(e -> !(e.getKey() instanceof PlaceholderLayer)) // This would be too verbose
          .map((@Nonnull final Map.Entry<UUID, Delta<UUID>> e) -> {
            @Nullable final double[] lbfgsVector = gradient.getMap().get(e.getKey()).getDelta();
            for (int index = 0; index < lbfgsVector.length; index++) {
              lbfgsVector[index] = Double.isFinite(lbfgsVector[index]) ? lbfgsVector[index] : 0;
            }
            @Nullable final double[] gradientVector = gradient.getMap().get(e.getKey()).getDelta();
            for (int index = 0; index < gradientVector.length; index++) {
              gradientVector[index] = Double.isFinite(gradientVector[index]) ? gradientVector[index] : 0;
            }
            final double lbfgsMagnitude = ArrayUtil.magnitude(lbfgsVector);
            final double gradientMagnitude = ArrayUtil.magnitude(gradientVector);
            if (!Double.isFinite(gradientMagnitude)) throw new IllegalStateException();
            if (!Double.isFinite(lbfgsMagnitude)) throw new IllegalStateException();
            final CharSequence layerName = gradient.getMap().get(e.getKey()).key.toString();
            if (gradientMagnitude == 0.0) {
              return String.format("%s = %.3e", layerName, lbfgsMagnitude);
            } else {
              final double dotP = ArrayUtil.dot(lbfgsVector, gradientVector) / (lbfgsMagnitude * gradientMagnitude);
              return String.format("%s = %.3f/%.3e", layerName, dotP, lbfgsMagnitude / gradientMagnitude);
            }
          }).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return String.format("LBFGS Orientation magnitude: %.3e, gradient %.3e, dot %.3f; %s",
          getMag(), getMagGrad(), getDot(), getAnglesPerLayer());
    }

    public double getMag() {
      return mag;
    }

    public double getMagGrad() {
      return magGrad;
    }

    public double getDot() {
      return dot;
    }

    public List<CharSequence> getAnglesPerLayer() {
      return anglesPerLayer;
    }
  }
}
