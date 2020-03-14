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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LBFGS extends OrientationStrategyBase<SimpleLineSearchCursor> {

  protected final boolean verbose = true;
  private final RefTreeSet<PointSample> history = new RefTreeSet<>(RefComparator.reversed(RefComparator.comparingDouble(PointSample::getMean)));
  private int maxHistory = 30;
  private int minHistory = 3;

  public int getMaxHistory() {
    return maxHistory;
  }

  public void setMaxHistory(int maxHistory) {
    this.maxHistory = maxHistory;
  }

  public int getMinHistory() {
    return minHistory;
  }

  public void setMinHistory(int minHistory) {
    this.minHistory = minHistory;
  }


  private static boolean isFinite(@Nonnull final DoubleBufferSet<?, ?> delta) {
    boolean temp_47_0011 = delta.stream().parallel().allMatch(y -> {
      boolean temp_47_0002 = RefArrays.stream(y.getDelta()).allMatch(Double::isFinite);
      y.freeRef();
      return temp_47_0002;
    });
    delta.freeRef();
    return temp_47_0011;
  }

  public void addToHistory(@Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor) {
    assert assertAlive();
    assert measurement.assertAlive();
    if (null == measurement) return;
    try {
      if (!LBFGS.isFinite(measurement.delta.addRef())) {
        if (verbose) {
          monitor.log("Corrupt evalInputDelta measurement");
        }
      } else if (!LBFGS.isFinite(measurement.weights.addRef())) {
        if (verbose) {
          monitor.log("Corrupt weights measurement");
        }
      } else {
        double minFitness = history.stream().mapToDouble(pointSample -> {
          double mean = pointSample.getMean();
          pointSample.freeRef();
          return mean;
        }).min().orElse(Double.POSITIVE_INFINITY);
        if (measurement.getMean() < minFitness) {
          @Nonnull final PointSample copyFull = measurement.copyFull();
          if (verbose) {
            monitor.log(RefString.format("Adding measurement %s to history. Total: %s",
                Long.toHexString(RefSystem.identityHashCode(copyFull.addRef())), history.size()));
          }
          history.add(copyFull);
        } else if (verbose) {
          monitor.log(RefString.format("Non-optimal measurement %s < %s. Total: %s", measurement.getMean(),
              minFitness, history.size()));
        }
      }
    } finally {
      measurement.freeRef();
    }
  }

  @Override
  public SimpleLineSearchCursor orient(@Nullable final Trainable subject, @Nonnull final PointSample measurement,
                                       @Nonnull final TrainingMonitor monitor) {

    //    if (getClass().desiredAssertionStatus()) {
    //      double verify = subject.measureStyle(monitor).getMean();
    //      double input = measurement.getMean();
    //      boolean isDifferent = Math.abs(verify - input) > 1e-2;
    //      if (isDifferent) throw new AssertionError(String.format("Invalid input point: %s != %s", verify, input));
    //      monitor.log(String.format("Verified input point: %s == %s", verify, input));
    //    }

    addToHistory(measurement.addRef(), monitor);
    @Nullable final DeltaSet<UUID> result = lbfgs(measurement.addRef(), monitor, new RefArrayList<>(history.addRef()));
    int historySize = null == result ? minHistory : maxHistory;
    try {
      SimpleLineSearchCursor cursor;
      if (null != result) {
        cursor = cursor(subject, measurement, "LBFGS", result);
      } else {
        cursor = cursor(subject, measurement, "GD", measurement.delta.scale(-1));
      }
      return cursor;
    } finally {
      truncateHistory(monitor, historySize);
    }

    //    if (getClass().desiredAssertionStatus()) {
    //      double verify = returnValue.step(0, monitor).point.getMean();
    //      double input = measurement.getMean();
    //      boolean isDifferent = Math.abs(verify - input) > 1e-2;
    //      if (isDifferent) throw new AssertionError(String.format("Invalid lfbgs cursor: %s != %s", verify, input));
    //      monitor.log(String.format("Verified lfbgs cursor: %s == %s", verify, input));
    //    }

  }

  @Override
  public synchronized void reset() {
    history.clear();
  }

  public void _free() {
    super._free();
    history.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LBFGS addRef() {
    return (LBFGS) super.addRef();
  }

  @Nullable
  protected DeltaSet<UUID> lbfgs(@Nonnull final PointSample measurement, @Nonnull final TrainingMonitor monitor,
                                 @Nonnull final RefList<PointSample> historyList) {
    @Nonnull final DeltaSet<UUID> result = measurement.delta.scale(-1);
    if (historyList.size() > minHistory) {
      if (lbfgs(measurement.addRef(), monitor, historyList.addRef(), result.addRef())) {
        setHistory(monitor, historyList);
        measurement.freeRef();
        return result;
      } else {
        result.freeRef();
        monitor.log("Orientation rejected. Popping history element from " + RefUtil.get(historyList.stream().map(x -> {
          double mean = x.getMean();
          x.freeRef();
          return String.format("%s", mean);
        }).reduce((a, b) -> a + ", " + b)));
        RefList<PointSample> subList = historyList.subList(0, historyList.size() - 1);
        historyList.freeRef();
        return lbfgs(measurement, monitor, subList);
      }
    } else {
      result.freeRef();
      monitor.log(RefString.format("LBFGS Accumulation History: %s points", historyList.size()));
      measurement.freeRef();
      historyList.freeRef();
      return null;
    }
  }

  private void truncateHistory(@Nonnull TrainingMonitor monitor, int historySize) {
    while (this.history.size() > historySize) {
      @Nullable final PointSample remove = this.history.pollFirst();
      if (verbose) {
        monitor.log(RefString.format("Removed measurement %s to history. Total: %s",
            Long.toHexString(RefSystem.identityHashCode(remove)), this.history.size()));
      } else {
        if (null != remove)
          remove.freeRef();
      }
    }
  }

  @Nonnull
  private SimpleLineSearchCursor cursor(final Trainable subject, @Nonnull final PointSample measurement,
                                        final String type, final DeltaSet<UUID> result) {
    SimpleLineSearchCursor simpleLineSearchCursor = new SimpleLineSearchCursor(subject, measurement, result) {

      @Nonnull
      @Override
      public LineSearchPoint step(final double t, @Nonnull final TrainingMonitor monitor) {
        final LineSearchPoint measure = super.step(t, monitor);
        addToHistory(measure.getPoint(), monitor);
        return measure;
      }

      public @SuppressWarnings("unused")
      void _free() {
        super._free();
      }
    };
    simpleLineSearchCursor.setDirectionType(type);
    return simpleLineSearchCursor;
  }

  private void setHistory(@Nonnull final TrainingMonitor monitor, @Nonnull final RefList<PointSample> history) {
    if (history.size() == this.history.size())
      if (history.stream().filter(x -> !this.history.contains(x)).count() == 0) {
        history.freeRef();
        return;
      }
    if (verbose) {
      monitor.log(RefString.format("Overwriting history with %s points", history.size()));
    }
    synchronized (this.history) {
      this.history.clear();
      this.history.addAll(history);
    }
  }

  private boolean lbfgs(@Nonnull PointSample measurement, @Nonnull TrainingMonitor monitor,
                        @Nonnull RefList<PointSample> history, @Nonnull DeltaSet<UUID> direction) {
    DeltaSet<UUID> copy = measurement.delta.copy();
    @Nonnull
    DeltaSet<UUID> p = copy.allFinite(0.0);
    copy.freeRef();
    try {
      @Nonnull final double[] alphas = new double[history.size()];
      for (int i = history.size() - 2; i >= 0; i--) {
        @Nonnull final DeltaSet<UUID> sd = subtractWeights(history.get(i + 1), history.get(i));
        @Nonnull final DeltaSet<UUID> yd = subtractDelta(history.get(i + 1), history.get(i));
        final double denominator = sd.dot(yd.addRef());
        if (0 == denominator) {
          sd.freeRef();
          yd.freeRef();
          throw new IllegalStateException("Orientation vanished.");
        }
        alphas[i] = p.dot(sd) / denominator;
        DeltaSet<UUID> scale = yd.scale(alphas[i]);
        yd.freeRef();
        DeltaSet<UUID> subtract = p.subtract(scale);
        if (p != null) p.freeRef();
        p = subtract.allFinite(0);
        subtract.freeRef();
      }
      @Nonnull final DeltaSet<UUID> sk = subtractWeights(history.get(history.size() - 1), history.get(history.size() - 2));
      @Nonnull final DeltaSet<UUID> yk = subtractDelta(history.get(history.size() - 1), history.get(history.size() - 2));
      double dot = sk.dot(yk.addRef());
      sk.freeRef();
      double f = dot / yk.dot(yk.addRef());
      yk.freeRef();
      DeltaSet<UUID> scale1 = p.scale(f);
      if (p != null) p.freeRef();
      p = scale1.allFinite(0);
      scale1.freeRef();
      for (int i = 0; i < history.size() - 1; i++) {
        @Nonnull final DeltaSet<UUID> sd = subtractWeights(history.get(i + 1), history.get(i));
        @Nonnull final DeltaSet<UUID> yd = subtractDelta(history.get(i + 1), history.get(i));
        double dot1 = sd.dot(yd.addRef());
        double dot2 = p.dot(yd);
        final double beta = dot2 / dot1;
        DeltaSet<UUID> add = p.add(sd.scale(alphas[i] - beta));
        sd.freeRef();
        if (p != null) p.freeRef();
        p = add.allFinite(0);
        add.freeRef();
      }
      boolean accept = measurement.delta.dot(p.addRef()) < 0;
      if (accept) {
        monitor.log(
            "Accepted: " + new Stats(direction.addRef(), p.addRef()));
        copy(p.addRef(), direction.addRef());
      } else {
        monitor.log(
            "Rejected: " + new Stats(direction.addRef(), p.addRef()));
      }
      return accept;
    } catch (Throwable e) {
      monitor.log(RefString.format("LBFGS Orientation Error: %s", e.getMessage()));
      return false;
    } finally {
      p.freeRef();
      measurement.freeRef();
      history.freeRef();
      direction.freeRef();
    }
  }

  @NotNull
  private DeltaSet<UUID> subtractDelta(PointSample a, PointSample b) {
    try {
      return a.delta.subtract(b.delta.addRef());
    } finally {
      b.freeRef();
      a.freeRef();
    }
  }

  @NotNull
  private DeltaSet<UUID> subtractWeights(PointSample a, PointSample b) {
    try {
      return a.weights.subtract(b.weights.addRef());
    } finally {
      b.freeRef();
      a.freeRef();
    }
  }

  private void copy(@Nonnull DeltaSet<UUID> from, @Nonnull DeltaSet<UUID> to) {
    RefMap<UUID, Delta<UUID>> uuidDeltaRefMap = to.getMap();
    RefSet<Map.Entry<UUID, Delta<UUID>>> entries = uuidDeltaRefMap.entrySet();
    uuidDeltaRefMap.freeRef();
    entries.forEach(e -> {
      RefMap<UUID, Delta<UUID>> temp_47_0029 = from.getMap();
      Delta<UUID> temp_47_0030 = temp_47_0029.get(e.getKey());
      assert temp_47_0030 != null;
      @Nullable final double[] delta = temp_47_0030.getDelta();
      temp_47_0030.freeRef();
      temp_47_0029.freeRef();
      Delta<UUID> temp_47_0031 = e.getValue();
      RefUtil.freeRef(e);
      RefArrays.setAll(temp_47_0031.getDelta(), j -> {
        assert delta != null;
        return delta[j];
      });
      temp_47_0031.freeRef();
    });
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
      mag = Math.sqrt(quasinewton.dot(quasinewton.addRef()));
      magGrad = Math.sqrt(gradient.dot(gradient.addRef()));
      dot = gradient.dot(quasinewton) / (mag * magGrad);
      RefMap<UUID, Delta<UUID>> temp_47_0032 = gradient.getMap();
      RefSet<Map.Entry<UUID, Delta<UUID>>> temp_47_0033 = temp_47_0032.entrySet();
      try {
        anglesPerLayer = temp_47_0033.stream()
            //.filter(e -> !(e.getKey() instanceof PlaceholderLayer)) // This would be too verbose
            .map(RefUtil.wrapInterface((Function<Map.Entry<UUID, Delta<UUID>>, ? extends String>) (
                final Map.Entry<UUID, Delta<UUID>> e) -> {
              RefMap<UUID, Delta<UUID>> temp_47_0034 = gradient.getMap();
              Delta<UUID> temp_47_0035 = temp_47_0034.get(e.getKey());
              assert temp_47_0035 != null;
              @Nullable final double[] lbfgsVector = temp_47_0035.getDelta();
              temp_47_0035.freeRef();
              temp_47_0034.freeRef();
              assert lbfgsVector != null;
              for (int index = 0; index < lbfgsVector.length; index++) {
                lbfgsVector[index] = Double.isFinite(lbfgsVector[index]) ? lbfgsVector[index] : 0;
              }
              RefMap<UUID, Delta<UUID>> temp_47_0036 = gradient.getMap();
              Delta<UUID> temp_47_0037 = temp_47_0036.get(e.getKey());
              assert temp_47_0037 != null;
              @Nullable final double[] gradientVector = temp_47_0037.getDelta();
              temp_47_0037.freeRef();
              temp_47_0036.freeRef();
              assert gradientVector != null;
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
              assert temp_47_0039 != null;
              final CharSequence layerName = temp_47_0039.key.toString();
              temp_47_0039.freeRef();
              temp_47_0038.freeRef();
              RefUtil.freeRef(e);
              if (gradientMagnitude == 0.0) {
                return RefString.format("%s = %.3e", layerName, lbfgsMagnitude);
              } else {
                final double dotP = ArrayUtil.dot(lbfgsVector, gradientVector) / (lbfgsMagnitude * gradientMagnitude);
                return RefString.format("%s = %.3f/%.3e", layerName, dotP, lbfgsMagnitude / gradientMagnitude);
              }
            }, gradient)).collect(Collectors.toList());
      } finally {
        temp_47_0033.freeRef();
        temp_47_0032.freeRef();
      }
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

    @Nonnull
    @Override
    public String toString() {
      return RefString.format("LBFGS Orientation magnitude: %.3e, gradient %.3e, dot %.3f; %s",
          getMag(), getMagGrad(), getDot(), getAnglesPerLayer());
    }
  }
}
