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
import java.util.Arrays;

public class TrainableWrapper<T extends Trainable> extends ReferenceCountingBase implements TrainableDataMask {

  @Nullable
  private final T inner;

  public TrainableWrapper(@Nullable final T inner) {
    T temp_21_0001 = RefUtil.addRef(inner);
    this.inner = RefUtil.addRef(temp_21_0001);
    if (null != temp_21_0001)
      temp_21_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  @Nullable
  public T getInner() {
    return RefUtil.addRef(inner);
  }

  @Nonnull
  @Override
  public Layer getLayer() {
    assert inner != null;
    return inner.getLayer();
  }

  @Override
  public boolean[] getMask() {
    assert inner != null;
    return ((TrainableDataMask) inner).getMask();
  }

  @Nullable
  public static @SuppressWarnings("unused")
  TrainableWrapper[] addRefs(@Nullable TrainableWrapper[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TrainableWrapper::addRef)
        .toArray((x) -> new TrainableWrapper[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  TrainableWrapper[][] addRefs(@Nullable TrainableWrapper[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TrainableWrapper::addRefs)
        .toArray((x) -> new TrainableWrapper[x][]);
  }

  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    assert inner != null;
    return inner.measure(monitor);
  }

  @Override
  public boolean reseed(final long seed) {
    T temp_21_0003 = getInner();
    assert temp_21_0003 != null;
    boolean temp_21_0002 = temp_21_0003.reseed(seed);
    temp_21_0003.freeRef();
    return temp_21_0002;
  }

  @Nonnull
  @Override
  public TrainableDataMask setMask(final boolean... mask) {
    assert inner != null;
    RefUtil.freeRef(((TrainableDataMask) inner).setMask(mask));
    return this.addRef();
  }

  @Nonnull
  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "heapCopy=" + inner + '}';
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  TrainableWrapper<T> addRef() {
    return (TrainableWrapper<T>) super.addRef();
  }
}
