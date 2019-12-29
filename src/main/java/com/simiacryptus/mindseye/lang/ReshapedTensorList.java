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

import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Stream;

public class ReshapedTensorList extends ReferenceCountingBase implements TensorList {
  @Nonnull
  private final TensorList inner;
  private final int[] dims;

  public ReshapedTensorList(@Nonnull TensorList inner, int[] toDim) {
    if (Tensor.length(inner.getDimensions()) != Tensor.length(toDim))
      throw new IllegalArgumentException(Arrays.toString(inner.getDimensions()) + " != " + Arrays.toString(toDim));
    this.inner = inner;
    this.dims = toDim;
  }

  @Nonnull
  @Override
  public int[] getDimensions() {
    return Arrays.copyOf(dims, dims.length);
  }

  @Nonnull
  public TensorList getInner() {
    return inner;
  }

  @Override
  public ReshapedTensorList addRef() {
    return (ReshapedTensorList) super.addRef();
  }

  @Nonnull
  @Override
  public Tensor get(int i) {
    assertAlive();
    @Nonnull
    Tensor tensor = inner.get(i);
    return tensor.reshapeCast(dims);
  }

  @Override
  public int length() {
    return inner.length();
  }

  @Override
  public Stream<Tensor> stream() {
    return inner.stream().map(t -> {
      return t.reshapeCast(dims);
    });
  }

  @Override
  protected void _free() {
  }
}
