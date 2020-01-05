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
import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.DoubleBuffer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.util.ArrayUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;

public @RefAware
class MomentumStrategy extends OrientationStrategyBase<SimpleLineSearchCursor> {

  public final OrientationStrategy<SimpleLineSearchCursor> inner;
  @Nonnull
  DeltaSet<UUID> prevDelta = new DeltaSet<UUID>();
  private double carryOver = 0.1;

  public MomentumStrategy(final OrientationStrategy<SimpleLineSearchCursor> inner) {
    {
      OrientationStrategy<SimpleLineSearchCursor> temp_05_0001 = inner == null
          ? null
          : inner.addRef();
      this.inner = temp_05_0001 == null ? null : temp_05_0001.addRef();
      if (null != temp_05_0001)
        temp_05_0001.freeRef();
    }
    if (null != inner)
      inner.freeRef();
  }

  public double getCarryOver() {
    return carryOver;
  }

  @Nonnull
  public MomentumStrategy setCarryOver(final double carryOver) {
    this.carryOver = carryOver;
    return this.addRef();
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
    final LineSearchCursor orient = inner.orient(subject == null ? null : subject.addRef(),
        measurement == null ? null : measurement.addRef(), monitor);
    final DeltaSet<UUID> direction = ((SimpleLineSearchCursor) orient).direction.addRef();
    if (null != orient)
      orient.freeRef();
    @Nonnull final DeltaSet<UUID> newDelta = new DeltaSet<UUID>();
    RefMap<UUID, Delta<UUID>> temp_05_0004 = direction
        .getMap();
    temp_05_0004.forEach(RefUtil.wrapInterface(
        (BiConsumer<? super UUID, ? super Delta<UUID>>) (
            layer, delta) -> {
          final DoubleBuffer<UUID> prevBuffer = prevDelta.get(layer, delta.target);
          Delta<UUID> temp_05_0005 = newDelta.get(layer, delta.target);
          RefUtil.freeRef(temp_05_0005
              .addInPlace(ArrayUtil.add(ArrayUtil.multiply(prevBuffer.getDelta(), carryOver), delta.getDelta())));
          if (null != temp_05_0005)
            temp_05_0005.freeRef();
          if (null != prevBuffer)
            prevBuffer.freeRef();
          if (null != delta)
            delta.freeRef();
        }, newDelta == null ? null : newDelta.addRef()));
    if (null != temp_05_0004)
      temp_05_0004.freeRef();
    if (null != direction)
      direction.freeRef();
    {
      DeltaSet<UUID> temp_05_0002 = newDelta == null ? null
          : newDelta.addRef();
      if (null != prevDelta)
        prevDelta.freeRef();
      prevDelta = temp_05_0002 == null ? null : temp_05_0002.addRef();
      if (null != temp_05_0002)
        temp_05_0002.freeRef();
    }
    SimpleLineSearchCursor temp_05_0003 = new SimpleLineSearchCursor(
        subject == null ? null : subject.addRef(), measurement == null ? null : measurement,
        newDelta == null ? null : newDelta);
    if (null != subject)
      subject.freeRef();
    return temp_05_0003;
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
    if (null != prevDelta)
      prevDelta.freeRef();
    prevDelta = null;
    if (null != inner)
      inner.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  MomentumStrategy addRef() {
    return (MomentumStrategy) super.addRef();
  }
}
