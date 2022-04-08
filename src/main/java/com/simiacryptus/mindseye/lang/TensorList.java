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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
 * This is the TensorList interface.
 *
 * @docgenVersion 9
 */
public interface TensorList extends ReferenceCounting {
  /**
   * Returns an array of integers representing the dimensions of the object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  int[] getDimensions();

  /**
   * Returns the number of elements in the collection.
   *
   * @docgenVersion 9
   */
  default int getElements() {
    return length() * Tensor.length(getDimensions());
  }

  /**
   * Returns the data of the tensor as an array.
   *
   * @docgenVersion 9
   */
  @NotNull
  static Tensor getData0(TensorList data) {
    Tensor tensor = data.get(0);
    data.freeRef();
    return tensor;
  }


  /**
   * Adds a new TensorList.
   *
   * @docgenVersion 9
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
   * Add the tensor to the list and free it.
   *
   * @docgenVersion 9
   */
  default TensorList addAndFree(@Nonnull final TensorList right) {
    assertAlive();
    right.assertAlive();
    return add(right);
  }

  /**
   * Returns a new TensorList that is the element-wise difference of this TensorList and the argument.
   *
   * @docgenVersion 9
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
   * Returns a copy of this TensorList.
   *
   * @docgenVersion 9
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
   * Returns the Tensor object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @RefAware
  Tensor get(int i);

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  double get(int tensorIndex, int elementIndex);

  /**
   * Returns the length of the string.
   *
   * @docgenVersion 9
   */
  int length();

  /**
   * Returns a stream of tensors.
   *
   * @docgenVersion 9
   */
  @Nonnull
  RefStream<Tensor> stream();

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * Adds a reference to the TensorList.
   *
   * @docgenVersion 9
   */
  @Nonnull
  TensorList addRef();

  /**
   * Returns the JSON element.
   *
   * @docgenVersion 9
   */
  default JsonElement getJson() {
    JsonArray array = new JsonArray();
    for (int i = 0; i < length(); i++) {
      Tensor tensor = get(i);
      array.add(tensor.getJson());
      tensor.freeRef();
    }
    return array;
  }

  /**
   * Returns the underlying JSON element.
   *
   * @docgenVersion 9
   */
  default JsonElement getJsonRaw() {
    JsonArray array = new JsonArray();
    for (int i = 0; i < length(); i++) {
      Tensor tensor = get(i);
      array.add(tensor.getJsonRaw());
      tensor.freeRef();
    }
    return array;
  }

}
