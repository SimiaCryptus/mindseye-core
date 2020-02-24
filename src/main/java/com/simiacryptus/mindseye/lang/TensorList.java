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
import java.util.function.IntFunction;

public interface TensorList extends ReferenceCounting {
  @Nonnull
  int[] getDimensions();

  default int getElements() {
    return length() * Tensor.length(getDimensions());
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
    return new TensorArray(
        RefIntStream.range(0, length()).mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) i -> {
          return Tensor.add(get(i), right.get(i));
        }, right)).toArray(Tensor[]::new));
  }

  default TensorList addAndFree(@Nonnull final TensorList right) {
    assertAlive();
    right.assertAlive();
    return add(right);
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

  default TensorList copy() {
    return new TensorArray(RefIntStream.range(0, length()).mapToObj(i -> {
      @Nullable
      Tensor element = get(i);
      Tensor temp_40_0003 = element.copy();
      element.freeRef();
      return temp_40_0003;
    }).toArray(Tensor[]::new));
  }

  @Nonnull
  @RefAware
  Tensor get(int i);

  int length();

  @Nonnull
  RefStream<Tensor> stream();

  void _free();

  @Nonnull
  TensorList addRef();

}
