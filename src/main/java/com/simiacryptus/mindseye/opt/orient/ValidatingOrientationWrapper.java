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
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

/**
 * This class wraps an OrientationStrategy object, validating it for null values.
 *
 * @docgenVersion 9
 */
public class ValidatingOrientationWrapper extends OrientationStrategyBase<LineSearchCursor> {

  @Nullable
  private final OrientationStrategy<? extends LineSearchCursor> inner;

  /**
   * Instantiates a new Validating orientation wrapper.
   *
   * @param inner the inner
   */
  public ValidatingOrientationWrapper(@Nullable final OrientationStrategy<? extends LineSearchCursor> inner) {
    OrientationStrategy<? extends LineSearchCursor> temp_26_0001 = inner == null ? null : inner.addRef();
    this.inner = temp_26_0001 == null ? null : temp_26_0001.addRef();
    if (null != temp_26_0001)
      temp_26_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  /**
   * @return the LineSearchCursor in its current orientation
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public LineSearchCursor orient(@Nullable final Trainable subject, @Nullable final PointSample measurement,
                                 final TrainingMonitor monitor) {
    assert inner != null;
    final LineSearchCursor cursor = inner.orient(subject, measurement, monitor);
    return new ValidatingLineSearchCursor(cursor);
  }

  /**
   * Resets the value.
   *
   * @docgenVersion 9
   */
  @Override
  public void reset() {
    assert inner != null;
    inner.reset();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != inner)
      inner.freeRef();
  }

  /**
   * Adds a reference to the ValidatingOrientationWrapper.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ValidatingOrientationWrapper addRef() {
    return (ValidatingOrientationWrapper) super.addRef();
  }

  /**
   * A cursor that validates the lines it searches.
   *
   * @param cursor the cursor to validate
   * @docgenVersion 9
   */
  private static class ValidatingLineSearchCursor extends LineSearchCursorBase {
    @Nullable
    private final LineSearchCursor cursor;

    /**
     * Instantiates a new Validating line search cursor.
     *
     * @param cursor the cursor
     */
    public ValidatingLineSearchCursor(@Nullable final LineSearchCursor cursor) {
      this.cursor = cursor;
    }

    /**
     * Returns the direction type of this object.
     *
     * @docgenVersion 9
     */
    @Override
    public CharSequence getDirectionType() {
      assert cursor != null;
      return cursor.getDirectionType();
    }

    /**
     * Adds a reference to the ValidatingLineSearchCursor.
     *
     * @docgenVersion 9
     */
    @Nullable
    public static @SuppressWarnings("unused")
    ValidatingLineSearchCursor[] addRef(@Nullable ValidatingLineSearchCursor[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter(x -> {
        boolean notNull = x != null;
        x.freeRef();
        return notNull;
      }).toArray(ValidatingLineSearchCursor[]::new);
    }

    /**
     * Returns the PointSample after taking a step.
     *
     * @docgenVersion 9
     */
    @Override
    public PointSample afterStep(@Nonnull PointSample step) {
      super.afterStep(step.addRef()).freeRef();
      assert cursor != null;
      return cursor.afterStep(step);
    }

    /**
     * Returns the delta set of UUIDs for the positions.
     *
     * @docgenVersion 9
     */
    @Override
    public DeltaSet<UUID> position(final double alpha) {
      assert cursor != null;
      return cursor.position(alpha);
    }

    /**
     * Resets the value.
     *
     * @docgenVersion 9
     */
    @Override
    public void reset() {
      assert cursor != null;
      cursor.reset();
    }

    /**
     * This method steps through the line search process.
     *
     * @docgenVersion 9
     */
    @javax.annotation.Nullable
    @Override
    public LineSearchPoint step(final double alpha, @Nonnull final TrainingMonitor monitor) {
      assert cursor != null;
      final LineSearchPoint primaryPoint = cursor.step(alpha, monitor);
      //monitor.log(String.format("f(%s) = %s",alphaList, primaryPoint.point.value));
      test(monitor, primaryPoint == null ? null : primaryPoint.addRef(), 1e-3);
      test(monitor, primaryPoint == null ? null : primaryPoint.addRef(), 1e-4);
      test(monitor, primaryPoint == null ? null : primaryPoint.addRef(), 1e-6);
      return primaryPoint;
    }

    /**
     * This is a test function that does nothing.
     *
     * @docgenVersion 9
     */
    public void test(@Nonnull final TrainingMonitor monitor, @Nonnull final LineSearchPoint primaryPoint,
                     final double probeSize) {
      final double alpha = primaryPoint.getPointRate();
      double probeAlpha = alpha + primaryPoint.getPointSum() * probeSize / primaryPoint.derivative;
      if (!Double.isFinite(probeAlpha) || probeAlpha == alpha) {
        probeAlpha = alpha + probeSize;
      }
      assert cursor != null;
      final LineSearchPoint probePoint = cursor.step(probeAlpha, monitor);
      assert probePoint != null;
      final double dy = probePoint.getPointSum() - primaryPoint.getPointSum();
      final double dx = probeAlpha - alpha;
      final double measuredDerivative = dy / dx;
      monitor.log(RefString.format("%s vs (%s, %s); probe=%s", measuredDerivative, primaryPoint.derivative,
          probePoint.derivative, probeSize));
      primaryPoint.freeRef();
      probePoint.freeRef();
    }

    /**
     * Frees the memory associated with this object.
     *
     * @docgenVersion 9
     */
    public void _free() {
      super._free();
      if (null != cursor)
        cursor.freeRef();
    }

    /**
     * Adds a reference to the ValidatingLineSearchCursor.
     *
     * @docgenVersion 9
     */
    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    ValidatingLineSearchCursor addRef() {
      return (ValidatingLineSearchCursor) super.addRef();
    }
  }
}
