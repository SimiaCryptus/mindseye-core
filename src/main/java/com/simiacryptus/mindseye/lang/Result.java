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
import java.util.UUID;
import java.util.function.BiConsumer;

public @com.simiacryptus.ref.lang.RefAware
class Result extends ReferenceCountingBase {
  public final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  @Nonnull
  protected final TensorList data;
  @Nonnull
  protected final int[] dims;
  protected final int dataLength;
  @Nonnull
  protected final BiConsumer<DeltaSet<UUID>, TensorList> accumulator;

  public Result(@Nonnull final TensorList data, @Nonnull BiConsumer<DeltaSet<UUID>, TensorList> accumulator) {
    super();
    this.data = data;
    this.accumulator = accumulator;
    this.dims = data.getDimensions();
    this.dataLength = data.length();
  }

  public BiConsumer<DeltaSet<UUID>, TensorList> getAccumulator() {
    assertAlive();
    return accumulator;
  }

  public final TensorList getData() {
    assertAlive();
    data.assertAlive();
    return data;
  }

  public double[] getSingleDelta() {
    DeltaSet<UUID> deltaBuffer = new DeltaSet<>();
    accumulate(deltaBuffer);
    if (deltaBuffer.getMap().size() != 1)
      throw new AssertionError(deltaBuffer.getMap().size());
    return copy(deltaBuffer.getMap().values().iterator().next().getDelta());
  }

  public boolean isAlive() {
    return null != getAccumulator();
  }

  public static @SuppressWarnings("unused")
  Result[] addRefs(Result[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(Result::addRef).toArray((x) -> new Result[x]);
  }

  public static @SuppressWarnings("unused")
  Result[][] addRefs(Result[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(Result::addRefs).toArray((x) -> new Result[x][]);
  }

  public double[] copy(double[] delta) {
    delta = com.simiacryptus.ref.wrappers.RefArrays.copyOf(delta, delta.length);
    return delta;
  }

  public final void accumulate(final DeltaSet<UUID> buffer) {
    accumulate(buffer, 1.0);
  }

  public final void accumulate(final DeltaSet<UUID> buffer, final double value) {

    accumulate(buffer, new TensorArray(com.simiacryptus.ref.wrappers.RefIntStream.range(0, dataLength)
        .mapToObj(x -> new Tensor(dims).setAll(value)).toArray(i -> new Tensor[i])));
  }

  public void accumulate(DeltaSet<UUID> buffer, TensorList delta) {
    getAccumulator().accept(buffer, delta);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  Result addRef() {
    return (Result) super.addRef();
  }
}
