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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.IntFunction;

public final class ConstantResult extends Result {

  public ConstantResult(@Nullable final Tensor... data) {
    this(new TensorArray(Tensor.addRefs(data)));
    if (null != data)
      ReferenceCounting.freeRefs(data);
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
      }
    });
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  @Nonnull
  public static Result[] batchResultArray(@Nonnull final Tensor[]... input) {
    Result[] temp_44_0003 = RefIntStream.range(0, input[0].length)
        .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor[]>) x -> RefIntStream.range(0, input.length)
            .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) y -> input[y][x], Tensor.addRefs(input)))
            .toArray(i -> new Tensor[i]), Tensor.addRefs(input)))
        .map(tensors -> {
          TensorArray temp_44_0001 = new TensorArray(Tensor.addRefs(tensors));
          if (null != tensors)
            ReferenceCounting.freeRefs(tensors);
          return temp_44_0001;
        }).map(tensorArray -> {
          ConstantResult temp_44_0002 = new ConstantResult(tensorArray.addRef());
          tensorArray.freeRef();
          return temp_44_0002;
        }).toArray(x -> new Result[x]);
    ReferenceCounting.freeRefs(input);
    return temp_44_0003;
  }

  @Nonnull
  public static Result[] singleResultArray(@Nonnull final Tensor[] input) {
    Result[] temp_44_0004 = RefArrays.stream(Tensor.addRefs(input)).map((@Nonnull final Tensor x) -> {
      ConstantResult temp_44_0005 = new ConstantResult(new TensorArray(x == null ? null : x.addRef()));
      if (null != x)
        x.freeRef();
      return temp_44_0005;
    }).toArray(i -> new Result[i]);
    ReferenceCounting.freeRefs(input);
    return temp_44_0004;
  }

  @Nonnull
  public static Result[] singleResultArray(@Nonnull final Tensor[][] input) {
    Result[] temp_44_0006 = RefArrays.stream(Tensor.addRefs(input)).map((@Nonnull final Tensor[] x) -> {
      ConstantResult temp_44_0007 = new ConstantResult(new TensorArray(Tensor.addRefs(x)));
      if (null != x)
        ReferenceCounting.freeRefs(x);
      return temp_44_0007;
    }).toArray(i -> new Result[i]);
    ReferenceCounting.freeRefs(input);
    return temp_44_0006;
  }

  @Nullable
  public static @SuppressWarnings("unused")
  ConstantResult[] addRefs(@Nullable ConstantResult[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ConstantResult::addRef)
        .toArray((x) -> new ConstantResult[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  ConstantResult[][] addRefs(@Nullable ConstantResult[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ConstantResult::addRefs)
        .toArray((x) -> new ConstantResult[x][]);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ConstantResult addRef() {
    return (ConstantResult) super.addRef();
  }

}
