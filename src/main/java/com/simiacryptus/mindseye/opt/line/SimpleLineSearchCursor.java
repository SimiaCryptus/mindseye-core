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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

public @RefAware
class SimpleLineSearchCursor extends LineSearchCursorBase {
  public final DeltaSet<UUID> direction;
  @Nonnull
  public final PointSample origin;
  public final Trainable subject;
  private String type = "";

  public SimpleLineSearchCursor(final Trainable subject, @Nonnull final PointSample origin,
                                final DeltaSet<UUID> direction) {
    {
      PointSample temp_25_0001 = origin.copyFull();
      this.origin = temp_25_0001 == null ? null : temp_25_0001.addRef();
      if (null != temp_25_0001)
        temp_25_0001.freeRef();
    }
    origin.freeRef();
    {
      DeltaSet<UUID> temp_25_0002 = direction == null ? null
          : direction.addRef();
      this.direction = temp_25_0002 == null ? null : temp_25_0002.addRef();
      if (null != temp_25_0002)
        temp_25_0002.freeRef();
    }
    if (null != direction)
      direction.freeRef();
    {
      Trainable temp_25_0003 = subject == null ? null : subject.addRef();
      this.subject = temp_25_0003 == null ? null : temp_25_0003.addRef();
      if (null != temp_25_0003)
        temp_25_0003.freeRef();
    }
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

  public static @SuppressWarnings("unused")
  SimpleLineSearchCursor[] addRefs(SimpleLineSearchCursor[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SimpleLineSearchCursor::addRef)
        .toArray((x) -> new SimpleLineSearchCursor[x]);
  }

  public static @SuppressWarnings("unused")
  SimpleLineSearchCursor[][] addRefs(SimpleLineSearchCursor[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SimpleLineSearchCursor::addRefs)
        .toArray((x) -> new SimpleLineSearchCursor[x][]);
  }

  @Nonnull
  @Override
  public DeltaSet<UUID> position(final double alpha) {
    return direction.scale(alpha);
  }

  @Override
  public void reset() {
    RefUtil.freeRef(origin.restore());
  }

  @Override
  public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
    if (!Double.isFinite(alpha))
      throw new IllegalArgumentException();
    reset();
    if (0.0 != alpha) {
      direction.accumulate(alpha);
    }
    PointSample temp_25_0005 = subject.measure(monitor);
    @Nonnull final PointSample sample = afterStep(temp_25_0005.setRate(alpha));
    if (null != temp_25_0005)
      temp_25_0005.freeRef();
    final double dot = direction.dot(sample.delta.addRef());
    LineSearchPoint temp_25_0004 = new LineSearchPoint(
        sample == null ? null : sample, dot);
    return temp_25_0004;
  }

  public void _free() {
    if (null != subject)
      subject.freeRef();
    origin.freeRef();
    if (null != direction)
      direction.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  SimpleLineSearchCursor addRef() {
    return (SimpleLineSearchCursor) super.addRef();
  }
}
