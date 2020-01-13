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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;

public class Result extends ReferenceCountingBase {
  public final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  @Nonnull
  protected final TensorList data;
  @Nonnull
  protected final int[] dims;
  protected final int dataLength;
  @Nonnull
  protected final Result.Accumulator accumulator;

  public Result(@Nonnull final TensorList data, @Nonnull Result.Accumulator accumulator) {
    super();
    TensorList temp_16_0001 = data == null ? null : data.addRef();
    this.data = temp_16_0001 == null ? null : temp_16_0001.addRef();
    if (null != temp_16_0001)
      temp_16_0001.freeRef();
    Accumulator temp_16_0002 = accumulator == null ? null : accumulator.addRef();
    this.accumulator = temp_16_0002 == null ? null : temp_16_0002.addRef();
    if (null != temp_16_0002)
      temp_16_0002.freeRef();
    accumulator.freeRef();
    this.dims = data.getDimensions();
    this.dataLength = data.length();
    data.freeRef();
  }

  public Result.Accumulator getAccumulator() {
    assertAlive();
    return accumulator == null ? null : accumulator.addRef();
  }

  public final TensorList getData() {
    assertAlive();
    data.assertAlive();
    return data == null ? null : data.addRef();
  }

  public double[] getSingleDelta() {
    DeltaSet<UUID> deltaBuffer = new DeltaSet<>();
    accumulate(deltaBuffer == null ? null : deltaBuffer.addRef());
    RefMap<UUID, Delta<UUID>> temp_16_0005 = deltaBuffer.getMap();
    if (temp_16_0005.size() != 1) {
      RefMap<UUID, Delta<UUID>> temp_16_0006 = deltaBuffer.getMap();
      AssertionError temp_16_0004 = new AssertionError(temp_16_0006.size());
      if (null != temp_16_0006)
        temp_16_0006.freeRef();
      if (null != deltaBuffer)
        deltaBuffer.freeRef();
      throw temp_16_0004;
    }
    if (null != temp_16_0005)
      temp_16_0005.freeRef();
    RefMap<UUID, Delta<UUID>> temp_16_0007 = deltaBuffer.getMap();
    RefCollection<Delta<UUID>> temp_16_0008 = temp_16_0007.values();
    RefIterator<Delta<UUID>> temp_16_0009 = temp_16_0008.iterator();
    Delta<UUID> temp_16_0010 = temp_16_0009.next();
    double[] temp_16_0003 = copy(temp_16_0010.getDelta());
    if (null != temp_16_0010)
      temp_16_0010.freeRef();
    if (null != temp_16_0009)
      temp_16_0009.freeRef();
    if (null != temp_16_0008)
      temp_16_0008.freeRef();
    if (null != temp_16_0007)
      temp_16_0007.freeRef();
    if (null != deltaBuffer)
      deltaBuffer.freeRef();
    return temp_16_0003;
  }

  public boolean isAlive() {
    Result.Accumulator temp_16_0012 = getAccumulator();
    boolean temp_16_0011 = null != temp_16_0012;
    if (null != temp_16_0012)
      temp_16_0012.freeRef();
    return temp_16_0011;
  }

  public static @SuppressWarnings("unused") Result[] addRefs(Result[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Result::addRef).toArray((x) -> new Result[x]);
  }

  public static @SuppressWarnings("unused") Result[][] addRefs(Result[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Result::addRefs).toArray((x) -> new Result[x][]);
  }

  public double[] copy(double[] delta) {
    delta = RefArrays.copyOf(delta, delta.length);
    return delta;
  }

  public final void accumulate(final DeltaSet<UUID> buffer) {
    accumulate(buffer == null ? null : buffer.addRef(), 1.0);
    if (null != buffer)
      buffer.freeRef();
  }

  public final void accumulate(final DeltaSet<UUID> buffer, final double value) {

    accumulate(buffer == null ? null : buffer.addRef(),
        new TensorArray(RefIntStream.range(0, dataLength).mapToObj(x -> {
          return new Tensor(dims).setAll(value);
        }).toArray(i -> new Tensor[i])));
    if (null != buffer)
      buffer.freeRef();
  }

  public void accumulate(DeltaSet<UUID> buffer, TensorList delta) {
    Result.Accumulator temp_16_0013 = getAccumulator();
    temp_16_0013.accept(buffer == null ? null : buffer.addRef(), delta == null ? null : delta.addRef());
    if (null != temp_16_0013)
      temp_16_0013.freeRef();
    if (null != delta)
      delta.freeRef();
    if (null != buffer)
      buffer.freeRef();
  }

  public @SuppressWarnings("unused") void _free() {
    accumulator.freeRef();
    data.freeRef();
  }

  public @Override @SuppressWarnings("unused") Result addRef() {
    return (Result) super.addRef();
  }

  public static abstract class Accumulator extends ReferenceCountingBase
      implements BiConsumer<DeltaSet<UUID>, TensorList> {

    public static @SuppressWarnings("unused") Accumulator[] addRefs(Accumulator[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(Accumulator::addRef).toArray((x) -> new Accumulator[x]);
    }

    public @SuppressWarnings("unused") void _free() {
    }

    public @Override @SuppressWarnings("unused") Accumulator addRef() {
      return (Accumulator) super.addRef();
    }
  }
}
