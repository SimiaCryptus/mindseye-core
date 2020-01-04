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

import javax.annotation.Nonnull;
import java.util.UUID;

public @com.simiacryptus.ref.lang.RefAware class ValidatingOrientationWrapper
    extends OrientationStrategyBase<LineSearchCursor> {

  private final OrientationStrategy<? extends LineSearchCursor> inner;

  public ValidatingOrientationWrapper(final OrientationStrategy<? extends LineSearchCursor> inner) {
    this.inner = inner;
  }

  @Nonnull
  @Override
  public LineSearchCursor orient(final Trainable subject, final PointSample measurement,
      final TrainingMonitor monitor) {
    final LineSearchCursor cursor = inner.orient(subject, measurement, monitor);
    return new ValidatingLineSearchCursor(cursor);
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
  }

  private static @com.simiacryptus.ref.lang.RefAware class ValidatingLineSearchCursor extends LineSearchCursorBase {
    private final LineSearchCursor cursor;

    public ValidatingLineSearchCursor(final LineSearchCursor cursor) {
      this.cursor = cursor;
    }

    @Override
    public CharSequence getDirectionType() {
      return cursor.getDirectionType();
    }

    @Override
    public PointSample afterStep(@Nonnull PointSample step) {
      super.afterStep(step);
      return cursor.afterStep(step);
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
      test(monitor, primaryPoint, 1e-3);
      test(monitor, primaryPoint, 1e-4);
      test(monitor, primaryPoint, 1e-6);
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
    }

    public void _free() {
    }

    public @Override @SuppressWarnings("unused") ValidatingLineSearchCursor addRef() {
      return (ValidatingLineSearchCursor) super.addRef();
    }

    public static @SuppressWarnings("unused") ValidatingLineSearchCursor[] addRefs(ValidatingLineSearchCursor[] array) {
      if (array == null)
        return null;
      return java.util.Arrays.stream(array).filter((x) -> x != null).map(ValidatingLineSearchCursor::addRef)
          .toArray((x) -> new ValidatingLineSearchCursor[x]);
    }
  }

  public @Override @SuppressWarnings("unused") ValidatingOrientationWrapper addRef() {
    return (ValidatingOrientationWrapper) super.addRef();
  }

  public static @SuppressWarnings("unused") ValidatingOrientationWrapper[] addRefs(
      ValidatingOrientationWrapper[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ValidatingOrientationWrapper::addRef)
        .toArray((x) -> new ValidatingOrientationWrapper[x]);
  }

  public static @SuppressWarnings("unused") ValidatingOrientationWrapper[][] addRefs(
      ValidatingOrientationWrapper[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ValidatingOrientationWrapper::addRefs)
        .toArray((x) -> new ValidatingOrientationWrapper[x][]);
  }
}
