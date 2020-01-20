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
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;

public class Result extends ReferenceCountingBase {
  @Nonnull
  protected final int[] dims;
  protected final int dataLength;
  //public final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  @Nonnull
  private final TensorList data;
  @Nonnull
  private final Result.Accumulator accumulator;

  public Result(@Nonnull final TensorList data, @Nonnull Result.Accumulator accumulator) {
    super();
    this.accumulator = accumulator;
    this.dims = data.getDimensions();
    this.dataLength = data.length();
    this.data = data;
  }

  @Nullable
  public Result.Accumulator getAccumulator() {
    assertAlive();
    return accumulator.addRef();
  }

  @NotNull
  public final TensorList getData() {
    assertAlive();
    data.assertAlive();
    return data.addRef();
  }

  public boolean isAlive() {
    assertAlive();
    return null != accumulator;
  }


  public double[] copy(double[] delta) {
    delta = RefArrays.copyOf(delta, delta.length);
    return delta;
  }

  public final void accumulate(@Nullable final DeltaSet<UUID> buffer) {
    accumulate(buffer == null ? null : buffer.addRef(), 1.0);
    if (null != buffer)
      buffer.freeRef();
  }

  public final void accumulate(@Nullable final DeltaSet<UUID> buffer, final double value) {

    accumulate(buffer == null ? null : buffer.addRef(),
        new TensorArray(RefIntStream.range(0, dataLength).mapToObj(x -> {
          Tensor tensor = new Tensor(dims);
          tensor.setAll(value);
          return tensor;
        }).toArray(i -> new Tensor[i])));
    if (null != buffer)
      buffer.freeRef();
  }

  public void accumulate(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList delta) {
    assertAlive();
    assert accumulator != null;
    accumulator.accept(buffer, delta);
  }

  public @SuppressWarnings("unused")
  void _free() {
    accumulator.freeRef();
    data.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  Result addRef() {
    return (Result) super.addRef();
  }

  public static abstract class Accumulator extends ReferenceCountingBase
      implements BiConsumer<DeltaSet<UUID>, TensorList> {

    public @SuppressWarnings("unused")
    void _free() {
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    Accumulator addRef() {
      return (Accumulator) super.addRef();
    }
  }
}
