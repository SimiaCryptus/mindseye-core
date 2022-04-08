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

/**
 * This class represents a double buffer.
 *
 * @param key    the key
 * @param target the target
 * @param delta  the delta
 * @docgenVersion 9
 */
public class DoubleBuffer<K> extends ReferenceCountingBase {
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DoubleBuffer.class);
  /**
   * The Key.
   */
  @Nonnull
  public final K key;
  /**
   * The Target.
   */
  public final double[] target;
  /**
   * The Delta.
   */
  @Nullable
  protected volatile double[] delta;

  /**
   * Instantiates a new Double buffer.
   *
   * @param key    the key
   * @param target the target
   */
  public DoubleBuffer(@Nonnull final K key, final double[] target) {
    this.key = key;
    this.target = target;
    this.delta = null;
  }

  /**
   * Instantiates a new Double buffer.
   *
   * @param key    the key
   * @param target the target
   * @param delta  the delta
   */
  public DoubleBuffer(@Nonnull final K key, final double[] target,
                      @Nullable final double[] delta) {
    this.key = key;
    this.target = target;
    this.delta = delta;
  }

  /**
   * Returns the delta array, or null if it does not exist.
   * If the delta array does not exist, this method will create it.
   *
   * @docgenVersion 9
   */
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

  /**
   * Returns the id of this object as a CharSequence.
   *
   * @docgenVersion 9
   */
  public CharSequence getId() {
    return this.key.toString();
  }

  /**
   * Returns true if the two arrays are equal, false otherwise.
   * Throws an IllegalArgumentException if the arrays are not the same length.
   *
   * @docgenVersion 9
   */
  public static boolean areEqual(@Nonnull final double[] l, @Nonnull final double[] r) {
    if (r.length != l.length)
      throw new IllegalArgumentException();
    for (int i = 0; i < r.length; i++) {
      if (r[i] != l[i])
        return false;
    }
    return true;
  }

  /**
   * Returns a new DoubleBuffer with the same contents as this one.
   * The new buffer's capacity will be the number of elements in this buffer.
   *
   * @return a new DoubleBuffer with the same contents as this one
   * @throws NullPointerException if this buffer is null
   * @docgenVersion 9
   */
  @Nullable
  public DoubleBuffer<K> copy() {
    assertAlive();
    return new DoubleBuffer<K>(key, target, RecycleBin.DOUBLES.copyOf(delta, length()));
  }

  /**
   * Returns a new DoubleArrayStatsFacade object containing statistics for the delta array.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public DoubleArrayStatsFacade deltaStatistics() {
    return new DoubleArrayStatsFacade(getDelta());
  }

  /**
   * Calculates the dot product of this vector with the given vector.
   *
   * @param right the other vector
   * @return the dot product
   * @docgenVersion 9
   */
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

  /**
   * Returns the length of the target String.
   *
   * @docgenVersion 9
   */
  public int length() {
    return this.target.length;
  }

  /**
   * Maps the given DoubleUnaryOperator onto the elements of this buffer.
   *
   * @param mapper the operator to map with
   * @return a new DoubleBuffer with the mapped elements
   * @docgenVersion 9
   */
  @Nonnull
  public DoubleBuffer<K> map(@Nonnull final DoubleUnaryOperator mapper) {
    return map(mapper, !CoreSettings.INSTANCE().singleThreaded);
  }

  /**
   * Maps the given mapper function to this DoubleBuffer, optionally in parallel.
   *
   * @param mapper   the mapper function to apply
   * @param parallel whether or not to run in parallel
   * @return a new DoubleBuffer with the mapped values
   * @docgenVersion 9
   */
  @Nonnull
  public DoubleBuffer<K> map(@Nonnull final DoubleUnaryOperator mapper, boolean parallel) {
    RefDoubleStream stream = RefArrays.stream(this.getDelta());
    if (parallel) stream = stream.parallel();
    return new DoubleBuffer<K>(this.key, this.target,
        stream.map(mapper::applyAsDouble).toArray());
  }

  /**
   * Sets the data for this object.
   *
   * @param data the data to set
   * @docgenVersion 9
   */
  public void set(@Nonnull final double[] data) {
    assert RefArrays.stream(data).parallel().allMatch(Double::isFinite);
    RefSystem.arraycopy(data, 0, this.getDelta(), 0, data.length);
    //    Arrays.parallelSetAll(this.getDelta(), i -> data[i]);
    assert RefArrays.stream(getDelta()).parallel().allMatch(Double::isFinite);
  }

  /**
   * Returns a new DoubleArrayStatsFacade object containing statistics about the target array.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public DoubleArrayStatsFacade targetStatistics() {
    return new DoubleArrayStatsFacade(target);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    @Nonnull final RefStringBuilder builder = new RefStringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append("/");
    builder.append(this.key);
    return builder.toString();
  }

  /**
   * Frees this object.
   *
   * @docgenVersion 9
   */
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

  /**
   * @return a new DoubleBuffer with a reference added
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  DoubleBuffer<K> addRef() {
    return (DoubleBuffer<K>) super.addRef();
  }
}
