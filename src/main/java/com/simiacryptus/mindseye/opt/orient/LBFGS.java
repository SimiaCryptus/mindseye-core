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

import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.DoubleBufferSet;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchPoint;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LBFGS extends OrientationStrategyBase<SimpleLineSearchCursor> {

  protected final boolean verbose = true;
  private final RefTreeSet<PointSample> history = new RefTreeSet<>(RefComparator.comparingDouble(x -> {
    double temp_47_0001 = -x.getMean();
    if (null != x)
      x.freeRef();
    return temp_47_0001;
  }));
  private int maxHistory = 30;
  private int minHistory = 3;

  public int getMaxHistory() {
    return maxHistory;
  }

  @Nonnull
  public LBFGS setMaxHistory(final int maxHistory) {
    this.maxHistory = maxHistory;
    return this.addRef();
  }

  public int getMinHistory() {
    return minHistory;
  }

  @Nonnull
  public LBFGS setMinHistory(final int minHistory) {
    this.minHistory = minHistory;
    return this.addRef();
  }

  public static @SuppressWarnings("unused") LBFGS[] addRefs(LBFGS[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LBFGS::addRef).toArray((x) -> new LBFGS[x]);
  }

  public static @SuppressWarnings("unused") LBFGS[][] addRefs(LBFGS[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LBFGS::addRefs).toArray((x) -> new LBFGS[x][]);
  }

  private static boolean isFinite(@Nonnull final DoubleBufferSet<?, ?> delta) {
    boolean temp_47_0011 = delta.stream().parallel().flatMapToDouble(y -> {
      RefDoubleStream temp_47_0002 = RefArrays.stream(y.getDelta());
      if (null != y)
        y.freeRef();
      return temp_47_0002;
    }).allMatch(d -> Double.isFinite(d));
    delta.freeRef();
    return temp_47_0011;
  }

  public void addToHistory(@Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor) {
    if (!LBFGS.isFinite(measurement.delta.addRef())) {
      if (verbose) {
        monitor.log("Corrupt evalInputDelta measurement");
      }
    } else if (!LBFGS.isFinite(measurement.weights.addRef())) {
      if (verbose) {
        monitor.log("Corrupt weights measurement");
      }
    } else {
      Optional<PointSample> temp_47_0015 = history.stream()
          .filter(RefUtil.wrapInterface((Predicate<? super PointSample>) x -> {
            boolean temp_47_0003 = x.getMean() <= measurement.getMean();
            if (null != x)
              x.freeRef();
            return temp_47_0003;
          }, measurement == null ? null : measurement.addRef())).findAny();
      boolean isFound = temp_47_0015.isPresent();
      if (null != temp_47_0015)
        RefUtil.freeRef(temp_47_0015);
      if (!isFound) {
        @Nonnull
        final PointSample copyFull = measurement.copyFull();
        if (verbose) {
          monitor.log(RefString.format("Adding measurement %s to history. Total: %s",
              Long.toHexString(com.simiacryptus.ref.wrappers.RefSystem.identityHashCode(copyFull)), history.size()));
        }
        history.add(copyFull == null ? null : copyFull);
      } else if (verbose) {
        monitor.log(RefString.format("Non-optimal measurement %s < %s. Total: %s", measurement.sum,
            history.stream().mapToDouble(x -> {
              double temp_47_0004 = x.sum;
              if (null != x)
                x.freeRef();
              return temp_47_0004;
            }).min().orElse(Double.POSITIVE_INFINITY), history.size()));
      }
    }
    measurement.freeRef();
  }

  @Override
  public SimpleLineSearchCursor orient(final Trainable subject, @Nonnull final PointSample measurement,
      @Nonnull final TrainingMonitor monitor) {

    //    if (getClass().desiredAssertionStatus()) {
    //      double verify = subject.measureStyle(monitor).getMean();
    //      double input = measurement.getMean();
    //      boolean isDifferent = Math.abs(verify - input) > 1e-2;
    //      if (isDifferent) throw new AssertionError(String.format("Invalid input point: %s != %s", verify, input));
    //      monitor.log(String.format("Verified input point: %s == %s", verify, input));
    //    }

    addToHistory(measurement == null ? null : measurement.addRef(), monitor);
    @Nonnull
    final RefList<PointSample> history = RefArrays.asList(this.history.toArray(new PointSample[] {}));
    @Nullable
    final DeltaSet<UUID> result = lbfgs(measurement == null ? null : measurement.addRef(), monitor,
        history == null ? null : history.addRef());
    SimpleLineSearchCursor returnValue;
    if (null == result) {
      @Nonnull
      DeltaSet<UUID> scale = measurement.delta.scale(-1);
      returnValue = cursor(subject == null ? null : subject.addRef(), measurement == null ? null : measurement.addRef(),
          "GD", scale == null ? null : scale);
    } else {
      returnValue = cursor(subject == null ? null : subject.addRef(), measurement == null ? null : measurement.addRef(),
          "LBFGS", result == null ? null : result.addRef());
    }
    measurement.freeRef();
    if (null != subject)
      subject.freeRef();
    while (this.history.size() > (null == result ? minHistory : maxHistory)) {
      @Nullable
      final PointSample remove = this.history.pollFirst();
      if (verbose) {
        monitor.log(RefString.format("Removed measurement %s to history. Total: %s",
            Long.toHexString(com.simiacryptus.ref.wrappers.RefSystem.identityHashCode(remove)), history.size()));
      }
      if (null != remove)
        remove.freeRef();
    }

    //    if (getClass().desiredAssertionStatus()) {
    //      double verify = returnValue.step(0, monitor).point.getMean();
    //      double input = measurement.getMean();
    //      boolean isDifferent = Math.abs(verify - input) > 1e-2;
    //      if (isDifferent) throw new AssertionError(String.format("Invalid lfbgs cursor: %s != %s", verify, input));
    //      monitor.log(String.format("Verified lfbgs cursor: %s == %s", verify, input));
    //    }

    if (null != result)
      result.freeRef();
    history.freeRef();
    return returnValue;
  }

  @Override
  public synchronized void reset() {
    history.clear();
  }

  public void _free() {
    if (null != history)
      history.freeRef();
    history.clear();
  }

  public @Override @SuppressWarnings("unused") LBFGS addRef() {
    return (LBFGS) super.addRef();
  }

  @Nullable
  protected DeltaSet<UUID> lbfgs(@Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor,
      @Nonnull final RefList<PointSample> history) {
    @Nonnull
    final DeltaSet<UUID> result = measurement.delta.scale(-1);
    if (history.size() > minHistory) {
      if (lbfgs(measurement == null ? null : measurement.addRef(), monitor, history == null ? null : history.addRef(),
          result == null ? null : result.addRef())) {
        setHistory(monitor, history == null ? null : history);
        measurement.freeRef();
        return result;
      } else {
        monitor.log("Orientation rejected. Popping history element from " + RefUtil.get(history.stream().map(x -> {
          String temp_47_0005 = RefString.format("%s", x.getMean());
          if (null != x)
            x.freeRef();
          return temp_47_0005;
        }).reduce((a, b) -> a + ", " + b)));
        result.freeRef();
        DeltaSet<UUID> temp_47_0012 = lbfgs(measurement == null ? null : measurement, monitor,
            history.subList(0, history.size() - 1));
        history.freeRef();
        return temp_47_0012;
      }
    } else {
      monitor.log(RefString.format("LBFGS Accumulation History: %s points", history.size()));
      result.freeRef();
      measurement.freeRef();
      history.freeRef();
      return null;
    }
  }

  @Nonnull
  private SimpleLineSearchCursor cursor(final Trainable subject, @Nonnull final PointSample measurement,
      final String type, final DeltaSet<UUID> result) {
    SimpleLineSearchCursor temp_47_0014 = new SimpleLineSearchCursor(subject, measurement, result) {

      @Override
      public LineSearchPoint step(final double t, @Nonnull final TrainingMonitor monitor) {
        final LineSearchPoint measure = super.step(t, monitor).addRef();
        addToHistory(measure.point.addRef(), monitor);
        return measure;
      }

      public @SuppressWarnings("unused") void _free() {
      }
    };
    SimpleLineSearchCursor temp_47_0013 = temp_47_0014.setDirectionType(type);
    if (null != temp_47_0014)
      temp_47_0014.freeRef();
    return temp_47_0013;
  }

  private void setHistory(@Nonnull final TrainingMonitor monitor, @Nonnull final RefList<PointSample> history) {
    if (history.size() == this.history.size() && history.stream().filter(x -> {
      boolean temp_47_0006 = !this.history.contains(x == null ? null : x.addRef());
      if (null != x)
        x.freeRef();
      return temp_47_0006;
    }).count() == 0) {
      history.freeRef();
      return;
    }
    if (verbose) {
      monitor.log(RefString.format("Overwriting history with %s points", history.size()));
    }
    synchronized (this.history) {
      this.history.clear();
      this.history.addAll(history == null ? null : history.addRef());
    }
    history.freeRef();
  }

  private boolean lbfgs(@Nonnull PointSample measurement, @Nonnull TrainingMonitor monitor,
      @Nonnull RefList<PointSample> history, @Nonnull DeltaSet<UUID> direction) {
    @Nonnull
    DeltaSet<UUID> p = null;
    try {
      p = measurement.delta.copy();
      if (!p.stream().parallel().allMatch(y -> {
        boolean temp_47_0007 = RefArrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d));
        if (null != y)
          y.freeRef();
        return temp_47_0007;
      })) {
        p.freeRef();
        measurement.freeRef();
        history.freeRef();
        direction.freeRef();
        throw new IllegalStateException("Non-finite value");
      }
      @Nonnull
      final double[] alphas = new double[history.size()];
      for (int i = history.size() - 2; i >= 0; i--) {
        PointSample temp_47_0016 = history.get(i + 1);
        PointSample temp_47_0017 = history.get(i);
        @Nonnull
        final DeltaSet<UUID> sd = temp_47_0016.weights.subtract(temp_47_0017.weights.addRef());
        if (null != temp_47_0017)
          temp_47_0017.freeRef();
        if (null != temp_47_0016)
          temp_47_0016.freeRef();
        PointSample temp_47_0018 = history.get(i + 1);
        PointSample temp_47_0019 = history.get(i);
        @Nonnull
        final DeltaSet<UUID> yd = temp_47_0018.delta.subtract(temp_47_0019.delta.addRef());
        if (null != temp_47_0019)
          temp_47_0019.freeRef();
        if (null != temp_47_0018)
          temp_47_0018.freeRef();
        final double denominator = sd.dot(yd == null ? null : yd.addRef());
        if (0 == denominator) {
          p.freeRef();
          sd.freeRef();
          yd.freeRef();
          measurement.freeRef();
          history.freeRef();
          direction.freeRef();
          throw new IllegalStateException("Orientation vanished.");
        }
        alphas[i] = p.dot(sd == null ? null : sd) / denominator;
        DeltaSet<UUID> scale = yd.scale(alphas[i]);
        yd.freeRef();
        p = p.subtract(scale == null ? null : scale.addRef());
        if (null != scale)
          scale.freeRef();
        if ((!p.stream().parallel().allMatch(y -> {
          boolean temp_47_0008 = RefArrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d));
          if (null != y)
            y.freeRef();
          return temp_47_0008;
        }))) {
          p.freeRef();
          measurement.freeRef();
          history.freeRef();
          direction.freeRef();
          throw new IllegalStateException("Non-finite value");
        }
      }
      PointSample temp_47_0020 = history.get(history.size() - 1);
      PointSample temp_47_0021 = history.get(history.size() - 2);
      @Nonnull
      final DeltaSet<UUID> sk = temp_47_0020.weights.subtract(temp_47_0021.weights.addRef());
      if (null != temp_47_0021)
        temp_47_0021.freeRef();
      if (null != temp_47_0020)
        temp_47_0020.freeRef();
      PointSample temp_47_0022 = history.get(history.size() - 1);
      PointSample temp_47_0023 = history.get(history.size() - 2);
      @Nonnull
      final DeltaSet<UUID> yk = temp_47_0022.delta.subtract(temp_47_0023.delta.addRef());
      if (null != temp_47_0023)
        temp_47_0023.freeRef();
      if (null != temp_47_0022)
        temp_47_0022.freeRef();
      double dot = sk.dot(yk == null ? null : yk.addRef());
      double f = dot / yk.dot(yk == null ? null : yk.addRef());
      p = p.scale(f);
      yk.freeRef();
      sk.freeRef();
      if (!p.stream().parallel().allMatch(y -> {
        boolean temp_47_0009 = RefArrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d));
        if (null != y)
          y.freeRef();
        return temp_47_0009;
      })) {
        p.freeRef();
        measurement.freeRef();
        history.freeRef();
        direction.freeRef();
        throw new IllegalStateException("Non-finite value");
      }
      for (int i = 0; i < history.size() - 1; i++) {
        PointSample temp_47_0024 = history.get(i + 1);
        PointSample temp_47_0025 = history.get(i);
        @Nonnull
        final DeltaSet<UUID> sd = temp_47_0024.weights.subtract(temp_47_0025.weights.addRef());
        if (null != temp_47_0025)
          temp_47_0025.freeRef();
        if (null != temp_47_0024)
          temp_47_0024.freeRef();
        PointSample temp_47_0026 = history.get(i + 1);
        PointSample temp_47_0027 = history.get(i);
        @Nonnull
        final DeltaSet<UUID> yd = temp_47_0026.delta.subtract(temp_47_0027.delta.addRef());
        if (null != temp_47_0027)
          temp_47_0027.freeRef();
        if (null != temp_47_0026)
          temp_47_0026.freeRef();
        final double beta = p.dot(yd == null ? null : yd) / sd.dot(yd == null ? null : yd.addRef());
        DeltaSet<UUID> scale = sd.scale(alphas[i] - beta);
        p = p.add(scale == null ? null : scale.addRef());
        if (null != scale)
          scale.freeRef();
        sd.freeRef();
        if (!p.stream().parallel().allMatch(y -> {
          boolean temp_47_0010 = RefArrays.stream(y.getDelta()).allMatch(d -> Double.isFinite(d));
          if (null != y)
            y.freeRef();
          return temp_47_0010;
        })) {
          p.freeRef();
          measurement.freeRef();
          history.freeRef();
          direction.freeRef();
          throw new IllegalStateException("Non-finite value");
        }
      }
      boolean accept = measurement.delta.dot(p == null ? null : p.addRef()) < 0;
      if (accept) {
        monitor.log(
            "Accepted: " + new Stats(direction == null ? null : direction.addRef(), p == null ? null : p.addRef()));
        copy(p == null ? null : p.addRef(), direction == null ? null : direction.addRef());
      } else {
        monitor.log(
            "Rejected: " + new Stats(direction == null ? null : direction.addRef(), p == null ? null : p.addRef()));
      }
      p.freeRef();
      measurement.freeRef();
      history.freeRef();
      direction.freeRef();
      return accept;
    } catch (Throwable e) {
      monitor.log(RefString.format("LBFGS Orientation Error: %s", e.getMessage()));
      return false;
    } finally {
    }
  }

  private void copy(@Nonnull DeltaSet<UUID> from, @Nonnull DeltaSet<UUID> to) {
    RefMap<UUID, Delta<UUID>> uuidDeltaRefMap = to.getMap();
    RefSet<Map.Entry<UUID, Delta<UUID>>> entries = uuidDeltaRefMap.entrySet();
    if (null != uuidDeltaRefMap)
      uuidDeltaRefMap.freeRef();
    for (final Map.Entry<UUID, Delta<UUID>> e : entries) {
      RefMap<UUID, Delta<UUID>> temp_47_0029 = from.getMap();
      Delta<UUID> temp_47_0030 = temp_47_0029.get(e.getKey());
      @Nullable
      final double[] delta = temp_47_0030.getDelta();
      if (null != temp_47_0030)
        temp_47_0030.freeRef();
      if (null != temp_47_0029)
        temp_47_0029.freeRef();
      Delta<UUID> temp_47_0031 = e.getValue();
      RefArrays.setAll(temp_47_0031.getDelta(), j -> delta[j]);
      if (null != temp_47_0031)
        temp_47_0031.freeRef();
    }
    entries.freeRef();
    to.freeRef();
    from.freeRef();
  }

  private class Stats {
    private final double mag;
    private final double magGrad;
    private final double dot;
    private final List<CharSequence> anglesPerLayer;

    public Stats(@Nonnull DeltaSet<UUID> gradient, @Nonnull DeltaSet<UUID> quasinewton) {
      mag = Math.sqrt(quasinewton.dot(quasinewton == null ? null : quasinewton.addRef()));
      magGrad = Math.sqrt(gradient.dot(gradient == null ? null : gradient.addRef()));
      dot = gradient.dot(quasinewton == null ? null : quasinewton) / (mag * magGrad);
      RefMap<UUID, Delta<UUID>> temp_47_0032 = gradient.getMap();
      RefSet<Map.Entry<UUID, Delta<UUID>>> temp_47_0033 = temp_47_0032.entrySet();
      anglesPerLayer = temp_47_0033.stream()
          //.filter(e -> !(e.getKey() instanceof PlaceholderLayer)) // This would be too verbose
          .map(RefUtil.wrapInterface((Function<Map.Entry<UUID, Delta<UUID>>, ? extends String>) (
              @Nonnull final Map.Entry<UUID, Delta<UUID>> e) -> {
            RefMap<UUID, Delta<UUID>> temp_47_0034 = gradient.getMap();
            Delta<UUID> temp_47_0035 = temp_47_0034.get(e.getKey());
            @Nullable
            final double[] lbfgsVector = temp_47_0035.getDelta();
            if (null != temp_47_0035)
              temp_47_0035.freeRef();
            if (null != temp_47_0034)
              temp_47_0034.freeRef();
            for (int index = 0; index < lbfgsVector.length; index++) {
              lbfgsVector[index] = Double.isFinite(lbfgsVector[index]) ? lbfgsVector[index] : 0;
            }
            RefMap<UUID, Delta<UUID>> temp_47_0036 = gradient.getMap();
            Delta<UUID> temp_47_0037 = temp_47_0036.get(e.getKey());
            @Nullable
            final double[] gradientVector = temp_47_0037.getDelta();
            if (null != temp_47_0037)
              temp_47_0037.freeRef();
            if (null != temp_47_0036)
              temp_47_0036.freeRef();
            for (int index = 0; index < gradientVector.length; index++) {
              gradientVector[index] = Double.isFinite(gradientVector[index]) ? gradientVector[index] : 0;
            }
            final double lbfgsMagnitude = ArrayUtil.magnitude(lbfgsVector);
            final double gradientMagnitude = ArrayUtil.magnitude(gradientVector);
            if (!Double.isFinite(gradientMagnitude)) {
              RefUtil.freeRef(e);
              throw new IllegalStateException();
            }
            if (!Double.isFinite(lbfgsMagnitude)) {
              RefUtil.freeRef(e);
              throw new IllegalStateException();
            }
            RefMap<UUID, Delta<UUID>> temp_47_0038 = gradient.getMap();
            Delta<UUID> temp_47_0039 = temp_47_0038.get(e.getKey());
            final CharSequence layerName = temp_47_0039.key.toString();
            if (null != temp_47_0039)
              temp_47_0039.freeRef();
            if (null != temp_47_0038)
              temp_47_0038.freeRef();
            RefUtil.freeRef(e);
            if (gradientMagnitude == 0.0) {
              return RefString.format("%s = %.3e", layerName, lbfgsMagnitude);
            } else {
              final double dotP = ArrayUtil.dot(lbfgsVector, gradientVector) / (lbfgsMagnitude * gradientMagnitude);
              return RefString.format("%s = %.3f/%.3e", layerName, dotP, lbfgsMagnitude / gradientMagnitude);
            }
          }, gradient == null ? null : gradient)).collect(Collectors.toList());
      if (null != temp_47_0033)
        temp_47_0033.freeRef();
      if (null != temp_47_0032)
        temp_47_0032.freeRef();
    }

    public List<CharSequence> getAnglesPerLayer() {
      return anglesPerLayer;
    }

    public double getDot() {
      return dot;
    }

    public double getMag() {
      return mag;
    }

    public double getMagGrad() {
      return magGrad;
    }

    @Override
    public String toString() {
      return RefString.format("LBFGS Orientation magnitude: %.3e, gradient %.3e, dot %.3f; %s", getMag(), getMagGrad(),
          getDot(), getAnglesPerLayer());
    }
  }
}
