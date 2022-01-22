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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefStream;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntFunction;

/**
 * The interface Tensor list.
 */
public interface TensorList extends ReferenceCounting {
  /**
   * Get dimensions int [ ].
   *
   * @return the int [ ]
   */
  @Nonnull
  int[] getDimensions();

  /**
   * Gets elements.
   *
   * @return the elements
   */
  default int getElements() {
    return length() * Tensor.length(getDimensions());
  }

  /**
   * Gets data 0.
   *
   * @param data the data
   * @return the data 0
   */
  @NotNull
  static Tensor getData0(TensorList data) {
    Tensor tensor = data.get(0);
    data.freeRef();
    return tensor;
  }


  /**
   * Add tensor list.
   *
   * @param right the right
   * @return the tensor list
   */
  default TensorList add(@Nonnull final TensorList right) {
    if (right.length() == 0) {
      right.freeRef();
      return this.addRef();
    }
    if (length() == 0) {
      right.freeRef();
      throw new IllegalArgumentException();
    }
    assert length() == right.length();
    return new TensorArray(
        RefIntStream.range(0, length()).mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) i -> {
          return Tensor.add(get(i), right.get(i));
        }, right)).toArray(Tensor[]::new));
  }

  /**
   * Add and free tensor list.
   *
   * @param right the right
   * @return the tensor list
   */
  default TensorList addAndFree(@Nonnull final TensorList right) {
    assertAlive();
    right.assertAlive();
    return add(right);
  }

  /**
   * Minus tensor list.
   *
   * @param right the right
   * @return the tensor list
   */
  @Nonnull
  default TensorList minus(@Nonnull final TensorList right) {
    if (right.length() == 0) {
      right.freeRef();
      return this.addRef();
    }
    if (length() == 0) {
      right.freeRef();
      throw new IllegalArgumentException();
    }
    assert length() == right.length();
    return new TensorArray(
        RefIntStream.range(0, length()).mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) i -> {
          @Nullable
          Tensor a = get(i);
          @Nullable
          Tensor b = right.get(i);
          Tensor temp_40_0002 = a.minus(b.addRef());
          b.freeRef();
          a.freeRef();
          return temp_40_0002;
        }, right)).toArray(Tensor[]::new));
  }

  /**
   * Copy tensor list.
   *
   * @return the tensor list
   */
  default TensorList copy() {
    return new TensorArray(RefIntStream.range(0, length()).mapToObj(i -> {
      @Nullable
      Tensor element = get(i);
      Tensor temp_40_0003 = element.copy();
      element.freeRef();
      return temp_40_0003;
    }).toArray(Tensor[]::new));
  }

  /**
   * Get tensor.
   *
   * @param i the
   * @return the tensor
   */
  @Nonnull
  @RefAware
  Tensor get(int i);

  double get(int tensorIndex, int elementIndex);

  /**
   * Length int.
   *
   * @return the int
   */
  int length();

  /**
   * Stream ref stream.
   *
   * @return the ref stream
   */
  @Nonnull
  RefStream<Tensor> stream();

  /**
   * Free.
   */
  void _free();

  @Nonnull
  TensorList addRef();

}
