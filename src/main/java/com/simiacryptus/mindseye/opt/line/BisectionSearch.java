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

import javax.annotation.Nonnull;

public @com.simiacryptus.ref.lang.RefAware
class BisectionSearch implements LineSearchStrategy {

  private double maxRate = 1e20;
  private double currentRate = 1.0;
  private double zeroTol = 1e-20;
  private double spanTol = 1e-3;

  public double getCurrentRate() {
    return currentRate;
  }

  @Nonnull
  public BisectionSearch setCurrentRate(final double currentRate) {
    this.currentRate = currentRate;
    return this;
  }

  public double getMaxRate() {
    return maxRate;
  }

  public BisectionSearch setMaxRate(double maxRate) {
    this.maxRate = maxRate;
    return this;
  }

  public double getSpanTol() {
    return spanTol;
  }

  public BisectionSearch setSpanTol(double spanTol) {
    this.spanTol = spanTol;
    return this;
  }

  public double getZeroTol() {
    return zeroTol;
  }

  @Nonnull
  public BisectionSearch setZeroTol(final double zeroTol) {
    this.zeroTol = zeroTol;
    return this;
  }

  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {

    double leftX = 0;
    double leftValue;
    final LineSearchPoint searchPoint = cursor.step(leftX, monitor);
    monitor.log(String.format("F(%s) = %s", leftX, searchPoint));
    leftValue = searchPoint.point.sum;
    double rightRight = getMaxRate();
    double rightX;
    double rightLineDeriv;
    double rightValue;
    double rightRightSoft = currentRate * 2;
    LineSearchPoint rightPoint = null;
    int loopCount = 0;
    while (true) {
      rightX = (leftX + Math.min(rightRight, rightRightSoft)) / 2;
      rightPoint = cursor.step(rightX, monitor);
      monitor.log(String.format("F(%s)@%s = %s", rightX, loopCount, rightPoint));
      rightLineDeriv = rightPoint.derivative;
      rightValue = rightPoint.point.sum;
      if (loopCount++ > 100) {
        monitor.log(String.format("Loop overflow"));
        break;
      }
      if ((rightRight - leftX) * 2.0 / (leftX + rightRight) < spanTol) {
        monitor.log(String.format("Right limit is nonconvergent at %s/%s", leftX, rightRight));
        currentRate = leftX;
        return cursor.step(leftX, monitor).point;
      }
      if (rightValue > leftValue) {
        rightRight = rightX;
        monitor.log(String.format("Right is at most %s", rightX));
      } else if (rightLineDeriv < 0) {
        rightRightSoft *= 2.0;
        leftValue = rightValue;
        leftX = rightX;
        monitor.log(String.format("Right is at least %s", rightX));
      } else {
        break;
      }
    }
    monitor.log(String.format("Starting bisection search from %s to %s", leftX, rightX));
    return iterate(cursor, monitor, leftX, rightX).point;
  }

  public LineSearchPoint iterate(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                                 double leftX, double rightX) {
    LineSearchPoint searchPoint = null;
    int loopCount = 0;
    while (true) {
      double thisX;
      thisX = (rightX + leftX) / 2;
      searchPoint = cursor.step(thisX, monitor);
      monitor.log(String.format("F(%s) = %s", thisX, searchPoint));
      if (loopCount++ > 1000)
        return searchPoint;
      if (searchPoint.derivative < -zeroTol) {
        if (leftX == thisX) {
          monitor.log(String.format("End (static left) at %s", thisX));
          currentRate = thisX;
          return searchPoint;
        }
        leftX = thisX;
      } else if (searchPoint.derivative > zeroTol) {
        if (rightX == thisX) {
          monitor.log(String.format("End (static right) at %s", thisX));
          currentRate = thisX;
          return searchPoint;
        }
        rightX = thisX;
      } else {
        monitor.log(String.format("End (at min) at %s", thisX));
        currentRate = thisX;
        return searchPoint;
      }
      if (Math.log10((rightX - leftX) * 2.0 / (leftX + rightX)) < -1) {
        monitor.log(String.format("End (narrow range) at %s to %s", rightX, leftX));
        currentRate = thisX;
        return searchPoint;
      }
    }
  }
}
