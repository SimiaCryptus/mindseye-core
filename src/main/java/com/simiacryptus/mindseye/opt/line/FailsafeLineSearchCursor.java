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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

public class FailsafeLineSearchCursor extends LineSearchCursorBase {
  @Nullable
  private final LineSearchCursor direction;
  private final TrainingMonitor monitor;
  @Nullable
  private PointSample best;

  public FailsafeLineSearchCursor(@Nullable final LineSearchCursor direction, @Nonnull final PointSample previousPoint,
                                  final TrainingMonitor monitor) {
    LineSearchCursor temp_11_0001 = direction == null ? null : direction.addRef();
    this.direction = temp_11_0001 == null ? null : temp_11_0001.addRef();
    if (null != temp_11_0001)
      temp_11_0001.freeRef();
    if (null != direction)
      direction.freeRef();
    PointSample temp_11_0002 = previousPoint.copyFull();
    best = temp_11_0002.addRef();
    temp_11_0002.freeRef();
    previousPoint.freeRef();
    this.monitor = monitor;
  }

  @Override
  public CharSequence getDirectionType() {
    assert direction != null;
    return direction.getDirectionType();
  }

  @Nonnull
  @Override
  public synchronized PointSample afterStep(@Nonnull final PointSample step) {
    super.afterStep(step.addRef());
    assert direction != null;
    RefUtil.freeRef(direction.afterStep(step.addRef()));
    if (null == best || best.getMean() > step.getMean()) {
      @Nonnull
      PointSample newValue = step.copyFull();
      if (null != this.best) {
        monitor.log(RefString.format("New Minimum: %s > %s", best.getMean(), step.getMean()));
      }
      PointSample temp_11_0003 = newValue.addRef();
      if (null != this.best)
        this.best.freeRef();
      this.best = temp_11_0003.addRef();
      temp_11_0003.freeRef();
      newValue.freeRef();
    }
    return step;
  }

  @Nullable
  public PointSample getBest(final TrainingMonitor monitor) {
    if (null != this.best) {
      best.weights.restore();
    }
    return best == null ? null : best.addRef();
  }

  @Override
  public DeltaSet<UUID> position(final double alpha) {
    assert direction != null;
    return direction.position(alpha);
  }

  @Override
  public void reset() {
    assert direction != null;
    direction.reset();
  }

  @javax.annotation.Nullable
  @Override
  public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
    assert direction != null;
    final LineSearchPoint step = direction.step(alpha, monitor);
    assert step != null;
    assert step.point != null;
    RefUtil.freeRef(afterStep(step.point.addRef()));
    return step;
  }

  @Override
  public void _free() {
    if (null != direction)
      direction.freeRef();
    if (null != best) {
      best.freeRef();
      best = null;
    }
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  FailsafeLineSearchCursor addRef() {
    return (FailsafeLineSearchCursor) super.addRef();
  }

}
