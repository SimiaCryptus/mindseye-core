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

  public SampledArrayTrainable(
      @Nonnull final RefList<? extends Supplier<Tensor[]>> trainingData,
      final Layer network, final int trainingSize) {
    this(trainingData, network, trainingSize, trainingSize);
  }

  public SampledArrayTrainable(
      @Nonnull final RefList<? extends Supplier<Tensor[]>> trainingData,
      final Layer network, final int trainingSize, final int batchSize) {
    super(new ArrayTrainable(null, network, batchSize));
    if (0 == trainingData.size())
      throw new IllegalArgumentException();
    this.trainingData = trainingData;
    this.trainingSize = trainingSize;
    reseed(System.nanoTime());
  }

  public SampledArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network, final int trainingSize) {
    this(trainingData, network, trainingSize, trainingSize);
  }

  public SampledArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network, final int trainingSize,
                               final int batchSize) {
    super(new ArrayTrainable(network, batchSize));
    getInner();
    if (0 == trainingData.length)
      throw new IllegalArgumentException();
    this.trainingData = RefArrays.stream(trainingData)
        .map(obj -> new WeakCachedSupplier<>(() -> obj)).collect(RefCollectors.toList());
    this.trainingSize = trainingSize;
    reseed(System.nanoTime());
  }

  public int getMinSamples() {
    return minSamples;
  }

  @Nonnull
  public SampledArrayTrainable setMinSamples(final int minSamples) {
    this.minSamples = minSamples;
    return this;
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
    return new SampledCachedTrainable<>(this);
  }

  @Override
  public boolean reseed(final long seed) {
    setSeed(Util.R.get().nextInt());
    getInner().reseed(seed);
    super.reseed(seed);
    return true;
  }

  public @SuppressWarnings("unused")
  void _free() {
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
      trainingData = RefIntStream.generate(() -> random.nextInt(this.trainingData.size()))
          .distinct().mapToObj(i -> this.trainingData.get(i)).filter(x -> x != null && x.get() != null)
          .limit(getTrainingSize()).map(x -> x.get()).toArray(i -> new Tensor[i][]);
    } else {
      trainingData = this.trainingData.stream().filter(x -> x != null && x.get() != null).limit(getTrainingSize())
          .map(x -> x.get()).toArray(i -> new Tensor[i][]);
    }
    getInner().setTrainingData(trainingData);
  }
}
