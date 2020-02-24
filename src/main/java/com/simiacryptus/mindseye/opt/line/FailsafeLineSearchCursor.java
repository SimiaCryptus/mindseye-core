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
import java.util.UUID;

public class FailsafeLineSearchCursor extends LineSearchCursorBase {
  @Nullable
  private final LineSearchCursor direction;
  private final TrainingMonitor monitor;
  @Nullable
  private PointSample best;

  public FailsafeLineSearchCursor(@Nullable final LineSearchCursor direction, @Nonnull final PointSample previousPoint,
                                  final TrainingMonitor monitor) {
    this.direction = direction;
    best = previousPoint.copyFull();
    previousPoint.freeRef();
    this.monitor = monitor;
  }

  @Nullable
  public PointSample getBest() {
    if (best == null) return null;
    best.weights.restore();
    return best.addRef();
  }

  @Override
  public CharSequence getDirectionType() {
    assert direction != null;
    return direction.getDirectionType();
  }

  @Override
  public synchronized PointSample afterStep(final PointSample step) {
    if (null == step) return null;
    assert direction != null;
    try {
      RefUtil.freeRef(direction.afterStep(step.addRef()));
      if (null == best || best.getMean() > step.getMean()) {
        @Nonnull
        PointSample newValue = step.copyFull();
        if (null != this.best) {
          monitor.log(RefString.format("New Minimum: %s > %s", best.getMean(), step.getMean()));
          this.best.freeRef();
        }
        this.best = newValue;
      }
      return step.addRef();
    } finally {
      step.freeRef();
    }
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
    try {
      RefUtil.freeRef(afterStep(step.getPoint()));
      return step.addRef();
    } finally {
      step.freeRef();
    }
  }

  @Override
  public void _free() {
    super._free();
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
