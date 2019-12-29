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

import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;

import javax.annotation.Nonnull;
import java.util.UUID;

public class SimpleLineSearchCursor extends LineSearchCursorBase {
  public final DeltaSet<UUID> direction;
  @Nonnull
  public final PointSample origin;
  public final Trainable subject;
  private String type = "";

  public SimpleLineSearchCursor(final Trainable subject, @Nonnull final PointSample origin,
                                final DeltaSet<UUID> direction) {
    this.origin = origin.copyFull();
    this.direction = direction;
    this.subject = subject;
  }

  @Override
  public CharSequence getDirectionType() {
    return type;
  }

  @Nonnull
  public SimpleLineSearchCursor setDirectionType(final String type) {
    this.type = type;
    return this;
  }

  @Nonnull
  @Override
  public DeltaSet<UUID> position(final double alpha) {
    return direction.scale(alpha);
  }

  @Override
  public void reset() {
    origin.restore();
  }

  @Override
  public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
    if (!Double.isFinite(alpha))
      throw new IllegalArgumentException();
    reset();
    if (0.0 != alpha) {
      direction.accumulate(alpha);
    }
    @Nonnull final PointSample sample = afterStep(subject.measure(monitor).setRate(alpha));
    final double dot = direction.dot(sample.delta);
    return new LineSearchPoint(sample, dot);
  }

  @Override
  protected void _free() {
  }
}
