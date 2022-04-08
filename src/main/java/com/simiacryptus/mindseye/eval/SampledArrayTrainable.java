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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.Util;
import com.simiacryptus.util.function.WeakCachedSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Supplier;

/**
 * This class represents a trainable array of samples.
 *
 * @param trainingData the data used to train the array
 * @param minSamples   the minimum number of samples
 * @param seed         the seed used to generate random numbers
 * @param trainingSize the size of the training data
 * @docgenVersion 9
 */
public class SampledArrayTrainable extends TrainableWrapper<ArrayTrainable>
    implements SampledTrainable, TrainableDataMask {

  @Nonnull
  private final RefList<? extends RefSupplier<Tensor[]>> trainingData;
  private int minSamples = 0;
  private long seed = Util.R.get().nextInt();
  private int trainingSize;

  /**
   * Instantiates a new Sampled array trainable.
   *
   * @param trainingData the training data
   * @param network      the network
   * @param trainingSize the training size
   */
  public SampledArrayTrainable(@Nonnull final RefList<? extends RefSupplier<Tensor[]>> trainingData, final Layer network,
                               final int trainingSize) {
    this(trainingData, network, trainingSize, trainingSize);
  }

  /**
   * Instantiates a new Sampled array trainable.
   *
   * @param trainingData the training data
   * @param network      the network
   * @param trainingSize the training size
   * @param batchSize    the batch size
   */
  public SampledArrayTrainable(@Nonnull final RefList<? extends RefSupplier<Tensor[]>> trainingData, @Nullable final Layer network,
                               final int trainingSize, final int batchSize) {
    super(new ArrayTrainable(null, network == null ? null : network.addRef(), batchSize));
    if (null != network)
      network.freeRef();
    if (0 == trainingData.size()) {
      trainingData.freeRef();
      throw new IllegalArgumentException();
    }
    this.trainingData = trainingData;
    this.trainingSize = trainingSize;
    reseed(RefSystem.nanoTime());
  }

  /**
   * Instantiates a new Sampled array trainable.
   *
   * @param trainingData the training data
   * @param network      the network
   * @param trainingSize the training size
   */
  public SampledArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network, final int trainingSize) {
    this(trainingData, network, trainingSize, trainingSize);
  }

  /**
   * Instantiates a new Sampled array trainable.
   *
   * @param trainingData the training data
   * @param network      the network
   * @param trainingSize the training size
   * @param batchSize    the batch size
   */
  public SampledArrayTrainable(@Nonnull final Tensor[][] trainingData, @Nullable final Layer network, final int trainingSize,
                               final int batchSize) {
    super(new ArrayTrainable(network == null ? null : network.addRef(), batchSize));
    if (null != network)
      network.freeRef();
    RefUtil.freeRef(getInner());
    if (0 == trainingData.length) {
      RefUtil.freeRef(trainingData);
      throw new IllegalArgumentException();
    }
    this.trainingData = RefArrays.stream(trainingData).map(obj -> {
      return RefUtil.wrapInterface((RefSupplier<Tensor[]>) new WeakCachedSupplier<Tensor[]>(
          () -> RefUtil.addRef(obj)), obj);
    }).collect(RefCollectors.toList());
    this.trainingSize = trainingSize;
    reseed(RefSystem.nanoTime());
  }

  /**
   * Returns the minimum number of samples required for this estimator.
   *
   * @docgenVersion 9
   */
  public int getMinSamples() {
    return minSamples;
  }

  /**
   * Sets the minimum number of samples required for a node to be
   * considered for splitting.
   *
   * @docgenVersion 9
   */
  public void setMinSamples(int minSamples) {
    this.minSamples = minSamples;
  }

  /**
   * Returns the number of training examples.
   *
   * @docgenVersion 9
   */
  @Override
  public int getTrainingSize() {
    return Math.max(minSamples, Math.min(trainingData.size(), trainingSize));
  }

  /**
   * Sets the training size.
   *
   * @docgenVersion 9
   */
  @Override
  public void setTrainingSize(final int trainingSize) {
    this.trainingSize = trainingSize;
    refreshSampledData();
  }

  /**
   * Sets the seed for the random number generator.
   *
   * @docgenVersion 9
   */
  private void setSeed(final int newValue) {
    if (seed == newValue)
      return;
    seed = newValue;
    refreshSampledData();
  }

  /**
   * Returns a SampledCachedTrainable that is a cached version of the
   * SampledTrainable.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this.addRef());
  }

  /**
   * Reseeds the random number generator with the given seed.
   *
   * @param seed the seed
   * @return true if the random number generator was reseeded, false otherwise
   * @docgenVersion 9
   */
  @Override
  public boolean reseed(final long seed) {
    setSeed(Util.R.get().nextInt());
    ArrayTrainable temp_00_0004 = getInner();
    assert temp_00_0004 != null;
    temp_00_0004.reseed(seed);
    temp_00_0004.freeRef();
    super.reseed(seed);
    return true;
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    trainingData.freeRef();
  }

  /**
   * Adds a reference to the SampledArrayTrainable.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SampledArrayTrainable addRef() {
    return (SampledArrayTrainable) super.addRef();
  }

  /**
   * Refreshes the sampled data.
   *
   * @docgenVersion 9
   */
  protected void refreshSampledData() {
    assert 0 < trainingData.size();
    Tensor[][] trainingData = null;
    if (0 < getTrainingSize() && getTrainingSize() < this.trainingData.size() - 1) {
      @Nonnull final Random random = new Random(seed);
      if (null != trainingData) RefUtil.freeRef(trainingData);
      trainingData = RefIntStream.generate(() -> random.nextInt(this.trainingData.size())).distinct()
          .mapToObj(this.trainingData::get).filter(x -> {
            if (x != null) {
              Tensor[] tensors = x.get();
              try {
                if (tensors != null) {
                  return true;
                }
              } finally {
                RefUtil.freeRef(tensors);
              }
            }
            return false;
          }).limit(getTrainingSize()).map(Supplier::get).toArray(Tensor[][]::new);
    } else {
      if (null != trainingData) RefUtil.freeRef(trainingData);
      trainingData = this.trainingData.stream().filter(refSupplier -> {
        if (refSupplier != null) {
          Tensor[] tensors = refSupplier.get();
          try {
            if (tensors != null) {
              return true;
            }
          } finally {
            RefUtil.freeRef(tensors);
          }
        }
        return false;
      }).limit(getTrainingSize()).map(Supplier::get).toArray(Tensor[][]::new);
    }
    ArrayTrainable inner = getInner();
    assert inner != null;
    inner.setTrainingData(trainingData);
    inner.freeRef();
  }
}
