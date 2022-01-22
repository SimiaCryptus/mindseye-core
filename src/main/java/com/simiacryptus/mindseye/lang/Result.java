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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * The type Result.
 */
public class Result extends ReferenceCountingBase {
  private static final Result.Accumulator NULL_ACCUMULATOR = new NullAccumulator();
  /**
   * The Dims.
   */
  @Nonnull
  protected final int[] dims;
  /**
   * The Data length.
   */
  protected final int dataLength;
  /**
   * The Data.
   */
  @Nonnull
  protected final TensorList data;
  /**
   * The Accumulator.
   */
  protected final Result.Accumulator accumulator;
  private final boolean alive;

  /**
   * Instantiates a new Result.
   *
   * @param data the data
   */
  public Result(@Nonnull final TensorList data) {
    this(data, getNullAccumulator());
  }

  /**
   * Instantiates a new Result.
   *
   * @param data        the data
   * @param accumulator the accumulator
   */
  public Result(@Nonnull final TensorList data, Accumulator accumulator) {
    this(data, accumulator, !isNull(accumulator));
  }

  /**
   * Instantiates a new Result.
   *
   * @param data        the data
   * @param accumulator the accumulator
   * @param alive       the alive
   */
  public Result(@Nonnull final TensorList data, Result.Accumulator accumulator, boolean alive) {
    super();
    this.alive = alive;
    if (this.alive) {
      this.accumulator = accumulator;
    } else {
      accumulator.freeRef();
      this.accumulator = getNullAccumulator();
    }
    this.dims = data.getDimensions();
    this.dataLength = data.length();
    this.data = data;
  }

  public static int[] getDimensions(Result result) {
    TensorList data = result.getData();
    result.freeRef();
    @Nonnull int[] dimensions = data.getDimensions();
    data.freeRef();
    return dimensions;
  }

  /**
   * Gets accumulator.
   *
   * @return the accumulator
   */
  public final Result.Accumulator getAccumulator() {
    assertAlive();
    if (accumulator == null) return getNullAccumulator();
    return accumulator.addRef();
  }

  /**
   * Gets data.
   *
   * @return the data
   */
  @NotNull
  public final TensorList getData() {
    assertAlive();
    data.assertAlive();
    return data.addRef();
  }

  /**
   * Gets null accumulator.
   *
   * @return the null accumulator
   */
  public static Result.Accumulator getNullAccumulator() {
    return new NullAccumulator();
    //return NULL_ACCUMULATOR.addRef();
  }

  /**
   * Is alive boolean.
   *
   * @return the boolean
   */
  public boolean isAlive() {
    assertAlive();
    return alive;
  }

  /**
   * Is null boolean.
   *
   * @param accumulator the accumulator
   * @return the boolean
   */
  public static boolean isNull(@RefIgnore Accumulator accumulator) {
    return null == accumulator || accumulator instanceof NullAccumulator;
  }

  /**
   * Gets data.
   *
   * @param result the result
   * @return the data
   */
  @NotNull
  public static TensorList getData(Result result) {
    TensorList data = result.getData();
    result.freeRef();
    return data;
  }

  /**
   * Any alive boolean.
   *
   * @param inObj the in obj
   * @return the boolean
   */
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

  /**
   * Gets data 0.
   *
   * @param eval the eval
   * @return the data 0
   */
  public static Tensor getData0(Result eval) {
    return TensorList.getData0(getData(eval));
  }

  /**
   * Copy double [ ].
   *
   * @param delta the delta
   * @return the double [ ]
   */
  public double[] copy(double[] delta) {
    delta = RefArrays.copyOf(delta, delta.length);
    return delta;
  }

  /**
   * Accumulate.
   *
   * @param buffer the buffer
   */
  public final void accumulate(@Nullable final DeltaSet<UUID> buffer) {
    accumulate(buffer, 1.0);
  }

  /**
   * Accumulate.
   *
   * @param buffer the buffer
   * @param value  the value
   */
  public final void accumulate(@Nullable final DeltaSet<UUID> buffer, final double value) {
    accumulate(buffer, new TensorArray(IntStream.range(0, dataLength).mapToObj(x -> {
      Tensor tensor = new Tensor(dims);
      tensor.setAll(value);
      return tensor;
    }).toArray(Tensor[]::new)));
  }

  /**
   * Accumulate.
   *
   * @param buffer the buffer
   * @param delta  the delta
   */
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

  /**
   * The type Accumulator.
   */
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
