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

import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefCollection;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

public abstract @RefAware
class L12Normalizer extends TrainableBase {
  public final Trainable inner;
  private final boolean hideAdj = false;

  public L12Normalizer(final Trainable inner) {
    {
      Trainable temp_01_0001 = inner == null ? null : inner.addRef();
      this.inner = temp_01_0001 == null ? null : temp_01_0001.addRef();
      if (null != temp_01_0001)
        temp_01_0001.freeRef();
    }
    if (null != inner)
      inner.freeRef();
  }

  public static @SuppressWarnings("unused")
  L12Normalizer[] addRefs(L12Normalizer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(L12Normalizer::addRef)
        .toArray((x) -> new L12Normalizer[x]);
  }

  public static @SuppressWarnings("unused")
  L12Normalizer[][] addRefs(L12Normalizer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(L12Normalizer::addRefs)
        .toArray((x) -> new L12Normalizer[x][]);
  }

  public Layer toLayer(UUID id) {
    RefMap<UUID, Layer> temp_01_0005 = ((DAGNetwork) inner
        .getLayer()).getLayersById();
    Layer temp_01_0004 = temp_01_0005.get(id);
    if (null != temp_01_0005)
      temp_01_0005.freeRef();
    return temp_01_0004;
  }

  public RefCollection<Layer> getLayers(@Nonnull final RefCollection<UUID> layers) {
    RefList<Layer> temp_01_0003 = layers.stream()
        .map(this::toLayer)
        //.filter(layer -> layer instanceof FullyConnectedLayer)
        .collect(RefCollectors.toList());
    layers.freeRef();
    return temp_01_0003;
  }

  @Nonnull
  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    final PointSample innerMeasure = inner.measure(monitor);
    @Nonnull final DeltaSet<UUID> normalizationVector = new DeltaSet<UUID>();
    double valueAdj = 0;
    RefMap<UUID, Delta<UUID>> temp_01_0006 = innerMeasure.delta
        .getMap();
    for (@Nonnull final Layer layer : getLayers(temp_01_0006.keySet())) {
      RefMap<UUID, Delta<UUID>> temp_01_0007 = innerMeasure.delta
          .getMap();
      Delta<UUID> temp_01_0008 = temp_01_0007.get(layer.getId());
      final double[] weights = temp_01_0008.target;
      if (null != temp_01_0008)
        temp_01_0008.freeRef();
      if (null != temp_01_0007)
        temp_01_0007.freeRef();
      Delta<UUID> temp_01_0009 = normalizationVector.get(layer.getId(),
          weights);
      @Nullable final double[] gradientAdj = temp_01_0009.getDelta();
      if (null != temp_01_0009)
        temp_01_0009.freeRef();
      final double factor_L1 = getL1(layer == null ? null : layer.addRef());
      final double factor_L2 = getL2(layer == null ? null : layer.addRef());
      assert null != gradientAdj;
      for (int i = 0; i < gradientAdj.length; i++) {
        final double sign = weights[i] < 0 ? -1.0 : 1.0;
        gradientAdj[i] += factor_L1 * sign + 2 * factor_L2 * weights[i];
        valueAdj += (factor_L1 * sign + factor_L2 * weights[i]) * weights[i];
      }
    }
    if (null != temp_01_0006)
      temp_01_0006.freeRef();
    final DeltaSet<UUID> deltaSet = innerMeasure.delta.add(normalizationVector == null ? null : normalizationVector);
    final PointSample pointSample = new PointSample(deltaSet == null ? null : deltaSet.addRef(),
        innerMeasure.weights.addRef(), innerMeasure.sum + (hideAdj ? 0 : valueAdj), innerMeasure.rate,
        innerMeasure.count);
    if (null != deltaSet)
      deltaSet.freeRef();
    if (null != innerMeasure)
      innerMeasure.freeRef();
    PointSample temp_01_0002 = pointSample.normalize();
    if (null != pointSample)
      pointSample.freeRef();
    return temp_01_0002;
  }

  @Override
  public boolean reseed(final long seed) {
    return inner.reseed(seed);
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  L12Normalizer addRef() {
    return (L12Normalizer) super.addRef();
  }

  protected abstract double getL1(Layer layer);

  protected abstract double getL2(Layer layer);

}
