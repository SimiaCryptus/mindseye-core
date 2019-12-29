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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

public final class ConstantResult extends Result {

  public ConstantResult(final Tensor... data) {
    this(new TensorArray(data));
  }

  public ConstantResult(TensorArray tensorArray) {
    super(tensorArray, (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList data) -> {
    });
  }

  public ConstantResult(final TensorList tensorList) {
    super(tensorList, (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList data) -> {
    });
  }

  @Override
  public boolean isAlive() {
    return false;
  }

  public static Result[] batchResultArray(@Nonnull final Tensor[]... input) {
    if (null == input)
      throw new IllegalArgumentException();
    return IntStream.range(0, input[0].length)
        .mapToObj(x -> IntStream.range(0, input.length).mapToObj(y -> input[y][x]).toArray(i -> new Tensor[i]))
        .map(tensors -> new TensorArray(tensors)).map(tensorArray -> new ConstantResult(tensorArray))
        .toArray(x -> new Result[x]);
  }

  public static Result[] singleResultArray(@Nonnull final Tensor[] input) {
    return Arrays.stream(input).map((@Nonnull final Tensor x) -> new ConstantResult(new TensorArray(x)))
        .toArray(i -> new Result[i]);
  }

  public static Result[] singleResultArray(@Nonnull final Tensor[][] input) {
    return Arrays.stream(input).map((@Nonnull final Tensor[] x) -> new ConstantResult(new TensorArray(x)))
        .toArray(i -> new Result[i]);
  }

}
