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
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefDoubleStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.DoubleUnaryOperator;

/**
 * The Delta compensation.
 *
 * @docgenVersion 9
 */
public class Delta<K> extends DoubleBuffer<K> {
  /**
   * The Delta compensation.
   */
  @Nullable
  protected double[] deltaCompensation;

  /**
   * Instantiates a new Delta.
   *
   * @param layer  the layer
   * @param target the target
   */
  public Delta(@Nonnull final K layer, @Nullable final double[] target) {
    this(layer, target, null == target ? null : RecycleBin.DOUBLES.obtain(target.length));
  }

  /**
   * Instantiates a new Delta.
   *
   * @param layer  the layer
   * @param target the target
   * @param delta  the delta
   */
  public Delta(@Nonnull final K layer, final double[] target, @Nonnull final double[] delta) {
    this(layer, target, delta, RecycleBin.DOUBLES.obtain(delta.length));
  }

  /**
   * Instantiates a new Delta.
   *
   * @param layer             the layer
   * @param target            the target
   * @param delta             the delta
   * @param deltaCompensation the delta compensation
   */
  protected Delta(@Nonnull final K layer, @Nullable final double[] target, @Nullable final double[] delta,
                  @Nullable final double[] deltaCompensation) {
    super(layer, target, delta);
    if (null == target)
      throw new IllegalArgumentException();
    assert null == delta || target.length == delta.length;
    //if(null == array) throw new IllegalArgumentException();
    this.deltaCompensation = deltaCompensation;
  }

  /**
   * @param data             the data to accumulate
   * @param delta            the delta to accumulate
   * @param dataCompensation the data compensation to accumulate
   * @docgenVersion 9
   */
  public static void accumulate(@Nonnull final double[] data, final double[] delta,
                                @Nullable final double[] dataCompensation) {
    synchronized (data) {
      for (int i = 0; i < data.length; i++) {
        final double sum = data[i];
        final double input = delta[i];
        double c = null == dataCompensation ? 0 : dataCompensation[i];
        if (Math.abs(sum) >= Math.abs(input)) {
          final double y = sum - c;
          final double t = input + y;
          c = t - input - y;
          data[i] = t;
          if (null != dataCompensation) {
            dataCompensation[i] = c;
          }
        } else {
          final double y = input - c;
          final double t = sum + y;
          c = t - sum - y;
          data[i] = t;
          if (null != dataCompensation) {
            dataCompensation[i] = c;
          }
        }
        if (!Double.isFinite(data[i]))
          data[i] = 0;
      }
    }
  }


  /**
   * Accumulates the given factor into this instance.
   *
   * @param factor the factor to accumulate
   * @docgenVersion 9
   */
  public final void accumulate(final double factor) {
    synchronized (target) {
      //assert RefArrays.stream(target).parallel().allMatch(Double::isFinite);
      @Nullable final double[] delta = getDelta();
      for (int i = 0; i < length(); i++) {
        assert delta != null;
        target[i] += delta[i] * factor;
        if (!Double.isFinite(target[i]))
          target[i] = 0;
      }
      assert RefArrays.stream(target).parallel().allMatch(Double::isFinite);
    }
  }

  /**
   * Adds the given {@link Delta} to this {@link Delta} in place.
   *
   * @param buffer the {@link Delta} to add
   * @docgenVersion 9
   */
  public void addInPlace(@Nonnull final Delta<K> buffer) {
    assertAlive();
    addInPlace(buffer.delta);
    assert buffer.deltaCompensation != null;
    this.addInPlace(buffer.deltaCompensation);
    buffer.freeRef();
  }

  /**
   * Adds the given tensor in place.
   *
   * @param tensor the tensor to add
   * @docgenVersion 9
   */
  public void addInPlace(@Nonnull Tensor tensor) {
    addInPlace(tensor.getData());
    tensor.freeRef();
  }

  /**
   * Adds the given array in place.
   *
   * @param data the array to add
   * @throws IllegalArgumentException if the array is not the same length as the target array
   * @docgenVersion 9
   */
  public void addInPlace(@Nonnull double[] data) {
    assert data.length == this.target.length;
    //assert Arrays.stream(data).allMatch(Double::isFinite);
    Delta.accumulate(getDelta(), data, deltaCompensation);
    //assert Arrays.stream(read()).allMatch(Double::isFinite);
  }

  /**
   * @return a copy of this Delta object
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Delta<K> copy() {
    assertAlive();
    return new Delta<K>(key, target, RecycleBin.DOUBLES.copyOf(delta, length()),
        RecycleBin.DOUBLES.copyOf(deltaCompensation, length()));
  }

  /**
   * @param mapper the mapper function to apply to this {@code Delta}
   * @return the new {@code Delta}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Delta<K> map(@Nonnull final DoubleUnaryOperator mapper) {
    return map(mapper, !CoreSettings.INSTANCE().singleThreaded);
  }

  /**
   * Maps the delta using the given mapper function.
   *
   * @param mapper   the mapper function
   * @param parallel whether or not to run in parallel
   * @return the mapped delta
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Delta<K> map(@Nonnull final DoubleUnaryOperator mapper, boolean parallel) {
    RefDoubleStream stream = RefArrays.stream(getDelta());
    if (parallel) stream = stream.parallel();
    return new Delta<K>(key, target, stream.map(mapper).toArray());
  }

  /**
   * Scales the delta by a factor.
   *
   * @param f the factor to scale by
   * @return the scaled delta
   * @docgenVersion 9
   */
  @Nonnull
  public Delta<K> scale(final double f) {
    return map(x -> x * f);
  }

  /**
   * @Override public void set(@Nonnull final double[] data) {
   * super.set(data);
   * }
   * @docgenVersion 9
   */
  @Override
  public void set(@Nonnull final double[] data) {
    super.set(data);
  }

  /**
   * Frees this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != deltaCompensation) {
      if (RecycleBin.DOUBLES.want(deltaCompensation.length)) {
        RecycleBin.DOUBLES.recycle(deltaCompensation, deltaCompensation.length);
      }
      deltaCompensation = null;
    }
  }

  /**
   * @return a Delta object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  Delta<K> addRef() {
    return (Delta<K>) super.addRef();
  }
}
