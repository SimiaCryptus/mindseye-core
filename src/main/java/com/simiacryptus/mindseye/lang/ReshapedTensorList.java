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

package com.simiacryptus.mindseye.lang;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * This class represents a list of tensors that have been reshaped.
 *
 * @param inner The list of tensors that have been reshaped
 * @param dims  The new dimensions of the tensors in the list
 * @docgenVersion 9
 */
public class ReshapedTensorList extends ReferenceCountingBase implements TensorList {
  @Nonnull
  private final TensorList inner;
  private final int[] dims;

  /**
   * Instantiates a new Reshaped tensor list.
   *
   * @param inner the inner
   * @param dims  the dims
   */
  public ReshapedTensorList(@Nonnull TensorList inner, int[] dims) {
    int[] dimensions = inner.getDimensions();
    if (Tensor.length(dimensions) != Tensor.length(dims)) {
      inner.freeRef();
      throw new IllegalArgumentException(
          RefArrays.toString(dimensions) + " != " + RefArrays.toString(dims));
    }
    this.inner = inner;
    this.dims = dims;
  }

  /**
   * Returns a copy of the dimensions array.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public int[] getDimensions() {
    return RefArrays.copyOf(dims, dims.length);
  }

  /**
   * @return the inner TensorList
   * @docgenVersion 9
   */
  @Nonnull
  public TensorList getInner() {
    return inner.addRef();
  }

  /**
   * @param i
   * @return
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  @RefAware
  public Tensor get(int i) {
    assertAlive();
    return reshape(inner.get(i));
  }

  /**
   * Returns the element at the given tensor and element indices.
   *
   * @docgenVersion 9
   */
  public double get(int tensorIndex, int elementIndex) {
    return inner.get(tensorIndex, elementIndex);
  }

  /**
   * @Override public int length() {
   * return inner.length();
   * }
   * @docgenVersion 9
   */
  @Override
  public int length() {
    return inner.length();
  }

  /**
   * Returns a stream of tensors, reshaping each one according to the current shape.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<Tensor> stream() {
    return inner.stream().map(tensor -> reshape(tensor));
  }

  /**
   * Frees this object and its inner object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    inner.freeRef();
  }

  /**
   * Returns a reference to this object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ReshapedTensorList addRef() {
    return (ReshapedTensorList) super.addRef();
  }

  /**
   * Reshapes the given tensor to the given dimensions.
   *
   * @param tensor the tensor to reshape
   * @return the reshaped tensor
   * @docgenVersion 9
   */
  @NotNull
  private Tensor reshape(Tensor tensor) {
    Tensor reshapeCast = tensor.reshapeCast(dims);
    tensor.freeRef();
    return reshapeCast;
  }
}
