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

package com.simiacryptus.mindseye.opt.line;

import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

public class FailsafeLineSearchCursor extends LineSearchCursorBase {
  private final LineSearchCursor direction;
  private final TrainingMonitor monitor;
  private PointSample best;

  public FailsafeLineSearchCursor(final LineSearchCursor direction, @Nonnull final PointSample previousPoint,
      final TrainingMonitor monitor) {
    LineSearchCursor temp_11_0001 = direction == null ? null : direction.addRef();
    this.direction = temp_11_0001 == null ? null : temp_11_0001.addRef();
    if (null != temp_11_0001)
      temp_11_0001.freeRef();
    if (null != direction)
      direction.freeRef();
    PointSample temp_11_0002 = previousPoint.copyFull();
    if (null != best)
      best.freeRef();
    best = temp_11_0002 == null ? null : temp_11_0002.addRef();
    if (null != temp_11_0002)
      temp_11_0002.freeRef();
    previousPoint.freeRef();
    this.monitor = monitor;
  }

  @Override
  public CharSequence getDirectionType() {
    return direction.getDirectionType();
  }

  public static @SuppressWarnings("unused") FailsafeLineSearchCursor[] addRefs(FailsafeLineSearchCursor[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(FailsafeLineSearchCursor::addRef)
        .toArray((x) -> new FailsafeLineSearchCursor[x]);
  }

  public static @SuppressWarnings("unused") FailsafeLineSearchCursor[][] addRefs(FailsafeLineSearchCursor[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(FailsafeLineSearchCursor::addRefs)
        .toArray((x) -> new FailsafeLineSearchCursor[x][]);
  }

  @Override
  public synchronized PointSample afterStep(@Nonnull final PointSample step) {
    super.afterStep(step.addRef());
    RefUtil.freeRef(direction.afterStep(step == null ? null : step.addRef()));
    if (null == best || best.getMean() > step.getMean()) {
      @Nonnull
      PointSample newValue = step.copyFull();
      if (null != this.best) {
        monitor.log(RefString.format("New Minimum: %s > %s", best.getMean(), step.getMean()));
      }
      PointSample temp_11_0003 = newValue == null ? null : newValue.addRef();
      if (null != this.best)
        this.best.freeRef();
      this.best = temp_11_0003 == null ? null : temp_11_0003.addRef();
      if (null != temp_11_0003)
        temp_11_0003.freeRef();
      newValue.freeRef();
    }
    return step;
  }

  public PointSample getBest(final TrainingMonitor monitor) {
    if (null != this.best) {
      best.weights.restore();
    }
    return best == null ? null : best.addRef();
  }

  @Override
  public DeltaSet<UUID> position(final double alpha) {
    return direction.position(alpha);
  }

  @Override
  public void reset() {
    direction.reset();
  }

  @Override
  public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
    final LineSearchPoint step = direction.step(alpha, monitor);
    RefUtil.freeRef(afterStep(step.point.addRef()));
    return step;
  }

  @Override
  public void _free() {
    if (null != best)
      best.freeRef();
    best = null;
    if (null != direction)
      direction.freeRef();
    if (null != this.best) {
      PointSample temp_11_0004 = null;
      if (null != this.best)
        this.best.freeRef();
      this.best = temp_11_0004 == null ? null : temp_11_0004.addRef();
      if (null != temp_11_0004)
        temp_11_0004.freeRef();
    }
  }

  public @Override @SuppressWarnings("unused") FailsafeLineSearchCursor addRef() {
    return (FailsafeLineSearchCursor) super.addRef();
  }

}
