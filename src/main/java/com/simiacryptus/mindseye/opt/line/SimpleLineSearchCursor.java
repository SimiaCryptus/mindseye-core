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
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The type Simple line search cursor.
 */
public class SimpleLineSearchCursor extends LineSearchCursorBase {
  /**
   * The Direction.
   */
  @Nullable
  public final DeltaSet<UUID> direction;
  /**
   * The Origin.
   */
  @Nonnull
  public final PointSample origin;
  /**
   * The Subject.
   */
  @Nullable
  public final Trainable subject;
  private String type = "";

  /**
   * Instantiates a new Simple line search cursor.
   *
   * @param subject   the subject
   * @param origin    the origin
   * @param direction the direction
   */
  public SimpleLineSearchCursor(@Nullable final Trainable subject, @Nonnull final PointSample origin,
                                @Nullable final DeltaSet<UUID> direction) {
    this.direction = direction;
    this.subject = subject;
    this.origin = origin.copyFull();
    origin.freeRef();
  }

  @Override
  public CharSequence getDirectionType() {
    return type;
  }

  /**
   * Sets direction type.
   *
   * @param type the type
   */
  public void setDirectionType(String type) {
    this.type = type;
  }

  @Nonnull
  @Override
  public DeltaSet<UUID> position(final double alpha) {
    assert direction != null;
    return direction.scale(alpha);
  }

  @Override
  public void reset() {
    origin.restore();
  }

  @Nullable
  @Override
  public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
    if (!Double.isFinite(alpha))
      throw new IllegalArgumentException();
    reset();
    if (0.0 != alpha) {
      assert direction != null;
      direction.accumulate(alpha);
    }
    assert subject != null;
    PointSample pointSample = subject.measure(monitor);
    pointSample.setRate(alpha);
    @Nonnull final PointSample sample = afterStep(pointSample);
    assert direction != null;
    final double dot = direction.dot(sample.delta.addRef());
    return new LineSearchPoint(sample, dot);
  }

  public void _free() {
    super._free();
    if (null != subject)
      subject.freeRef();
    origin.freeRef();
    if (null != direction)
      direction.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SimpleLineSearchCursor addRef() {
    return (SimpleLineSearchCursor) super.addRef();
  }
}
