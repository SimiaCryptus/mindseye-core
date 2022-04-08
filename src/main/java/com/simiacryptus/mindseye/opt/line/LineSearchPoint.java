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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefStringBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * This class represents a point on a line search.
 *
 * @param derivative the derivative at this point
 * @param point      the point on the line (may be null)
 * @docgenVersion 9
 */
public class LineSearchPoint extends ReferenceCountingBase {

  /**
   * The Derivative.
   */
  public final double derivative;
  @Nullable
  private final PointSample point;

  /**
   * Instantiates a new Line search point.
   *
   * @param point      the point
   * @param derivative the derivative
   */
  public LineSearchPoint(@Nullable final PointSample point, final double derivative) {
    this.point = point;
    this.derivative = derivative;
  }

  /**
   * Returns a PointSample.
   *
   * @docgenVersion 9
   */
  @Nullable
  public PointSample getPoint() {
    if (null == point) return null;
    return point.addRef();
  }

  /**
   * Returns the mean value of all points.
   *
   * @docgenVersion 9
   */
  public double getPointMean() {
    return point.getMean();
  }

  /**
   * Returns the point rate.
   *
   * @docgenVersion 9
   */
  public double getPointRate() {
    return point.rate;
  }

  /**
   * Returns the sum of all points in the game.
   *
   * @docgenVersion 9
   */
  public double getPointSum() {
    return point.sum;
  }

  /**
   * Returns a string representation of this object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    @Nonnull final RefStringBuilder sb = new RefStringBuilder(
        "LineSearchPoint{");
    sb.append("point=").append(getPoint());
    sb.append(", derivative=").append(derivative);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != point)
      point.freeRef();
  }

  /**
   * Add a reference to the LineSearchPoint.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LineSearchPoint addRef() {
    return (LineSearchPoint) super.addRef();
  }

  /**
   * Returns a copy of the point delta set.
   *
   * @docgenVersion 9
   */
  public DeltaSet<UUID> copyPointDelta() {
    return point.delta.copy();
  }
}
