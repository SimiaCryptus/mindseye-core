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
import com.simiacryptus.ref.lang.RefUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

public class SimpleLineSearchCursor extends LineSearchCursorBase {
  @Nullable
  public final DeltaSet<UUID> direction;
  @Nonnull
  public final PointSample origin;
  @Nullable
  public final Trainable subject;
  private String type = "";

  public SimpleLineSearchCursor(@Nullable final Trainable subject, @Nonnull final PointSample origin,
                                @Nullable final DeltaSet<UUID> direction) {
    PointSample temp_25_0001 = origin.copyFull();
    this.origin = temp_25_0001.addRef();
    temp_25_0001.freeRef();
    origin.freeRef();
    DeltaSet<UUID> temp_25_0002 = direction == null ? null : direction.addRef();
    this.direction = temp_25_0002 == null ? null : temp_25_0002.addRef();
    if (null != temp_25_0002)
      temp_25_0002.freeRef();
    if (null != direction)
      direction.freeRef();
    Trainable temp_25_0003 = subject == null ? null : subject.addRef();
    this.subject = temp_25_0003 == null ? null : temp_25_0003.addRef();
    if (null != temp_25_0003)
      temp_25_0003.freeRef();
    if (null != subject)
      subject.freeRef();
  }

  @Override
  public CharSequence getDirectionType() {
    return type;
  }

  @Nonnull
  public SimpleLineSearchCursor setDirectionType(final String type) {
    this.type = type;
    return this.addRef();
  }

  @Nullable
  public static @SuppressWarnings("unused")
  SimpleLineSearchCursor[] addRefs(@Nullable SimpleLineSearchCursor[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SimpleLineSearchCursor::addRef)
        .toArray((x) -> new SimpleLineSearchCursor[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  SimpleLineSearchCursor[][] addRefs(@Nullable SimpleLineSearchCursor[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SimpleLineSearchCursor::addRefs)
        .toArray((x) -> new SimpleLineSearchCursor[x][]);
  }

  @Nonnull
  @Override
  public DeltaSet<UUID> position(final double alpha) {
    assert direction != null;
    return direction.scale(alpha);
  }

  @Override
  public void reset() {
    RefUtil.freeRef(origin.restore());
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
    PointSample temp_25_0005 = subject.measure(monitor);
    @Nonnull final PointSample sample = afterStep(temp_25_0005.setRate(alpha));
    temp_25_0005.freeRef();
    assert direction != null;
    final double dot = direction.dot(sample.delta.addRef());
    LineSearchPoint temp_25_0004 = new LineSearchPoint(sample, dot);
    return temp_25_0004;
  }

  public void _free() {
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
