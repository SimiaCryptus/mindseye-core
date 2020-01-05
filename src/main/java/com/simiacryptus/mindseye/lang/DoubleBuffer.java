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

import com.simiacryptus.ref.lang.RecycleBin;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

public @RefAware
class DoubleBuffer<K> extends ReferenceCountingBase {
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DoubleBuffer.class);
  @Nonnull
  public final K key;
  public final double[] target;
  @Nullable
  protected volatile double[] delta;

  public DoubleBuffer(@Nonnull final K key, final double[] target) {
    this.key = key;
    this.target = target;
    this.delta = null;
  }

  public DoubleBuffer(@Nonnull final K key, final double[] target,
                      @org.jetbrains.annotations.Nullable final double[] delta) {
    this.key = key;
    this.target = target;
    this.delta = delta;
  }

  @Nullable
  public double[] getDelta() {
    assertAlive();
    if (null == delta) {
      synchronized (this) {
        if (null == delta) {
          delta = RecycleBin.DOUBLES.obtain(target.length);
        }
      }
    }
    return delta;
  }

  public CharSequence getId() {
    return this.key.toString();
  }

  public static boolean areEqual(@Nonnull final double[] l, @Nonnull final double[] r) {
    if (r.length != l.length)
      throw new IllegalArgumentException();
    for (int i = 0; i < r.length; i++) {
      if (r[i] != l[i])
        return false;
    }
    return true;
  }

  public static @SuppressWarnings("unused")
  DoubleBuffer[] addRefs(DoubleBuffer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DoubleBuffer::addRef)
        .toArray((x) -> new DoubleBuffer[x]);
  }

  public static @SuppressWarnings("unused")
  DoubleBuffer[][] addRefs(DoubleBuffer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DoubleBuffer::addRefs)
        .toArray((x) -> new DoubleBuffer[x][]);
  }

  @Nullable
  public DoubleBuffer<K> copy() {
    assertAlive();
    return new DoubleBuffer<K>(key, target, RecycleBin.DOUBLES.copyOf(delta, length()));
  }

  @Nonnull
  public DoubleArrayStatsFacade deltaStatistics() {
    return new DoubleArrayStatsFacade(getDelta());
  }

  public double dot(@Nonnull final DoubleBuffer<K> right) {
    if (this.target != right.target) {
      throw new IllegalArgumentException(
          String.format("Deltas are not based on same buffer. %s != %s", this.key, right.key));
    }
    if (!this.key.equals(right.key)) {
      throw new IllegalArgumentException(
          String.format("Deltas are not based on same key. %s != %s", this.key, right.key));
    }
    @Nullable final double[] l = this.getDelta();
    @Nullable final double[] r = right.getDelta();
    assert l.length == r.length;
    final double[] array = RefIntStream.range(0, l.length).mapToDouble(i -> l[i] * r[i])
        .toArray();
    return RefArrays.stream(array).summaryStatistics().getSum();
  }

  public int length() {
    return this.target.length;
  }

  @Nonnull
  public DoubleBuffer<K> map(@Nonnull final DoubleUnaryOperator mapper) {
    return new DoubleBuffer<K>(this.key, this.target,
        RefArrays.stream(this.getDelta()).map(x -> mapper.applyAsDouble(x)).toArray());
  }

  @Nonnull
  public void set(@Nonnull final double[] data) {
    assert RefArrays.stream(data).allMatch(Double::isFinite);
    System.arraycopy(data, 0, this.getDelta(), 0, data.length);
    //    Arrays.parallelSetAll(this.getDelta(), i -> data[i]);
    assert RefArrays.stream(getDelta()).allMatch(Double::isFinite);
  }

  @Nonnull
  public DoubleArrayStatsFacade targetStatistics() {
    return new DoubleArrayStatsFacade(target);
  }

  @Nonnull
  @Override
  public String toString() {
    @Nonnull final StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append("/");
    builder.append(this.key);
    return builder.toString();
  }

  public void _free() {
    @Nullable
    double[] delta = this.delta;
    if (null != delta) {
      if (RecycleBin.DOUBLES.want(delta.length)) {
        RecycleBin.DOUBLES.recycle(this.delta, delta.length);
      }
      this.delta = null;
    }
  }

  public @Override
  @SuppressWarnings("unused")
  DoubleBuffer<K> addRef() {
    return (DoubleBuffer<K>) super.addRef();
  }
}
