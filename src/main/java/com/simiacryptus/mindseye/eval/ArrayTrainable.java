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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class ArrayTrainable extends BatchedTrainable implements TrainableDataMask {

  @Nullable
  private Tensor[][] trainingData;

  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[]... trainingData) {
    this(inner, trainingData, trainingData.length);
  }

  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[][] trainingData, int batchSize) {
    super(inner, batchSize);
    Tensor[][] temp_03_0001 = Tensor.addRefs(trainingData);
    if (null != this.trainingData)
      ReferenceCounting.freeRefs(this.trainingData);
    this.trainingData = Tensor.addRefs(temp_03_0001);
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
    Tensor[][] temp_03_0002 = Tensor.addRefs(trainingData);
    if (null != this.trainingData)
      ReferenceCounting.freeRefs(this.trainingData);
    this.trainingData = Tensor.addRefs(temp_03_0002);
    if (null != temp_03_0002)
      ReferenceCounting.freeRefs(temp_03_0002);
    if (null != trainingData)
      ReferenceCounting.freeRefs(trainingData);
  }

  @Nullable
  @Override
  public Tensor[][] getData() {
    return Tensor.addRefs(trainingData);
  }

  @Nonnull
  @Override
  public ArrayTrainable setMask(boolean... mask) {
    return (ArrayTrainable) super.setMask(mask);
  }

  public void setTrainingData(@Nonnull final Tensor[][] tensors) {
    Tensor[][] temp_03_0003 = Tensor.addRefs(tensors);
    if (null != this.trainingData)
      ReferenceCounting.freeRefs(this.trainingData);
    this.trainingData = Tensor.addRefs(temp_03_0003);
    ReferenceCounting.freeRefs(temp_03_0003);
    ReferenceCounting.freeRefs(tensors);
  }

  @Nonnull
  @Override
  public ArrayTrainable setVerbose(final boolean verbose) {
    return (ArrayTrainable) super.setVerbose(verbose);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  ArrayTrainable[] addRefs(@Nullable ArrayTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ArrayTrainable::addRef)
        .toArray((x) -> new ArrayTrainable[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  ArrayTrainable[][] addRefs(@Nullable ArrayTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ArrayTrainable::addRefs)
        .toArray((x) -> new ArrayTrainable[x][]);
  }

  @Nonnull
  @Override
  public Trainable setData(@Nonnull final RefList<Tensor[]> tensors) {
    Tensor[][] temp_03_0004 = tensors.toArray(new Tensor[][]{});
    if (null != trainingData)
      ReferenceCounting.freeRefs(trainingData);
    trainingData = Tensor.addRefs(temp_03_0004);
    ReferenceCounting.freeRefs(temp_03_0004);
    tensors.freeRef();
    return this.addRef();
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
