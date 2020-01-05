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
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefAware;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

public @RefAware
class GradientDescent extends OrientationStrategyBase<SimpleLineSearchCursor> {

  public static @SuppressWarnings("unused")
  GradientDescent[] addRefs(GradientDescent[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(GradientDescent::addRef)
        .toArray((x) -> new GradientDescent[x]);
  }

  public static @SuppressWarnings("unused")
  GradientDescent[][] addRefs(GradientDescent[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(GradientDescent::addRefs)
        .toArray((x) -> new GradientDescent[x][]);
  }

  @Nonnull
  @Override
  public SimpleLineSearchCursor orient(final Trainable subject, @Nonnull final PointSample measurement,
                                       @Nonnull final TrainingMonitor monitor) {
    @Nonnull final DeltaSet<UUID> direction = measurement.delta.scale(-1);
    final double magnitude = direction.getMagnitude();
    if (Math.abs(magnitude) < 1e-10) {
      monitor.log(String.format("Zero gradient: %s", magnitude));
    } else if (Math.abs(magnitude) < 1e-5) {
      monitor.log(String.format("Low gradient: %s", magnitude));
    }
    SimpleLineSearchCursor temp_42_0002 = new SimpleLineSearchCursor(
        subject == null ? null : subject.addRef(), measurement == null ? null : measurement,
        direction == null ? null : direction);
    SimpleLineSearchCursor temp_42_0001 = temp_42_0002.setDirectionType("GD");
    if (null != temp_42_0002)
      temp_42_0002.freeRef();
    if (null != subject)
      subject.freeRef();
    return temp_42_0001;
  }

  @Override
  public void reset() {

  }

  public void _free() {

  }

  public @Override
  @SuppressWarnings("unused")
  GradientDescent addRef() {
    return (GradientDescent) super.addRef();
  }
}
