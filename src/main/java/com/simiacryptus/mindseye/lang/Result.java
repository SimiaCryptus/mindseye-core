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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class Result extends ReferenceCountingBase {
  private static final Result.Accumulator NULL_ACCUMULATOR = new NullAccumulator();
  @Nonnull
  protected final int[] dims;
  protected final int dataLength;
  //public final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  @Nonnull
  protected final TensorList data;
  @Nonnull
  protected final Result.Accumulator accumulator;
  private final boolean alive;

  public Result(@Nonnull final TensorList data) {
    this(data, NULL_ACCUMULATOR.addRef());
  }

  public Result(@Nonnull final TensorList data, @Nonnull Accumulator accumulator) {
    this(data, accumulator, null != accumulator && accumulator != NULL_ACCUMULATOR);
  }

  public Result(@Nonnull final TensorList data, @Nonnull Result.Accumulator accumulator, boolean alive) {
    super();
    this.alive = alive;
    if(this.alive) {
      this.accumulator = accumulator;
    } else {
      accumulator.freeRef();
      this.accumulator = Result.NULL_ACCUMULATOR.addRef();
    }
    this.dims = data.getDimensions();
    this.dataLength = data.length();
    this.data = data;
  }

  @Nullable
  public Result.Accumulator getAccumulator() {
    assertAlive();
    if(accumulator == null) return NULL_ACCUMULATOR.addRef();
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
    return alive;
  }

  @NotNull
  public static TensorList getData(Result result) {
    TensorList data = result.getData();
    result.freeRef();
    return data;
  }

  public static boolean anyAlive(Result[] inObj) {
    try {
      for (@Nonnull final Result element : inObj)
        if (element.isAlive()) {
          return true;
        }
      return false;
    } finally {
      RefUtil.freeRef(inObj);
    }
  }

  public double[] copy(double[] delta) {
    delta = RefArrays.copyOf(delta, delta.length);
    return delta;
  }

  public final void accumulate(@Nullable final DeltaSet<UUID> buffer) {
    accumulate(buffer, 1.0);
  }

  public final void accumulate(@Nullable final DeltaSet<UUID> buffer, final double value) {
    accumulate(buffer, new TensorArray(IntStream.range(0, dataLength).mapToObj(x -> {
      Tensor tensor = new Tensor(dims);
      tensor.setAll(value);
      return tensor;
    }).toArray(Tensor[]::new)));
  }

  public final void accumulate(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList delta) {
    assertAlive();
    assert accumulator != null;
    accumulator.accept(buffer, delta);
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
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
      super._free();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    Accumulator addRef() {
      return (Accumulator) super.addRef();
    }
  }

  private static final class NullAccumulator extends Result.Accumulator {
    @Override
    public void accept(@Nullable DeltaSet<UUID> deltaSet, @Nullable TensorList tensorList) {
      RefUtil.freeRef(tensorList);
      RefUtil.freeRef(deltaSet);
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
    }
  }
}
