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
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The type Array trainable.
 */
public class ArrayTrainable extends BatchedTrainable implements TrainableDataMask {

  @Nullable
  private Tensor[][] trainingData;

  /**
   * Instantiates a new Array trainable.
   *
   * @param inner        the inner
   * @param trainingData the training data
   */
  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[]... trainingData) {
    this(inner, trainingData, trainingData.length);
  }

  /**
   * Instantiates a new Array trainable.
   *
   * @param inner        the inner
   * @param trainingData the training data
   * @param batchSize    the batch size
   */
  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[][] trainingData, int batchSize) {
    super(inner, batchSize);
    if (null != this.trainingData)
      RefUtil.freeRef(this.trainingData);
    this.trainingData = trainingData;
  }

  /**
   * Instantiates a new Array trainable.
   *
   * @param network   the network
   * @param batchSize the batch size
   */
  public ArrayTrainable(final Layer network, final int batchSize) {
    this(null, network, batchSize);
  }

  /**
   * Instantiates a new Array trainable.
   *
   * @param trainingData the training data
   * @param network      the network
   */
  public ArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network) {
    this(trainingData, network, trainingData.length);
  }

  /**
   * Instantiates a new Array trainable.
   *
   * @param trainingData the training data
   * @param network      the network
   * @param batchSize    the batch size
   */
  public ArrayTrainable(@Nullable final Tensor[][] trainingData, final Layer network, final int batchSize) {
    super(network, batchSize);
    if (null != this.trainingData)
      RefUtil.freeRef(this.trainingData);
    this.trainingData = trainingData;
  }

  @Nullable
  @Override
  public Tensor[][] getData() {
    assertAlive();
    return RefUtil.addRef(trainingData);
  }

  @Override
  public void setData(@Nonnull RefList<Tensor[]> tensors) {
    if (null != trainingData)
      RefUtil.freeRef(trainingData);
    trainingData = tensors.toArray(new Tensor[][]{});
    tensors.freeRef();
  }

  /**
   * Sets training data.
   *
   * @param tensors the tensors
   */
  public void setTrainingData(@Nonnull final Tensor[][] tensors) {
    if (null != this.trainingData)
      RefUtil.freeRef(this.trainingData);
    this.trainingData = tensors;
  }

  public void _free() {
    if (null != trainingData) {
      RefUtil.freeRef(trainingData);
      trainingData = null;
    }
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ArrayTrainable addRef() {
    return (ArrayTrainable) super.addRef();
  }

}
