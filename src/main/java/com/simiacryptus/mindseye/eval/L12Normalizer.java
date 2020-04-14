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

import com.google.common.util.concurrent.AtomicDouble;
import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.wrappers.RefCollection;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The type L 12 normalizer.
 */
public abstract class L12Normalizer extends TrainableBase {
  /**
   * The Inner.
   */
  @Nullable
  public final Trainable inner;
  private final boolean hideAdj = false;

  /**
   * Instantiates a new L 12 normalizer.
   *
   * @param inner the inner
   */
  public L12Normalizer(@Nullable final Trainable inner) {
    Trainable temp_01_0001 = inner == null ? null : inner.addRef();
    this.inner = temp_01_0001 == null ? null : temp_01_0001.addRef();
    if (null != temp_01_0001)
      temp_01_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  /**
   * To layer layer.
   *
   * @param id the id
   * @return the layer
   */
  @javax.annotation.Nullable
  public Layer toLayer(UUID id) {
    assert inner != null;
    DAGNetwork layer = (DAGNetwork) inner.getLayer();
    if (null == layer) return null;
    RefMap<UUID, Layer> layersById = layer.getLayersById();
    Layer temp_01_0004 = layersById.get(id);
    layersById.freeRef();
    layer.freeRef();
    return temp_01_0004;
  }

  /**
   * Gets layers.
   *
   * @param layers the layers
   * @return the layers
   */
  public RefCollection<Layer> getLayers(@Nonnull final RefCollection<UUID> layers) {
    RefList<Layer> temp_01_0003 = layers.stream().map(this::toLayer)
        //.filter(layer -> layer instanceof FullyConnectedLayer)
        .collect(RefCollectors.toList());
    layers.freeRef();
    return temp_01_0003;
  }

  @Nonnull
  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    assert inner != null;
    final PointSample innerMeasure = inner.measure(monitor);
    @Nonnull final DeltaSet<UUID> normalizationVector = new DeltaSet<UUID>();
    AtomicDouble valueAdj = new AtomicDouble(0);
    RefCollection<Layer> layers = getLayers(innerMeasure.delta.keySet());
    layers.forEach(layer -> {
      Delta<UUID> temp_01_0008 = innerMeasure.delta.get(layer.getId());
      assert temp_01_0008 != null;
      final double[] weights = temp_01_0008.target;
      temp_01_0008.freeRef();
      Delta<UUID> temp_01_0009 = normalizationVector.get(layer.getId(), weights);
      assert temp_01_0009 != null;
      @Nullable final double[] gradientAdj = temp_01_0009.getDelta();
      temp_01_0009.freeRef();
      final double factor_L1 = getL1(layer.addRef());
      final double factor_L2 = getL2(layer);
      assert null != gradientAdj;
      for (int i = 0; i < gradientAdj.length; i++) {
        final double sign = weights[i] < 0 ? -1.0 : 1.0;
        gradientAdj[i] += factor_L1 * sign + 2 * factor_L2 * weights[i];
        valueAdj.addAndGet((factor_L1 * sign + factor_L2 * weights[i]) * weights[i]);
      }
    });

    layers.freeRef();
    final DeltaSet<UUID> deltaSet = innerMeasure.delta.add(normalizationVector);
    final PointSample pointSample = new PointSample(deltaSet.addRef(),
        innerMeasure.weights.addRef(),
        innerMeasure.sum + (hideAdj ? 0 : valueAdj.get()),
        innerMeasure.rate,
        innerMeasure.count);
    deltaSet.freeRef();
    innerMeasure.freeRef();
    PointSample temp_01_0002 = pointSample.normalize();
    pointSample.freeRef();
    return temp_01_0002;
  }

  @Override
  public boolean reseed(final long seed) {
    assert inner != null;
    return inner.reseed(seed);
  }

  public void _free() {
    super._free();
    if (null != inner)
      inner.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  L12Normalizer addRef() {
    return (L12Normalizer) super.addRef();
  }

  /**
   * Gets l 1.
   *
   * @param layer the layer
   * @return the l 1
   */
  protected abstract double getL1(Layer layer);

  /**
   * Gets l 2.
   *
   * @param layer the layer
   * @return the l 2
   */
  protected abstract double getL2(Layer layer);

}
