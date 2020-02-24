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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.IntFunction;

public final class ConstantResult extends Result {

  public ConstantResult(@Nullable final Tensor... data) {
    this(new TensorArray(RefUtil.addRefs(data)));
    if (null != data)
      RefUtil.freeRef(data);
  }

  public ConstantResult(@Nonnull TensorArray tensorArray) {
    super(tensorArray, new Accumulator() {
      @Override
      public void accept(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList data) {
        if (null != data)
          data.freeRef();
        if (null != buffer)
          buffer.freeRef();
      }

      public @SuppressWarnings("unused")
      void _free() {
        super._free();
      }
    });
  }

  public ConstantResult(@Nonnull final TensorList tensorList) {
    super(tensorList, new Accumulator() {
      @Override
      public void accept(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList data) {
        if (null != data)
          data.freeRef();
        if (null != buffer)
          buffer.freeRef();
      }

      public @SuppressWarnings("unused")
      void _free() {
        super._free();
      }
    });
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  @Nonnull
  public static Result[] batchResultArray(@Nonnull final Tensor[]... input) {
    return RefIntStream.range(0, input[0].length)
        .mapToObj(
            RefUtil.wrapInterface((IntFunction<Tensor[]>) x ->
                    RefIntStream.range(0, input.length)
                        .mapToObj(y -> input[y][x].addRef())
                        .toArray(Tensor[]::new),
                input))
        .map(TensorArray::new)
        .map(ConstantResult::new)
        .toArray(Result[]::new);
  }

  @Nonnull
  public static Result[] singleResultArray(@Nonnull final Tensor[] input) {
    return RefArrays.stream(input)
        .map(tensor -> new ConstantResult(new TensorArray(tensor)))
        .toArray(Result[]::new);
  }

  @Nonnull
  public static Result[] singleResultArray(@Nonnull final Tensor[][] input) {
    return RefArrays.stream(input)
        .map((@Nonnull final Tensor[] x) -> new ConstantResult(new TensorArray(x)))
        .toArray(Result[]::new);
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ConstantResult addRef() {
    return (ConstantResult) super.addRef();
  }

  @Override
  public void _free() {
    super._free();
  }
}
