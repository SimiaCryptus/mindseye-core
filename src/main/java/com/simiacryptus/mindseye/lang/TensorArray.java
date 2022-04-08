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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * This class represents an array of tensors.
 * The data field is annotated with @Nonnull, meaning that it must not be null.
 *
 * @docgenVersion 9
 */
public class TensorArray extends ReferenceCountingBase implements TensorList, Serializable {
  @Nonnull
  private final Tensor[] data;

  /**
   * Instantiates a new Tensor array.
   *
   * @param data the data
   */
  public TensorArray(@Nonnull final Tensor... data) {
    if (0 >= data.length) {
      RefUtil.freeRef(data);
      throw new IllegalArgumentException();
    }
    this.data = RefArrays.copyOf(data, data.length);
    int[] dimensions0 = this.data[0].getDimensions();
    for (@Nonnull Tensor tensor : this.data) {
      int[] dimensions = tensor.getDimensions();
      assert RefArrays.equals(dimensions, dimensions0) :
          RefArrays.toString(dimensions) + " != " + RefArrays.toString(dimensions);
    }
  }

  /**
   * Returns an array of tensors.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor[] getData() {
    return RefUtil.addRef(data);
  }

  /**
   * Returns an array of integers representing the dimensions of the object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public int[] getDimensions() {
    return data[0].getDimensions();
  }

  /**
   * Converts this object to a String.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> CharSequence toString(int limit, @Nonnull T... data) {
    return data.length < limit ? RefArrays.toString(data)
        : "[" + RefUtil.get(RefArrays.stream(data).limit(limit).map(Object::toString).reduce((a, b) -> a + ", " + b))
        + ", ...]";
  }

  /**
   * Returns the Tensor object.
   *
   * @docgenVersion 9
   */
  @Override
  @Nonnull
  @RefAware
  public Tensor get(final int i) {
    return data[i].addRef();
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(int tensorIndex, int elementIndex) {
    return data[tensorIndex].get(elementIndex);
  }

  /**
   * Returns the length of the string.
   *
   * @docgenVersion 9
   */
  @Override
  public int length() {
    return data.length;
  }

  /**
   * Returns a stream of tensors.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<Tensor> stream() {
    return RefArrays.stream(getData());
  }

  /**
   * Returns a string representation of this object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return RefString.format("TensorArray{data=%s}", toString(9, getData()));
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    RefUtil.freeRef(data);
  }

  /**
   * Adds a reference to the TensorArray.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  TensorArray addRef() {
    return (TensorArray) super.addRef();
  }
}
