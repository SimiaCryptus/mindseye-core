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

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;

import javax.annotation.Nonnull;

public class TrainableWrapper<T extends Trainable> extends ReferenceCountingBase implements TrainableDataMask {

  private final T inner;

  public TrainableWrapper(final T inner) {
    this.inner = inner;
    this.inner.addRef();
  }

  @Override
  protected void _free() {
    this.inner.freeRef();
    super._free();
  }

  public T getInner() {
    return inner;
  }

  @Override
  public boolean[] getMask() {
    return ((TrainableDataMask) inner).getMask();
  }

  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    return inner.measure(monitor);
  }

  @Override
  public boolean reseed(final long seed) {
    return getInner().reseed(seed);
  }

  @Override
  public Layer getLayer() {
    return inner.getLayer();
  }

  @Nonnull
  @Override
  public TrainableDataMask setMask(final boolean... mask) {
    ((TrainableDataMask) inner).setMask(mask);
    return this;
  }

  @Nonnull
  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "heapCopy=" + inner +
        '}';
  }
}
