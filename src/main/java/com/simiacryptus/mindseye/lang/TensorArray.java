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
 * The type Tensor array.
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
   * Get data tensor [ ].
   *
   * @return the tensor [ ]
   */
  @Nonnull
  public Tensor[] getData() {
    return RefUtil.addRef(data);
  }

  @Nonnull
  @Override
  public int[] getDimensions() {
    return data[0].getDimensions();
  }

  /**
   * To string char sequence.
   *
   * @param <T>   the type parameter
   * @param limit the limit
   * @param data  the data
   * @return the char sequence
   */
  @Nonnull
  public static <T> CharSequence toString(int limit, @Nonnull T... data) {
    return data.length < limit ? RefArrays.toString(data)
        : "[" + RefUtil.get(RefArrays.stream(data).limit(limit).map(Object::toString).reduce((a, b) -> a + ", " + b))
        + ", ...]";
  }

  @Override
  @Nonnull
  @RefAware
  public Tensor get(final int i) {
    return data[i].addRef();
  }

  @Override
  public int length() {
    return data.length;
  }

  @Nonnull
  @Override
  public RefStream<Tensor> stream() {
    return RefArrays.stream(getData());
  }

  @Nonnull
  @Override
  public String toString() {
    return RefString.format("TensorArray{data=%s}", toString(9, getData()));
  }

  public void _free() {
    super._free();
    RefUtil.freeRef(data);
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  TensorArray addRef() {
    return (TensorArray) super.addRef();
  }
}
