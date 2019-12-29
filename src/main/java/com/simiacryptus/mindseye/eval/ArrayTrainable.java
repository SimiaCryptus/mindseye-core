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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ArrayTrainable extends BatchedTrainable implements TrainableDataMask {

  @Nullable
  private Tensor[][] trainingData;

  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[]... trainingData) {
    this(inner, trainingData, trainingData.length);
  }

  public ArrayTrainable(DataTrainable inner, @Nonnull Tensor[][] trainingData, int batchSize) {
    super(inner, batchSize);
    this.trainingData = trainingData;
  }

  public ArrayTrainable(final Layer network, final int batchSize) {
    this(null, network, batchSize);
  }

  public ArrayTrainable(@Nonnull final Tensor[][] trainingData, final Layer network) {
    this(trainingData, network, trainingData.length);
  }

  public ArrayTrainable(@Nullable final Tensor[][] trainingData, final Layer network, final int batchSize) {
    super(network, batchSize);
    this.trainingData = trainingData;
  }

  @Nullable
  @Override
  public Tensor[][] getData() {
    return trainingData;
  }

  @Override
  public ArrayTrainable setVerbose(final boolean verbose) {
    return (ArrayTrainable) super.setVerbose(verbose);
  }

  @Override
  protected void _free() {
    super._free();
  }

  @Nonnull
  @Override
  public Trainable setData(@Nonnull final List<Tensor[]> tensors) {
    trainingData = tensors.toArray(new Tensor[][] {});
    return this;
  }

  public void setTrainingData(@Nonnull final Tensor[][] tensors) {
    this.trainingData = tensors;
  }

  @Nonnull
  @Override
  public ArrayTrainable setMask(boolean... mask) {
    return (ArrayTrainable) super.setMask(mask);
  }

}
