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

package com.simiacryptus.mindseye.eval;

import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class wraps an object of type T, allowing it to be trainable.
 * The object being wrapped must be non-null.
 *
 * @docgenVersion 9
 */
public class TrainableWrapper<T extends Trainable> extends ReferenceCountingBase implements TrainableDataMask {

  @Nullable
  private final T inner;

  /**
   * Instantiates a new Trainable wrapper.
   *
   * @param inner the inner
   */
  public TrainableWrapper(@Nullable final T inner) {
    T temp_21_0001 = RefUtil.addRef(inner);
    this.inner = RefUtil.addRef(temp_21_0001);
    if (null != temp_21_0001)
      temp_21_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  /**
   * @return the inner value, or null if it is not set
   * @docgenVersion 9
   */
  @Nullable
  public T getInner() {
    return RefUtil.addRef(inner);
  }

  /**
   * @return the layer
   * @docgenVersion 9
   */
  @Override
  public Layer getLayer() {
    assert inner != null;
    return inner.getLayer();
  }

  /**
   * @return the mask of the inner TrainableDataMask
   * @docgenVersion 9
   */
  @Override
  public boolean[] getMask() {
    assert inner != null;
    return ((TrainableDataMask) inner).getMask();
  }

  /**
   * Sets the mask for this TrainableDataMask.
   *
   * @param mask the mask to set
   * @docgenVersion 9
   */
  public void setMask(final boolean... mask) {
    assert inner != null;
    ((TrainableDataMask) inner).setMask(mask);
  }

  /**
   * Asserts that the inner field is not null and returns the result of
   * inner.measure(monitor).
   *
   * @docgenVersion 9
   */
  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    assert inner != null;
    return inner.measure(monitor);
  }

  /**
   * @Override public boolean reseed(final long seed) {
   * assert inner != null;
   * return inner.reseed(seed);
   * }
   * @docgenVersion 9
   */
  @Override
  public boolean reseed(final long seed) {
    assert inner != null;
    return inner.reseed(seed);
  }

  /**
   * @return a string representation of this object, including its heapCopy field
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "heapCopy=" + inner + '}';
  }

  /**
   * Frees this object and its inner object, if any.
   *
   * @docgenVersion 9
   */
  public void _free() {
    if (null != inner)
      inner.freeRef();
    super._free();
  }

  /**
   * @return a new TrainableWrapper with a reference added
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  TrainableWrapper<T> addRef() {
    return (TrainableWrapper<T>) super.addRef();
  }
}
