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

import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefStream;

public @com.simiacryptus.ref.lang.RefAware interface TensorList extends ReferenceCounting {
  @Nonnull
  int[] getDimensions();

  default int getElements() {
    return length() * Tensor.length(getDimensions());
  }

  default TensorList add(@Nonnull final TensorList right) {
    if (right.length() == 0)
      return this;
    if (length() == 0)
      throw new IllegalArgumentException();
    assert length() == right.length();
    return new TensorArray(com.simiacryptus.ref.wrappers.RefIntStream.range(0, length()).mapToObj(i -> {
      Tensor b = right.get(i);
      return get(i).addAndFree(b);
    }).toArray(i -> new Tensor[i]));
  }

  default TensorList addAndFree(@Nonnull final TensorList right) {
    assertAlive();
    right.assertAlive();
    return add(right);
  }

  @Nonnull
  default TensorList minus(@Nonnull final TensorList right) {
    if (right.length() == 0)
      return this;
    if (length() == 0)
      throw new IllegalArgumentException();
    assert length() == right.length();
    return new TensorArray(com.simiacryptus.ref.wrappers.RefIntStream.range(0, length()).mapToObj(i -> {
      @Nullable
      Tensor a = get(i);
      @Nullable
      Tensor b = right.get(i);
      return a.minus(b);
    }).toArray(i -> new Tensor[i]));
  }

  default TensorList copy() {
    return new TensorArray(com.simiacryptus.ref.wrappers.RefIntStream.range(0, length()).mapToObj(i -> {
      @Nullable
      Tensor element = get(i);
      return element.copy();
    }).toArray(i -> new Tensor[i]));
  }

  @Nonnull
  Tensor get(int i);

  int length();

  com.simiacryptus.ref.wrappers.RefStream<Tensor> stream();

  public void _free();

  public TensorList addRef();

  public static @SuppressWarnings("unused") TensorList[] addRefs(TensorList[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TensorList::addRef)
        .toArray((x) -> new TensorList[x]);
  }

  public static @SuppressWarnings("unused") TensorList[][] addRefs(TensorList[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TensorList::addRefs)
        .toArray((x) -> new TensorList[x][]);
  }

}
