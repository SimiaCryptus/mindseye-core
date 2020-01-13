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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.IntFunction;

public interface TensorList extends ReferenceCounting {
  @Nonnull
  int[] getDimensions();

  default int getElements() {
    return length() * Tensor.length(getDimensions());
  }

  public static @SuppressWarnings("unused") TensorList[] addRefs(TensorList[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TensorList::addRef).toArray((x) -> new TensorList[x]);
  }

  public static @SuppressWarnings("unused") TensorList[][] addRefs(TensorList[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TensorList::addRefs).toArray((x) -> new TensorList[x][]);
  }

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
    TensorArray temp_40_0004 = new TensorArray(
        RefIntStream.range(0, length()).mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) i -> {
          Tensor b = right.get(i);
          Tensor temp_40_0007 = get(i);
          Tensor temp_40_0001 = temp_40_0007.addAndFree(b == null ? null : b.addRef());
          if (null != temp_40_0007)
            temp_40_0007.freeRef();
          if (null != b)
            b.freeRef();
          return temp_40_0001;
        }, right == null ? null : right)).toArray(i -> new Tensor[i]));
    return temp_40_0004;
  }

  default TensorList addAndFree(@Nonnull final TensorList right) {
    assertAlive();
    right.assertAlive();
    TensorList temp_40_0005 = add(right == null ? null : right);
    return temp_40_0005;
  }

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
    TensorArray temp_40_0006 = new TensorArray(
        RefIntStream.range(0, length()).mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) i -> {
          @Nullable
          Tensor a = get(i);
          @Nullable
          Tensor b = right.get(i);
          Tensor temp_40_0002 = a.minus(b == null ? null : b.addRef());
          if (null != b)
            b.freeRef();
          if (null != a)
            a.freeRef();
          return temp_40_0002;
        }, right == null ? null : right)).toArray(i -> new Tensor[i]));
    return temp_40_0006;
  }

  default TensorList copy() {
    return new TensorArray(RefIntStream.range(0, length()).mapToObj(i -> {
      @Nullable
      Tensor element = get(i);
      Tensor temp_40_0003 = element.copy();
      if (null != element)
        element.freeRef();
      return temp_40_0003;
    }).toArray(i -> new Tensor[i]));
  }

  @Nonnull
  Tensor get(int i);

  int length();

  RefStream<Tensor> stream();

  public void _free();

  public TensorList addRef();

}
