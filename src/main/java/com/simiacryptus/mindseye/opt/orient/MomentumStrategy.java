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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * The MomentumStrategy class represents a strategy for carrying over momentum from one iteration to the next.
 * The class contains an inner OrientationStrategy object, as well as a DeltaSet object representing the
 * previous delta. The carryOver variable represents the amount of momentum that is carried over from one
 * iteration to the next.*
 *
 * @docgenVersion 9
 */
public class MomentumStrategy extends OrientationStrategyBase<SimpleLineSearchCursor> {

  /**
   * The Inner.
   */
  @Nullable
  public final OrientationStrategy<SimpleLineSearchCursor> inner;
  /**
   * The Prev delta.
   */
  @Nonnull
  DeltaSet<UUID> prevDelta = new DeltaSet<UUID>();
  private double carryOver = 0.1;

  /**
   * Instantiates a new Momentum strategy.
   *
   * @param inner the inner
   */
  public MomentumStrategy(@Nullable final OrientationStrategy<SimpleLineSearchCursor> inner) {
    OrientationStrategy<SimpleLineSearchCursor> temp_05_0001 = inner == null ? null : inner.addRef();
    this.inner = temp_05_0001 == null ? null : temp_05_0001.addRef();
    if (null != temp_05_0001)
      temp_05_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  /**
   * Returns the carry over value.
   *
   * @docgenVersion 9
   */
  public double getCarryOver() {
    return carryOver;
  }

  /**
   * Sets the carry over value.
   *
   * @param carryOver the new carry over value
   * @docgenVersion 9
   */
  public void setCarryOver(double carryOver) {
    this.carryOver = carryOver;
  }

  /**
   * @param subject     The subject to orient.
   * @param measurement The measurement to use.
   * @param monitor     The training monitor.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public SimpleLineSearchCursor orient(@Nullable final Trainable subject, @Nonnull final PointSample measurement,
                                       final TrainingMonitor monitor) {
    assert inner != null;
    final LineSearchCursor orient = inner.orient(subject == null ? null : subject.addRef(),
        measurement.addRef(), monitor);
    assert ((SimpleLineSearchCursor) orient).direction != null;
    final DeltaSet<UUID> direction = ((SimpleLineSearchCursor) orient).direction.addRef();
    orient.freeRef();
    @Nonnull final DeltaSet<UUID> newDelta = new DeltaSet<UUID>();
    RefMap<UUID, Delta<UUID>> temp_05_0004 = direction.getMap();
    temp_05_0004.forEach(RefUtil.wrapInterface((BiConsumer<? super UUID, ? super Delta<UUID>>) (layer, delta) -> {
      final DoubleBuffer<UUID> prevBuffer = prevDelta.get(layer, delta.target);
      Delta<UUID> temp_05_0005 = newDelta.get(layer, delta.target);
      assert prevBuffer != null;
      assert temp_05_0005 != null;
      temp_05_0005.addInPlace(ArrayUtil.add(ArrayUtil.multiply(prevBuffer.getDelta(), carryOver), delta.getDelta()));
      temp_05_0005.freeRef();
      prevBuffer.freeRef();
      delta.freeRef();
    }, newDelta.addRef()));
    temp_05_0004.freeRef();
    direction.freeRef();
    prevDelta.freeRef();
    prevDelta = newDelta.addRef();
    return new SimpleLineSearchCursor(subject, measurement, newDelta);
  }

  /**
   * Resets the inner object.
   *
   * @docgenVersion 9
   */
  @Override
  public void reset() {
    assert inner != null;
    inner.reset();
  }

  /**
   * Frees this object and all resources associated with it.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    prevDelta.freeRef();
    prevDelta = null;
    if (null != inner)
      inner.freeRef();
  }

  /**
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  MomentumStrategy addRef() {
    return (MomentumStrategy) super.addRef();
  }
}
