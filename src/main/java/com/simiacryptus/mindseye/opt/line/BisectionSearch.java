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

import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The type Bisection search.
 */
public class BisectionSearch implements LineSearchStrategy {

  private double maxRate = 1e20;
  private double currentRate = 1.0;
  private double zeroTol = 1e-20;
  private double spanTol = 1e-3;

  /**
   * Gets current rate.
   *
   * @return the current rate
   */
  public double getCurrentRate() {
    return currentRate;
  }

  /**
   * Sets current rate.
   *
   * @param currentRate the current rate
   * @return the current rate
   */
  @Nonnull
  public BisectionSearch setCurrentRate(final double currentRate) {
    this.currentRate = currentRate;
    return this;
  }

  /**
   * Gets max rate.
   *
   * @return the max rate
   */
  public double getMaxRate() {
    return maxRate;
  }

  /**
   * Sets max rate.
   *
   * @param maxRate the max rate
   * @return the max rate
   */
  @Nonnull
  public BisectionSearch setMaxRate(double maxRate) {
    this.maxRate = maxRate;
    return this;
  }

  /**
   * Gets span tol.
   *
   * @return the span tol
   */
  public double getSpanTol() {
    return spanTol;
  }

  /**
   * Sets span tol.
   *
   * @param spanTol the span tol
   * @return the span tol
   */
  @Nonnull
  public BisectionSearch setSpanTol(double spanTol) {
    this.spanTol = spanTol;
    return this;
  }

  /**
   * Gets zero tol.
   *
   * @return the zero tol
   */
  public double getZeroTol() {
    return zeroTol;
  }

  /**
   * Sets zero tol.
   *
   * @param zeroTol the zero tol
   * @return the zero tol
   */
  @Nonnull
  public BisectionSearch setZeroTol(final double zeroTol) {
    this.zeroTol = zeroTol;
    return this;
  }

  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {

    double leftX = 0;
    final LineSearchPoint searchPoint = cursor.step(leftX, monitor);
    monitor.log(RefString.format("F(%s) = %s", leftX, searchPoint.addRef()));
    assert searchPoint != null;
    double leftValue = searchPoint.getPointSum();
    searchPoint.freeRef();
    double rightRight = getMaxRate();
    double rightX;
    double rightLineDeriv;
    double rightValue;
    double rightRightSoft = currentRate * 2;
    LineSearchPoint rightPoint = null;
    int loopCount = 0;
    while (true) {
      rightX = (leftX + Math.min(rightRight, rightRightSoft)) / 2;
      if (null != rightPoint) rightPoint.freeRef();
      rightPoint = cursor.step(rightX, monitor);
      monitor.log(RefString.format("F(%s)@%s = %s", rightX, loopCount, rightPoint.addRef()));
      assert rightPoint != null;
      rightLineDeriv = rightPoint.derivative;
      rightValue = rightPoint.getPointSum();
      if (loopCount++ > 100) {
        monitor.log(RefString.format("Loop overflow"));
        break;
      }
      if ((rightRight - leftX) * 2.0 / (leftX + rightRight) < spanTol) {
        monitor.log(RefString.format("Right limit is nonconvergent at %s/%s", leftX, rightRight));
        currentRate = leftX;
        rightPoint.freeRef();
        LineSearchPoint temp_49_0003 = cursor.step(leftX, monitor);
        assert temp_49_0003 != null;
        PointSample temp_49_0002 = temp_49_0003.getPoint();
        temp_49_0003.freeRef();
        cursor.freeRef();
        return temp_49_0002;
      }
      if (rightValue > leftValue) {
        rightRight = rightX;
        monitor.log(RefString.format("Right is at most %s", rightX));
      } else if (rightLineDeriv < 0) {
        rightRightSoft *= 2.0;
        leftValue = rightValue;
        leftX = rightX;
        monitor.log(RefString.format("Right is at least %s", rightX));
      } else {
        break;
      }
    }
    rightPoint.freeRef();
    monitor.log(RefString.format("Starting bisection search from %s to %s", leftX, rightX));
    LineSearchPoint temp_49_0004 = iterate(cursor, monitor, leftX, rightX);
    assert temp_49_0004 != null;
    PointSample temp_49_0001 = temp_49_0004.getPoint();
    temp_49_0004.freeRef();
    return temp_49_0001;
  }

  /**
   * Iterate line search point.
   *
   * @param cursor  the cursor
   * @param monitor the monitor
   * @param leftX   the left x
   * @param rightX  the right x
   * @return the line search point
   */
  @Nullable
  public LineSearchPoint iterate(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                                 double leftX, double rightX) {
    LineSearchPoint searchPoint = null;
    int loopCount = 0;
    try {
      while (true) {
        double thisX = (rightX + leftX) / 2;
        if (null != searchPoint) searchPoint.freeRef();
        searchPoint = cursor.step(thisX, monitor);
        monitor.log(RefString.format("F(%s) = %s", thisX, searchPoint.addRef()));
        if (loopCount++ > 1000) {
          return searchPoint.addRef();
        }
        assert searchPoint != null;
        if (searchPoint.derivative < -zeroTol) {
          if (leftX == thisX) {
            monitor.log(RefString.format("End (static left) at %s", thisX));
            currentRate = thisX;
            return searchPoint.addRef();
          }
          leftX = thisX;
        } else if (searchPoint.derivative > zeroTol) {
          if (rightX == thisX) {
            monitor.log(RefString.format("End (static right) at %s", thisX));
            currentRate = thisX;
            return searchPoint.addRef();
          }
          rightX = thisX;
        } else {
          monitor.log(RefString.format("End (at min) at %s", thisX));
          currentRate = thisX;
          return searchPoint.addRef();
        }
        if (Math.log10((rightX - leftX) * 2.0 / (leftX + rightX)) < -1) {
          monitor.log(RefString.format("End (narrow range) at %s to %s", rightX, leftX));
          currentRate = thisX;
          return searchPoint.addRef();
        }
      }
    } finally {
      if (null != searchPoint) searchPoint.freeRef();
      cursor.freeRef();
    }
  }
}
