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

import com.simiacryptus.mindseye.lang.IterativeStopException;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.orient.DescribeOrientationWrapper;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public @RefAware
class QuadraticSearch implements LineSearchStrategy {

  private final double initialDerivFactor = 0.95;
  private double absoluteTolerance = 1e-12;
  private double currentRate = 0.0;
  private double minRate = 1e-10;
  private double maxRate = 1e10;
  private double relativeTolerance = 1e-2;
  private double stepSize = 1.0;

  public double getAbsoluteTolerance() {
    return absoluteTolerance;
  }

  @Nonnull
  public QuadraticSearch setAbsoluteTolerance(final double absoluteTolerance) {
    this.absoluteTolerance = absoluteTolerance;
    return this;
  }

  public double getCurrentRate() {
    return currentRate;
  }

  @Nonnull
  public QuadraticSearch setCurrentRate(final double currentRate) {
    this.currentRate = currentRate;
    return this;
  }

  public double getMaxRate() {
    return maxRate;
  }

  public QuadraticSearch setMaxRate(double maxRate) {
    this.maxRate = maxRate;
    return this;
  }

  public double getMinRate() {
    return minRate;
  }

  public QuadraticSearch setMinRate(final double minRate) {
    this.minRate = minRate;
    return this;
  }

  public double getRelativeTolerance() {
    return relativeTolerance;
  }

  @Nonnull
  public QuadraticSearch setRelativeTolerance(final double relativeTolerance) {
    this.relativeTolerance = relativeTolerance;
    return this;
  }

  public double getStepSize() {
    return stepSize;
  }

  @Nonnull
  public QuadraticSearch setStepSize(final double stepSize) {
    this.stepSize = stepSize;
    return this;
  }

  public PointSample _step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    double thisX = 0;
    LineSearchPoint thisPoint = cursor.step(thisX, monitor);
    final LineSearchPoint initialPoint = thisPoint;
    double leftX = thisX;
    LineSearchPoint leftPoint = thisPoint;
    monitor.log(String.format("F(%s) = %s", leftX, leftPoint));
    if (0 == leftPoint.derivative) {
      return leftPoint.point;
    }

    @Nonnull final LocateInitialRightPoint locateInitialRightPoint = new LocateInitialRightPoint(cursor, monitor, leftPoint,
        QuadraticSearch.this).apply();
    @Nonnull
    LineSearchPoint rightPoint = locateInitialRightPoint.getRightPoint();
    double rightX = locateInitialRightPoint.getRightX();

    {
      int loops = 0;
      while (true) {
        final double a = (rightPoint.derivative - leftPoint.derivative) / (rightX - leftX);
        final double b = rightPoint.derivative - a * rightX;
        thisX = -b / a;
        final boolean isBracketed = Math.signum(leftPoint.derivative) != Math.signum(rightPoint.derivative);
        if (!Double.isFinite(thisX) || isBracketed && (leftX > thisX || rightX < thisX)) {
          thisX = (rightX + leftX) / 2;
        }
        if (!isBracketed && thisX < 0) {
          thisX = rightX * 2;
        }
        if (thisX < getMinRate())
          thisX = getMinRate();
        if (thisX > getMaxRate())
          thisX = getMaxRate();
        if (isSame(leftX, thisX, 1.0)) {
          monitor.log(String.format("Converged to left"));
          return filter(cursor, leftPoint.point, monitor);
        } else if (isSame(thisX, rightX, 1.0)) {
          monitor.log(String.format("Converged to right"));
          return filter(cursor, rightPoint.point, monitor);
        }
        thisPoint = null;
        thisPoint = cursor.step(thisX, monitor);
        if (isSame(cursor, monitor, leftPoint, thisPoint)) {
          monitor.log(String.format("%s ~= %s", leftX, thisX));
          return filter(cursor, leftPoint.point, monitor);
        }
        if (isSame(cursor, monitor, thisPoint, rightPoint)) {
          monitor.log(String.format("%s ~= %s", thisX, rightX));
          return filter(cursor, rightPoint.point, monitor);
        }
        thisPoint = null;
        thisPoint = cursor.step(thisX, monitor);
        boolean isLeft;
        if (!isBracketed) {
          isLeft = Math.abs(rightPoint.point.rate - thisPoint.point.rate) > Math
              .abs(leftPoint.point.rate - thisPoint.point.rate);
        } else {
          isLeft = thisPoint.derivative < 0;
        }
        //monitor.log(String.format("isLeft=%s; isBracketed=%s; leftPoint=%s; rightPoint=%s", isLeft, isBracketed, leftPoint, rightPoint));
        monitor.log(String.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint,
            thisPoint.point.getMean() - initialPoint.point.getMean()));
        if (loops++ > 10) {
          monitor.log(String.format("Loops = %s", loops));
          return filter(cursor, thisPoint.point, monitor);
        }
        if (isSame(cursor, monitor, leftPoint, rightPoint)) {
          monitor.log(String.format("%s ~= %s", leftX, rightX));
          return filter(cursor, thisPoint.point, monitor);
        }
        if (isLeft) {
          if (thisPoint.point.getMean() > leftPoint.point.getMean()) {
            monitor.log(String.format("%s > %s", thisPoint.point.getMean(), leftPoint.point.getMean()));
            return filter(cursor, leftPoint.point, monitor);
          }
          if (!isBracketed && leftPoint.point.getMean() < rightPoint.point.getMean()) {
            rightX = leftX;
            rightPoint = leftPoint;
          }
          leftPoint = thisPoint;
          leftX = thisX;
          monitor.log(String.format("Left bracket at %s", thisX));
        } else {
          if (thisPoint.point.getMean() > rightPoint.point.getMean()) {
            monitor.log(String.format("%s > %s", thisPoint.point.getMean(), rightPoint.point.getMean()));
            return filter(cursor, rightPoint.point, monitor);
          }
          if (!isBracketed && rightPoint.point.getMean() < leftPoint.point.getMean()) {
            leftX = rightX;
            leftPoint = rightPoint;
          }
          rightX = thisX;
          rightPoint = thisPoint;
          monitor.log(String.format("Right bracket at %s", thisX));
        }
      }
    }
  }

  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    if (currentRate < getMinRate()) {
      currentRate = getMinRate();
    }
    if (currentRate > getMaxRate()) {
      currentRate = getMaxRate();
    }
    final PointSample pointSample = _step(cursor, monitor);
    setCurrentRate(pointSample.rate);
    return pointSample;
  }

  protected boolean isSame(final double a, final double b, final double slack) {
    final double diff = Math.abs(a - b) / slack;
    final double scale = Math.max(Math.abs(a), Math.abs(b));
    return diff < absoluteTolerance || diff < scale * relativeTolerance;
  }

  protected boolean isSame(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                           @Nonnull final LineSearchPoint a, @Nonnull final LineSearchPoint b) {
    if (isSame(a.point.rate, b.point.rate, 1.0)) {
      if (!isSame(a.point.getMean(), b.point.getMean(), 10.0)) {
        @Nonnull final String diagnose = diagnose(cursor, monitor, a, b);
        monitor.log(diagnose);
        throw new IterativeStopException(diagnose);
      }
      return true;
    } else {
      return false;
    }
  }

  private String diagnose(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                          @Nonnull final LineSearchPoint a, @Nonnull final LineSearchPoint b) {
    final LineSearchPoint verifyA = cursor.step(a.point.rate, monitor);
    final boolean validA = isSame(a.point.getMean(), verifyA.point.getMean(), 1.0);
    monitor.log(String.format("Verify %s: %s (%s)", a.point.rate, verifyA.point.getMean(), validA));
    if (!validA) {
      DescribeOrientationWrapper.render(a.point.weights, a.point.delta);
      return "Non-Reproducable Point Found: " + a.point.rate;
    }
    final LineSearchPoint verifyB = cursor.step(b.point.rate, monitor);
    final boolean validB = isSame(b.point.getMean(), verifyB.point.getMean(), 1.0);
    monitor.log(String.format("Verify %s: %s (%s)", b.point.rate, verifyB.point.getMean(), validB));
    if (!validA && !validB)
      return "Non-Reproducable Function Found";
    if (validA && validB)
      return "Function Discontinuity Found";
    if (!validA) {
      return "Non-Reproducable Point Found: " + a.point.rate;
    }
    if (!validB) {
      return "Non-Reproducable Point Found: " + b.point.rate;
    }
    return "";
  }

  private PointSample filter(@Nonnull final LineSearchCursor cursor, @Nonnull final PointSample point,
                             final TrainingMonitor monitor) {
    if (stepSize == 1.0) {
      return point;
    } else {
      LineSearchPoint step = cursor.step(point.rate * stepSize, monitor);
      return step.point;
    }
  }

  private static @RefAware
  class LocateInitialRightPoint extends ReferenceCountingBase {
    @Nonnull
    private final LineSearchCursor cursor;
    @Nonnull
    private final LineSearchPoint initialPoint;
    @Nonnull
    private final TrainingMonitor monitor;
    private final QuadraticSearch parent;
    private LineSearchPoint thisPoint;
    private double thisX;

    public LocateInitialRightPoint(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                                   @Nonnull final LineSearchPoint leftPoint, QuadraticSearch parent) {
      this.cursor = cursor;
      this.monitor = monitor;
      initialPoint = leftPoint;
      this.parent = parent;
      thisX = parent.getCurrentRate() > 0 ? parent.getCurrentRate()
          : Math.abs(leftPoint.point.getMean() * 1e-4 / leftPoint.derivative);
      thisPoint = cursor.step(thisX, monitor);
      monitor.log(String.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint,
          thisPoint.point.getMean() - initialPoint.point.getMean()));
    }

    public LineSearchPoint getRightPoint() {
      return thisPoint;
    }

    public double getRightX() {
      return thisX;
    }

    public static @SuppressWarnings("unused")
    LocateInitialRightPoint[] addRefs(LocateInitialRightPoint[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(LocateInitialRightPoint::addRef)
          .toArray((x) -> new LocateInitialRightPoint[x]);
    }

    @Nonnull
    public LocateInitialRightPoint apply() {
      assertAlive();
      @Nullable
      LineSearchPoint lastPoint = null;
      {
        int loops = 0;
        while (true) {
          lastPoint = thisPoint;
          if (parent.isSame(cursor, monitor, initialPoint, thisPoint)) {
            monitor.log(String.format("%s ~= %s", initialPoint.point.rate, thisX));
            return this;
          } else if (thisPoint.point.getMean() > initialPoint.point.getMean() && thisX > parent.minRate) {
            thisX = thisX / 13;
          } else if (thisPoint.derivative < parent.initialDerivFactor * thisPoint.derivative
              && thisX < parent.maxRate) {
            thisX = thisX * 7;
          } else {
            monitor.log(String.format("%s <= %s", thisPoint.point.getMean(), initialPoint.point.getMean()));
            return this;
          }

          thisPoint = cursor.step(thisX, monitor);
          if (parent.isSame(cursor, monitor, lastPoint, thisPoint)) {
            monitor.log(String.format("%s ~= %s", lastPoint.point.rate, thisX));
            return this;
          }
          monitor.log(String.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint,
              thisPoint.point.getMean() - initialPoint.point.getMean()));
          if (loops++ > 50) {
            monitor.log(String.format("Loops = %s", loops));
            return this;
          }
        }
      }
    }

    public void _free() {
    }

    public @Override
    @SuppressWarnings("unused")
    LocateInitialRightPoint addRef() {
      return (LocateInitialRightPoint) super.addRef();
    }
  }
}
