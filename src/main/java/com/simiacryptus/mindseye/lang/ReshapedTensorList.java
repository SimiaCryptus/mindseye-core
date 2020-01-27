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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class ReshapedTensorList extends ReferenceCountingBase implements TensorList {
  @Nonnull
  private final TensorList inner;
  private final int[] dims;

  public ReshapedTensorList(@Nonnull TensorList inner, int[] toDim) {
    if (Tensor.length(inner.getDimensions()) != Tensor.length(toDim)) {
      IllegalArgumentException temp_28_0004 = new IllegalArgumentException(
          RefArrays.toString(inner.getDimensions()) + " != " + RefArrays.toString(toDim));
      inner.freeRef();
      throw temp_28_0004;
    }
    TensorList temp_28_0001 = inner.addRef();
    this.inner = temp_28_0001.addRef();
    temp_28_0001.freeRef();
    inner.freeRef();
    this.dims = toDim;
  }

  @Nonnull
  @Override
  public int[] getDimensions() {
    return RefArrays.copyOf(dims, dims.length);
  }

  @Nonnull
  public TensorList getInner() {
    return inner.addRef();
  }

  @Nonnull
  @Override
  @RefAware
  public Tensor get(int i) {
    assertAlive();
    @Nonnull
    Tensor tensor = inner.get(i);
    Tensor temp_28_0002 = tensor.reshapeCast(dims);
    tensor.freeRef();
    return temp_28_0002;
  }

  @Override
  public int length() {
    return inner.length();
  }

  @Nonnull
  @Override
  public RefStream<Tensor> stream() {
    return inner.stream().map(t -> {
      Tensor temp_28_0003 = t.reshapeCast(dims);
      t.freeRef();
      return temp_28_0003;
    });
  }

  public void _free() {
    super._free();
    inner.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ReshapedTensorList addRef() {
    return (ReshapedTensorList) super.addRef();
  }
}
