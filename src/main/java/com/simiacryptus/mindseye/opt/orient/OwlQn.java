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
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchPoint;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OwlQn extends OrientationStrategyBase<LineSearchCursor> {
  public final OrientationStrategy<?> inner;
  private double factor_L1 = 0.000;
  private double zeroTol = 1e-20;

  public OwlQn() {
    this(new LBFGS());
  }

  protected OwlQn(final OrientationStrategy<?> inner) {
    this.inner = inner;
  }

  public double getFactor_L1() {
    return factor_L1;
  }

  @Nonnull
  public OwlQn setFactor_L1(final double factor_L1) {
    this.factor_L1 = factor_L1;
    return this;
  }

  public Collection<Layer> getLayers(@Nonnull final Collection<Layer> layers) {
    return layers.stream()
        //        .filter(layer -> layer instanceof FullyConnectedLayer)
        .collect(Collectors.toList());
  }

  public double getZeroTol() {
    return zeroTol;
  }

  @Nonnull
  public OwlQn setZeroTol(final double zeroTol) {
    this.zeroTol = zeroTol;
    return this;
  }

  @Nonnull
  @Override
  public LineSearchCursor orient(final Trainable subject, @Nonnull final PointSample measurement,
      final TrainingMonitor monitor) {
    @Nonnull
    final SimpleLineSearchCursor gradient = (SimpleLineSearchCursor) inner.orient(subject, measurement, monitor);
    @Nonnull
    final DeltaSet<UUID> searchDirection = gradient.direction.copy();
    @Nonnull
    final DeltaSet<UUID> orthant = new DeltaSet<UUID>();
    Set<UUID> keySet = gradient.direction.getMap().keySet();
    List<Layer> layerSet = keySet.stream().map(id -> ((DAGNetwork) subject.getLayer()).getLayersById().get(id))
        .collect(Collectors.toList());
    for (@Nonnull
    final Layer layer : getLayers(layerSet)) {
      gradient.direction.getMap().forEach((layerId, layerDelta) -> {
        final double[] weights = layerDelta.target;
        @Nullable
        final double[] delta = layerDelta.getDelta();
        Delta<UUID> layerDelta1 = searchDirection.get(layerId, weights);
        @Nullable
        final double[] searchDir = layerDelta1.getDelta();
        Delta<UUID> layerDelta2 = orthant.get(layerId, weights);
        @Nullable
        final double[] suborthant = layerDelta2.getDelta();
        for (int i = 0; i < searchDir.length; i++) {
          final int positionSign = sign(weights[i]);
          final int directionSign = sign(delta[i]);
          suborthant[i] = 0 == positionSign ? directionSign : positionSign;
          searchDir[i] += factor_L1 * (weights[i] < 0 ? -1.0 : 1.0);
          if (sign(searchDir[i]) != directionSign) {
            searchDir[i] = delta[i];
          }
        }
        assert null != searchDir;
      });
    }
    return new SimpleLineSearchCursor(subject, measurement, searchDirection) {
      @Nonnull
      @Override
      public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
        origin.weights.stream().forEach(d -> d.restore());
        @Nonnull
        final DeltaSet<UUID> currentDirection = direction.copy();
        direction.getMap().forEach((layer, buffer) -> {
          if (null == buffer.getDelta())
            return;
          Delta<UUID> layerDelta = currentDirection.get(layer, buffer.target);
          @Nullable
          final double[] currentDelta = layerDelta.getDelta();
          for (int i = 0; i < buffer.getDelta().length; i++) {
            final double prevValue = buffer.target[i];
            final double newValue = prevValue + buffer.getDelta()[i] * alpha;
            if (sign(prevValue) != 0 && sign(prevValue) != sign(newValue)) {
              currentDelta[i] = 0;
              buffer.target[i] = 0;
            } else {
              buffer.target[i] = newValue;
            }
          }
        });
        @Nonnull
        final PointSample measure = afterStep(subject.measure(monitor).setRate(alpha));
        double dot = currentDirection.dot(measure.delta);
        return new LineSearchPoint(measure, dot);
      }
    }.setDirectionType("OWL/QN");
  }

  @Override
  public void reset() {
    inner.reset();
  }

  protected int sign(final double weight) {
    if (weight > zeroTol) {
      return 1;
    } else if (weight < -zeroTol) {
    } else {
      return -1;
    }
    return 0;
  }

  @Override
  protected void _free() {
  }
}
