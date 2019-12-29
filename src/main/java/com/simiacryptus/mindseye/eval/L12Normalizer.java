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

package com.simiacryptus.mindseye.eval;

import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.TrainingMonitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class L12Normalizer extends TrainableBase {
  public final Trainable inner;
  private final boolean hideAdj = false;

  public L12Normalizer(final Trainable inner) {
    this.inner = inner;
  }

  @Override
  protected void _free() {
  }

  protected abstract double getL1(Layer layer);

  protected abstract double getL2(Layer layer);

  public Layer toLayer(UUID id) {
    return ((DAGNetwork) inner.getLayer()).getLayersById().get(id);
  }

  public Collection<Layer> getLayers(@Nonnull final Collection<UUID> layers) {
    return layers.stream().map(this::toLayer)
        //.filter(layer -> layer instanceof FullyConnectedLayer)
        .collect(Collectors.toList());
  }

  @Nonnull
  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    final PointSample innerMeasure = inner.measure(monitor);
    @Nonnull
    final DeltaSet<UUID> normalizationVector = new DeltaSet<UUID>();
    double valueAdj = 0;
    for (@Nonnull
    final Layer layer : getLayers(innerMeasure.delta.getMap().keySet())) {
      final double[] weights = innerMeasure.delta.getMap().get(layer.getId()).target;
      @Nullable
      final double[] gradientAdj = normalizationVector.get(layer.getId(), weights).getDelta();
      final double factor_L1 = getL1(layer);
      final double factor_L2 = getL2(layer);
      assert null != gradientAdj;
      for (int i = 0; i < gradientAdj.length; i++) {
        final double sign = weights[i] < 0 ? -1.0 : 1.0;
        gradientAdj[i] += factor_L1 * sign + 2 * factor_L2 * weights[i];
        valueAdj += (factor_L1 * sign + factor_L2 * weights[i]) * weights[i];
      }
    }
    final DeltaSet<UUID> deltaSet = innerMeasure.delta.add(normalizationVector);
    final PointSample pointSample = new PointSample(deltaSet, innerMeasure.weights,
        innerMeasure.sum + (hideAdj ? 0 : valueAdj), innerMeasure.rate, innerMeasure.count);
    return pointSample.normalize();
  }

  @Override
  public boolean reseed(final long seed) {
    return inner.reseed(seed);
  }

}
