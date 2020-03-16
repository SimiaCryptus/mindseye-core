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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.DoubleUnaryOperator;

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
    this.target = target;
    this.delta = null;
  }

  public DoubleBuffer(@Nonnull final K key, final double[] target,
                      @Nullable final double[] delta) {
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
      IllegalArgumentException temp_51_0001 = new IllegalArgumentException(
          RefString.format("Deltas are not based on same buffer. %s != %s", this.key, right.key));
      right.freeRef();
      throw temp_51_0001;
    }
    if (!this.key.equals(right.key)) {
      IllegalArgumentException temp_51_0002 = new IllegalArgumentException(
          RefString.format("Deltas are not based on same key. %s != %s", this.key, right.key));
      right.freeRef();
      throw temp_51_0002;
    }
    @Nullable final double[] l = this.getDelta();
    @Nullable final double[] r = right.getDelta();
    right.freeRef();
    assert r != null;
    assert l != null;
    assert l.length == r.length;
    final double[] array = RefIntStream.range(0, l.length).parallel().mapToDouble(i -> l[i] * r[i]).toArray();
    return RefArrays.stream(array).parallel().reduce((a, b) -> a + b).getAsDouble();
  }

  public int length() {
    return this.target.length;
  }

  @Nonnull
  public DoubleBuffer<K> map(@Nonnull final DoubleUnaryOperator mapper) {
    return map(mapper, !CoreSettings.INSTANCE().singleThreaded);
  }

  @Nonnull
  public DoubleBuffer<K> map(@Nonnull final DoubleUnaryOperator mapper, boolean parallel) {
    RefDoubleStream stream = RefArrays.stream(this.getDelta());
    if (parallel) stream = stream.parallel();
    return new DoubleBuffer<K>(this.key, this.target,
        stream.map(mapper::applyAsDouble).toArray());
  }

  public void set(@Nonnull final double[] data) {
    assert RefArrays.stream(data).parallel().allMatch(Double::isFinite);
    RefSystem.arraycopy(data, 0, this.getDelta(), 0, data.length);
    //    Arrays.parallelSetAll(this.getDelta(), i -> data[i]);
    assert RefArrays.stream(getDelta()).parallel().allMatch(Double::isFinite);
  }

  @Nonnull
  public DoubleArrayStatsFacade targetStatistics() {
    return new DoubleArrayStatsFacade(target);
  }

  @Nonnull
  @Override
  public String toString() {
    @Nonnull final RefStringBuilder builder = new RefStringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append("/");
    builder.append(this.key);
    return builder.toString();
  }

  public void _free() {
    super._free();
    @Nullable
    double[] delta = this.delta;
    if (null != delta) {
      if (RecycleBin.DOUBLES.want(delta.length)) {
        RecycleBin.DOUBLES.recycle(this.delta, delta.length);
      }
      this.delta = null;
    }
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  DoubleBuffer<K> addRef() {
    return (DoubleBuffer<K>) super.addRef();
  }
}
