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

import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.util.Util;
import com.simiacryptus.util.function.WeakCachedSupplier;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

public @RefAware
class SampledArrayTrainable extends TrainableWrapper<ArrayTrainable>
    implements SampledTrainable, TrainableDataMask {

  private final RefList<? extends Supplier<Tensor[]>> trainingData;
  private int minSamples = 0;
  private long seed = Util.R.get().nextInt();
  private int trainingSize;

  public SampledArrayTrainable(@Nonnull final RefList<? extends Supplier<Tensor[]>> trainingData, final Layer network,
                               final int trainingSize) {
    this(trainingData, network, trainingSize, trainingSize);
    if (null != network)
      network.freeRef();
    trainingData.freeRef();
  }

  public SampledArrayTrainable(@Nonnull final RefList<? extends Supplier<Tensor[]>> trainingData, final Layer network,
                               final int trainingSize, final int batchSize) {
    super(new ArrayTrainable(null, network == null ? null : network.addRef(), batchSize));
    if (null != network)
      network.freeRef();
    if (0 == trainingData.size()) {
      trainingData.freeRef();
      throw new IllegalArgumentException();
    }
    {
      RefList<? extends Supplier<Tensor[]>> temp_00_0001 = trainingData == null
          ? null
          : trainingData.addRef();
      this.trainingData = temp_00_0001 == null ? null : temp_00_0001.addRef();
      if (null != temp_00_0001)
        temp_00_0001.freeRef();
    }
    trainingData.freeRef();
    this.trainingSize = trainingSize;
    reseed(com.simiacryptus.ref.wrappers.RefSystem.nanoTime());
  }

  public SampledArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network, final int trainingSize) {
    this(trainingData, network, trainingSize, trainingSize);
    if (null != network)
      network.freeRef();
    ReferenceCounting.freeRefs(trainingData);
  }

  public SampledArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network, final int trainingSize,
                               final int batchSize) {
    super(new ArrayTrainable(network == null ? null : network.addRef(), batchSize));
    if (null != network)
      network.freeRef();
    RefUtil.freeRef(getInner());
    if (0 == trainingData.length) {
      ReferenceCounting.freeRefs(trainingData);
      throw new IllegalArgumentException();
    }
    {
      RefList<? extends Supplier<Tensor[]>> temp_00_0002 = RefArrays
          .stream(Tensor.addRefs(trainingData)).map(obj -> {
            WeakCachedSupplier<Tensor[]> temp_00_0003 = new WeakCachedSupplier<>(
                RefUtil.wrapInterface(
                    () -> obj,
                    Tensor.addRefs(obj)));
            if (null != obj)
              ReferenceCounting.freeRefs(obj);
            return temp_00_0003;
          }).collect(RefCollectors.toList());
      this.trainingData = temp_00_0002 == null ? null : temp_00_0002.addRef();
      if (null != temp_00_0002)
        temp_00_0002.freeRef();
    }
    ReferenceCounting.freeRefs(trainingData);
    this.trainingSize = trainingSize;
    reseed(com.simiacryptus.ref.wrappers.RefSystem.nanoTime());
  }

  public int getMinSamples() {
    return minSamples;
  }

  @Nonnull
  public SampledArrayTrainable setMinSamples(final int minSamples) {
    this.minSamples = minSamples;
    return this.addRef();
  }

  @Override
  public int getTrainingSize() {
    return Math.max(minSamples, Math.min(trainingData.size(), trainingSize));
  }

  @Nonnull
  @Override
  public void setTrainingSize(final int trainingSize) {
    this.trainingSize = trainingSize;
    refreshSampledData();
  }

  private void setSeed(final int newValue) {
    if (seed == newValue)
      return;
    seed = newValue;
    refreshSampledData();
  }

  public static @SuppressWarnings("unused")
  SampledArrayTrainable[] addRefs(SampledArrayTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SampledArrayTrainable::addRef)
        .toArray((x) -> new SampledArrayTrainable[x]);
  }

  public static @SuppressWarnings("unused")
  SampledArrayTrainable[][] addRefs(SampledArrayTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SampledArrayTrainable::addRefs)
        .toArray((x) -> new SampledArrayTrainable[x][]);
  }

  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this.addRef());
  }

  @Override
  public boolean reseed(final long seed) {
    setSeed(Util.R.get().nextInt());
    ArrayTrainable temp_00_0004 = getInner();
    temp_00_0004.reseed(seed);
    if (null != temp_00_0004)
      temp_00_0004.freeRef();
    super.reseed(seed);
    return true;
  }

  public @SuppressWarnings("unused")
  void _free() {
    if (null != trainingData)
      trainingData.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  SampledArrayTrainable addRef() {
    return (SampledArrayTrainable) super.addRef();
  }

  protected void refreshSampledData() {
    assert 0 < trainingData.size();
    Tensor[][] trainingData;
    if (0 < getTrainingSize() && getTrainingSize() < this.trainingData.size() - 1) {
      @Nonnull final Random random = new Random(seed);
      trainingData = RefIntStream.generate(() -> random.nextInt(this.trainingData.size())).distinct()
          .mapToObj(i -> this.trainingData.get(i)).filter(x -> {
            return x != null && x.get() != null;
          }).limit(getTrainingSize()).map(x -> x.get()).toArray(i -> new Tensor[i][]);
    } else {
      trainingData = this.trainingData.stream().filter(x -> {
        return x != null && x.get() != null;
      }).limit(getTrainingSize()).map(x -> x.get()).toArray(i -> new Tensor[i][]);
    }
    ArrayTrainable temp_00_0005 = getInner();
    temp_00_0005.setTrainingData(Tensor.addRefs(trainingData));
    if (null != temp_00_0005)
      temp_00_0005.freeRef();
    if (null != trainingData)
      ReferenceCounting.freeRefs(trainingData);
  }
}
