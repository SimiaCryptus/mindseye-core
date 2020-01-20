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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArrayTrainable extends BatchedTrainable implements TrainableDataMask {

  @Nullable
  private Tensor[][] trainingData;

  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[]... trainingData) {
    this(inner, trainingData, trainingData.length);
  }

  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[][] trainingData, int batchSize) {
    super(inner, batchSize);
    Tensor[][] temp_03_0001 = RefUtil.addRefs(trainingData);
    if (null != this.trainingData)
      ReferenceCounting.freeRefs(this.trainingData);
    this.trainingData = RefUtil.addRefs(temp_03_0001);
    ReferenceCounting.freeRefs(temp_03_0001);
    ReferenceCounting.freeRefs(trainingData);
  }

  public ArrayTrainable(final Layer network, final int batchSize) {
    this(null, network, batchSize);
  }

  public ArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network) {
    this(trainingData, network, trainingData.length);
  }

  public ArrayTrainable(@Nullable final Tensor[][] trainingData, final Layer network, final int batchSize) {
    super(network, batchSize);
    Tensor[][] temp_03_0002 = RefUtil.addRefs(trainingData);
    if (null != this.trainingData)
      ReferenceCounting.freeRefs(this.trainingData);
    this.trainingData = RefUtil.addRefs(temp_03_0002);
    if (null != temp_03_0002)
      ReferenceCounting.freeRefs(temp_03_0002);
    if (null != trainingData)
      ReferenceCounting.freeRefs(trainingData);
  }

  @Nullable
  @Override
  public Tensor[][] getData() {
    return RefUtil.addRefs(trainingData);
  }

  public void setTrainingData(@Nonnull final Tensor[][] tensors) {
    Tensor[][] temp_03_0003 = RefUtil.addRefs(tensors);
    if (null != this.trainingData)
      ReferenceCounting.freeRefs(this.trainingData);
    this.trainingData = RefUtil.addRefs(temp_03_0003);
    ReferenceCounting.freeRefs(temp_03_0003);
    ReferenceCounting.freeRefs(tensors);
  }

  @Override
  public void setData(@Nonnull RefList<Tensor[]> tensors) {
    if (null != trainingData)
      ReferenceCounting.freeRefs(trainingData);
    trainingData = tensors.toArray(new Tensor[][]{});
    tensors.freeRef();
  }

  public void _free() {
    if (null != trainingData)
      ReferenceCounting.freeRefs(trainingData);
    trainingData = null;
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ArrayTrainable addRef() {
    return (ArrayTrainable) super.addRef();
  }

}
