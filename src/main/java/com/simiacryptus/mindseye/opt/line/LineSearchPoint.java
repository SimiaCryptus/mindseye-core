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
 * The type Line search point.
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
   * Gets point.
   *
   * @return the point
   */
  @Nullable
  public PointSample getPoint() {
    if (null == point) return null;
    return point.addRef();
  }

  /**
   * Gets point mean.
   *
   * @return the point mean
   */
  public double getPointMean() {
    return point.getMean();
  }

  /**
   * Gets point rate.
   *
   * @return the point rate
   */
  public double getPointRate() {
    return point.rate;
  }

  /**
   * Gets point sum.
   *
   * @return the point sum
   */
  public double getPointSum() {
    return point.sum;
  }

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

  public void _free() {
    super._free();
    if (null != point)
      point.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LineSearchPoint addRef() {
    return (LineSearchPoint) super.addRef();
  }

  /**
   * Copy point delta delta set.
   *
   * @return the delta set
   */
  public DeltaSet<UUID> copyPointDelta() {
    return point.delta.copy();
  }
}
