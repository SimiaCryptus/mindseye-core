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
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursorBase;
import com.simiacryptus.mindseye.opt.line.LineSearchPoint;
import com.simiacryptus.ref.lang.RefAware;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

public @RefAware
class ValidatingOrientationWrapper extends OrientationStrategyBase<LineSearchCursor> {

  private final OrientationStrategy<? extends LineSearchCursor> inner;

  public ValidatingOrientationWrapper(final OrientationStrategy<? extends LineSearchCursor> inner) {
    {
      OrientationStrategy<? extends LineSearchCursor> temp_26_0001 = inner == null
          ? null
          : inner.addRef();
      this.inner = temp_26_0001 == null ? null : temp_26_0001.addRef();
      if (null != temp_26_0001)
        temp_26_0001.freeRef();
    }
    if (null != inner)
      inner.freeRef();
  }

  public static @SuppressWarnings("unused")
  ValidatingOrientationWrapper[] addRefs(
      ValidatingOrientationWrapper[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValidatingOrientationWrapper::addRef)
        .toArray((x) -> new ValidatingOrientationWrapper[x]);
  }

  public static @SuppressWarnings("unused")
  ValidatingOrientationWrapper[][] addRefs(
      ValidatingOrientationWrapper[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValidatingOrientationWrapper::addRefs)
        .toArray((x) -> new ValidatingOrientationWrapper[x][]);
  }

  @Nonnull
  @Override
  public LineSearchCursor orient(final Trainable subject, final PointSample measurement,
                                 final TrainingMonitor monitor) {
    final LineSearchCursor cursor = inner.orient(subject == null ? null : subject.addRef(),
        measurement == null ? null : measurement.addRef(), monitor);
    if (null != measurement)
      measurement.freeRef();
    if (null != subject)
      subject.freeRef();
    ValidatingOrientationWrapper.ValidatingLineSearchCursor temp_26_0003 = new ValidatingLineSearchCursor(
        cursor == null ? null : cursor.addRef());
    if (null != cursor)
      cursor.freeRef();
    return temp_26_0003;
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  ValidatingOrientationWrapper addRef() {
    return (ValidatingOrientationWrapper) super.addRef();
  }

  private static @RefAware
  class ValidatingLineSearchCursor extends LineSearchCursorBase {
    private final LineSearchCursor cursor;

    public ValidatingLineSearchCursor(final LineSearchCursor cursor) {
      {
        LineSearchCursor temp_26_0002 = cursor == null ? null : cursor.addRef();
        this.cursor = temp_26_0002 == null ? null : temp_26_0002.addRef();
        if (null != temp_26_0002)
          temp_26_0002.freeRef();
      }
      if (null != cursor)
        cursor.freeRef();
    }

    @Override
    public CharSequence getDirectionType() {
      return cursor.getDirectionType();
    }

    public static @SuppressWarnings("unused")
    ValidatingLineSearchCursor[] addRefs(ValidatingLineSearchCursor[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(ValidatingLineSearchCursor::addRef)
          .toArray((x) -> new ValidatingLineSearchCursor[x]);
    }

    @Override
    public PointSample afterStep(@Nonnull PointSample step) {
      super.afterStep(step);
      PointSample temp_26_0004 = cursor.afterStep(step == null ? null : step);
      return temp_26_0004;
    }

    @Override
    public DeltaSet<UUID> position(final double alpha) {
      return cursor.position(alpha);
    }

    @Override
    public void reset() {
      cursor.reset();
    }

    @Override
    public LineSearchPoint step(final double alpha, @Nonnull final TrainingMonitor monitor) {
      final LineSearchPoint primaryPoint = cursor.step(alpha, monitor);
      //monitor.log(String.format("f(%s) = %s",alphaList, primaryPoint.point.value));
      test(monitor, primaryPoint == null ? null : primaryPoint.addRef(), 1e-3);
      test(monitor, primaryPoint == null ? null : primaryPoint.addRef(), 1e-4);
      test(monitor, primaryPoint == null ? null : primaryPoint.addRef(), 1e-6);
      return primaryPoint;
    }

    public void test(@Nonnull final TrainingMonitor monitor, @Nonnull final LineSearchPoint primaryPoint,
                     final double probeSize) {
      final double alpha = primaryPoint.point.rate;
      double probeAlpha = alpha + primaryPoint.point.sum * probeSize / primaryPoint.derivative;
      if (!Double.isFinite(probeAlpha) || probeAlpha == alpha) {
        probeAlpha = alpha + probeSize;
      }
      final LineSearchPoint probePoint = cursor.step(probeAlpha, monitor);
      final double dy = probePoint.point.sum - primaryPoint.point.sum;
      final double dx = probeAlpha - alpha;
      final double measuredDerivative = dy / dx;
      monitor.log(String.format("%s vs (%s, %s); probe=%s", measuredDerivative, primaryPoint.derivative,
          probePoint.derivative, probeSize));
      primaryPoint.freeRef();
      if (null != probePoint)
        probePoint.freeRef();
    }

    public void _free() {
      if (null != cursor)
        cursor.freeRef();
    }

    public @Override
    @SuppressWarnings("unused")
    ValidatingLineSearchCursor addRef() {
      return (ValidatingLineSearchCursor) super.addRef();
    }
  }
}
