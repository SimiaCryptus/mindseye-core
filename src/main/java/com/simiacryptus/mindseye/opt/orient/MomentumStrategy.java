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

package com.simiacryptus.mindseye.opt.orient;

import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.DoubleBuffer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.util.ArrayUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

public @RefAware
class MomentumStrategy
    extends OrientationStrategyBase<SimpleLineSearchCursor> {

  public final OrientationStrategy<SimpleLineSearchCursor> inner;
  @Nonnull
  DeltaSet<UUID> prevDelta = new DeltaSet<UUID>();
  private double carryOver = 0.1;

  public MomentumStrategy(final OrientationStrategy<SimpleLineSearchCursor> inner) {
    this.inner = inner;
  }

  public double getCarryOver() {
    return carryOver;
  }

  @Nonnull
  public MomentumStrategy setCarryOver(final double carryOver) {
    this.carryOver = carryOver;
    return this;
  }

  public static @SuppressWarnings("unused")
  MomentumStrategy[] addRefs(MomentumStrategy[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MomentumStrategy::addRef)
        .toArray((x) -> new MomentumStrategy[x]);
  }

  public static @SuppressWarnings("unused")
  MomentumStrategy[][] addRefs(MomentumStrategy[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MomentumStrategy::addRefs)
        .toArray((x) -> new MomentumStrategy[x][]);
  }

  @Nonnull
  @Override
  public SimpleLineSearchCursor orient(final Trainable subject, @Nonnull final PointSample measurement,
                                       final TrainingMonitor monitor) {
    final LineSearchCursor orient = inner.orient(subject, measurement, monitor);
    final DeltaSet<UUID> direction = ((SimpleLineSearchCursor) orient).direction;
    @Nonnull final DeltaSet<UUID> newDelta = new DeltaSet<UUID>();
    direction.getMap().forEach((layer, delta) -> {
      final DoubleBuffer<UUID> prevBuffer = prevDelta.get(layer, delta.target);
      newDelta.get(layer, delta.target)
          .addInPlace(ArrayUtil.add(ArrayUtil.multiply(prevBuffer.getDelta(), carryOver), delta.getDelta()));
    });
    prevDelta = newDelta;
    return new SimpleLineSearchCursor(subject, measurement, newDelta);
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  MomentumStrategy addRef() {
    return (MomentumStrategy) super.addRef();
  }
}
