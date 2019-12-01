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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;

public class DoubleBuffer<K> extends ReferenceCountingBase {
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DoubleBuffer.class);
  @Nonnull
  public final K key;
  public final double[] target;
  @Nullable
  protected volatile double[] delta;

  public DoubleBuffer(@Nonnull final K key, final double[] target) {
    this.key = key;
    if (key instanceof ReferenceCounting) ((ReferenceCounting) key).addRef();
    this.target = target;
    this.delta = null;
  }

  public DoubleBuffer(@Nonnull final K key, final double[] target, final double[] delta) {
    this.key = key;
    if (key instanceof ReferenceCounting) ((ReferenceCounting) key).addRef();
    this.target = target;
    this.delta = delta;
  }

  public static boolean areEqual(@Nonnull final double[] l, @Nonnull final double[] r) {
    if (r.length != l.length) throw new IllegalArgumentException();
    for (int i = 0; i < r.length; i++) {
      if (r[i] != l[i]) return false;
    }
    return true;
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
      throw new IllegalArgumentException(String.format("Deltas are not based on same buffer. %s != %s", this.key, right.key));
    }
    if (!this.key.equals(right.key)) {
      throw new IllegalArgumentException(String.format("Deltas are not based on same key. %s != %s", this.key, right.key));
    }
    @Nullable final double[] l = this.getDelta();
    @Nullable final double[] r = right.getDelta();
    assert l.length == r.length;
    final double[] array = IntStream.range(0, l.length).mapToDouble(i -> l[i] * r[i]).toArray();
    return Arrays.stream(array).summaryStatistics().getSum();
  }

  @Nullable
  public double[] getDeltaAndFree() {
    double[] delta = getDelta();
    freeRef();
    return delta;
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

  public int length() {
    return this.target.length;
  }

  @Nonnull
  public DoubleBuffer<K> map(@Nonnull final DoubleUnaryOperator mapper) {
    return new DoubleBuffer<K>(this.key, this.target, Arrays.stream(this.getDelta()).map(x -> mapper.applyAsDouble(x)).toArray());
  }

  @Nonnull
  public DoubleBuffer<K> set(@Nonnull final double[] data) {
    assert Arrays.stream(data).allMatch(Double::isFinite);
    System.arraycopy(data, 0, this.getDelta(), 0, data.length);
//    Arrays.parallelSetAll(this.getDelta(), i -> data[i]);
    assert Arrays.stream(getDelta()).allMatch(Double::isFinite);
    return this;
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

  @Override
  protected void _free() {
    if (key instanceof ReferenceCounting) ((ReferenceCounting) key).freeRef();
    @Nullable double[] delta = this.delta;
    if (null != delta) {
      if (RecycleBin.DOUBLES.want(delta.length)) {
        RecycleBin.DOUBLES.recycle(this.delta, delta.length);
      }
      this.delta = null;
    }
  }
}
