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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QuadraticSearch implements LineSearchStrategy {

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

  @Nonnull
  public QuadraticSearch setMaxRate(double maxRate) {
    this.maxRate = maxRate;
    return this;
  }

  public double getMinRate() {
    return minRate;
  }

  @Nonnull
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

  @Nullable
  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    if (currentRate < getMinRate()) {
      currentRate = getMinRate();
    }
    if (currentRate > getMaxRate()) {
      currentRate = getMaxRate();
    }
    Stepper stepper = new Stepper(cursor, monitor);
    try {
      final PointSample pointSample = stepper.step();
      assert pointSample != null;
      setCurrentRate(pointSample.rate);
      return pointSample;
    } finally {
      stepper.freeRef();
    }
  }

  protected boolean isSame(final double a, final double b, final double slack) {
    final double diff = Math.abs(a - b) / slack;
    final double scale = Math.max(Math.abs(a), Math.abs(b));
    return diff < absoluteTolerance || diff < scale * relativeTolerance;
  }

  protected boolean isSame(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                           @Nonnull final LineSearchPoint a, @Nonnull final LineSearchPoint b) {
    PointSample pointB = b.getPoint();
    assert pointB != null;
    PointSample pointA = a.getPoint();
    assert pointA != null;
    try {
      if (isSame(pointA.rate, pointB.rate, 1.0)) {
        if (!isSame(pointA.getMean(), pointB.getMean(), 10.0)) {
          @Nonnull final String diagnose = diagnose(cursor, monitor, a, b);
          monitor.log(diagnose);
          throw new IterativeStopException(diagnose);
        }
        cursor.freeRef();
        a.freeRef();
        b.freeRef();
        return true;
      } else {
        cursor.freeRef();
        a.freeRef();
        b.freeRef();
        return false;
      }
    } finally {
      pointA.freeRef();
      pointB.freeRef();
    }
  }

  @Nonnull
  private String diagnose(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                          @Nonnull final LineSearchPoint a, @Nonnull final LineSearchPoint b) {
    PointSample pointA = a.getPoint();
    PointSample pointB = b.getPoint();
    try {
      double verifyMeanA = verifyMean(cursor.addRef(), monitor, a.getPointRate());
      final boolean validA = isSame(pointA.getMean(), verifyMeanA, 1.0);
      monitor.log(RefString.format("Verify %s: %s (%s)", pointA.rate, verifyMeanA, validA));
      assert pointA != null;
      assert pointB != null;
      if (!validA) {
        DescribeOrientationWrapper.render(pointA.weights.addRef(), pointA.delta.addRef());
        return "Non-Reproducable Point Found: " + pointA.rate;
      }
      double verifyMeanB = verifyMean(cursor.addRef(), monitor, pointB.rate);
      final boolean validB = isSame(pointB.getMean(), verifyMeanB, 1.0);
      monitor.log(RefString.format("Verify %s: %s (%s)", pointB.rate, verifyMeanB, validB));
      if (validB) {
        return "Function Discontinuity Found";
      }
      return "Non-Reproducable Point Found: " + pointB.rate;
    } finally {
      cursor.freeRef();
      a.freeRef();
      b.freeRef();
      pointA.freeRef();
      pointB.freeRef();
    }
  }

  private double verifyMean(@Nonnull LineSearchCursor cursor, @Nonnull TrainingMonitor monitor, double rate) {
    final LineSearchPoint point = cursor.step(rate, monitor);
    try {
      assert point != null;
      return point.getPointMean();
    } finally {
      cursor.freeRef();
      point.freeRef();
    }
  }

  @Nullable
  private PointSample filter(@Nonnull final LineSearchCursor cursor, @Nonnull final PointSample point,
                             final TrainingMonitor monitor) {
    if (stepSize == 1.0) {
      cursor.freeRef();
      return point;
    } else {
      try {
        LineSearchPoint step = cursor.step(point.rate * stepSize, monitor);
        assert step != null;
        return getPointSample(step);
      } finally {
        cursor.freeRef();
        point.freeRef();
      }
    }
  }

  private PointSample getPointSample(LineSearchPoint step) {
    try {
      return step.getPoint();
    } finally {
      step.freeRef();
    }
  }

  private class Stepper extends ReferenceCountingBase {
    @Nonnull
    private final LineSearchCursor cursor;
    @Nonnull
    private final TrainingMonitor monitor;
    private double thisX;
    private double leftX;
    private double rightX;
    private LineSearchPoint thisPoint;
    private LineSearchPoint initialPoint;
    private LineSearchPoint leftPoint;
    private LineSearchPoint rightPoint;
    private int loops;

    private Stepper(@Nonnull LineSearchCursor cursor, @Nonnull TrainingMonitor monitor) {
      this.cursor = cursor;
      this.monitor = monitor;
      this.thisX = 0;
      this.thisPoint = this.cursor.step(thisX, this.monitor);
      this.initialPoint = getThisPoint();
      this.leftX = thisX;
      this.leftPoint = getThisPoint();
      this.monitor.log(RefString.format("F(%s) = %s", leftX, getLeftPoint()));
      assert leftPoint != null;
    }

    @NotNull
    private LineSearchPoint getLeftPoint() {
      return leftPoint == null ? null : leftPoint.addRef();
    }

    @org.jetbrains.annotations.Nullable
    private LineSearchPoint getRightPoint() {
      return rightPoint == null ? null : rightPoint.addRef();
    }

    @org.jetbrains.annotations.Nullable
    private LineSearchPoint getThisPoint() {
      return thisPoint == null ? null : thisPoint.addRef();
    }

    private void setThisPoint(LineSearchPoint point) {
      if (null != thisPoint) thisPoint.freeRef();
      thisPoint = point;
    }

    @Nullable
    public PointSample step() {
      if (0 == leftPoint.derivative) {
        return getPointSample(getLeftPoint());
      }
      this.rightX = getCurrentRate() > 0 ? getCurrentRate()
          : Math.abs(leftMean() * 1e-4 / this.leftPoint.derivative);
      RefUtil.freeRef(this.rightPoint);
      this.rightPoint = this.cursor.step(this.rightX, this.monitor);
      assert rightPoint != null;
      monitor.log(RefString.format("F(%s) = %s, evalInputDelta = %s",
          this.rightX,
          this.rightPoint.addRef(),
          rightMean() - leftMean()));
      this.loops = 0;
      while (!locateInitialRightPoint()) {
      }
      this.loops = 0;
      PointSample returnValue = null;
      while (null == returnValue) {
        RefUtil.freeRef(returnValue);
        returnValue = iterate();
      }
      return returnValue;
    }

    @Override
    protected void _free() {
      RefUtil.freeRef(thisPoint);
      RefUtil.freeRef(initialPoint);
      RefUtil.freeRef(leftPoint);
      RefUtil.freeRef(rightPoint);
      RefUtil.freeRef(cursor);
      super._free();
    }

    private boolean locateInitialRightPoint() {
      LineSearchPoint prevRightPoint = getRightPoint();
      try {
        if (isSame(cursor.addRef(), monitor,
            getLeftPoint(), getRightPoint())) {
          monitor.log(RefString.format("%s ~= %s", leftPoint.getPointRate(), rightX));
          return true;
        } else {
          if (rightMean() > leftMean() && rightX > minRate) {
            rightX = rightX / 13;
          } else if (rightPoint.derivative < initialDerivFactor * rightPoint.derivative && rightX < maxRate) {
            rightX = rightX * 7;
          } else {
            monitor.log(RefString.format("%s <= %s", rightMean(), leftMean()));
            return true;
          }
        }

        if (null != rightPoint) rightPoint.freeRef();
        rightPoint = cursor.step(rightX, monitor);
        if (isSame(cursor.addRef(), monitor,
            prevRightPoint == null ? null : prevRightPoint.addRef(),
            getRightPoint()
        )) {
          assert prevRightPoint != null;
          monitor.log(RefString.format("%s ~= %s", prevRightPoint.getPointRate(), rightX));
          return true;
        }
        monitor.log(RefString.format("F(%s) = %s, evalInputDelta = %s", rightX,
            getRightPoint(),
            rightMean() - leftMean()));
        if (loops++ > 50) {
          monitor.log(RefString.format("Loops = %s", loops));
          return true;
        }
        return false;
      } finally {
        RefUtil.freeRef(prevRightPoint);
      }
    }

    @org.jetbrains.annotations.Nullable
    private PointSample iterate() {
      assert rightPoint != null;
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
        monitor.log(RefString.format("Converged to left"));
        return filter(cursor.addRef(), leftPoint.getPoint(), monitor);
      } else if (isSame(thisX, rightX, 1.0)) {
        monitor.log(RefString.format("Converged to right"));
        return filter(cursor.addRef(), rightPoint.getPoint(), monitor);
      } else {
        setThisPoint(cursor.step(thisX, monitor));
        if (isSame(cursor.addRef(), monitor,
            getLeftPoint(),
            getThisPoint()
        )) {
          monitor.log(RefString.format("%s ~= %s", leftX, thisX));
          return filter(cursor.addRef(), leftPoint.getPoint(), monitor);
        } else if (isSame(cursor.addRef(), monitor,
            getThisPoint(),
            rightPoint.addRef()
        )) {
          monitor.log(RefString.format("%s ~= %s", thisX, rightX));
          return filter(cursor.addRef(), rightPoint.getPoint(), monitor);
        } else {
          setThisPoint(cursor.step(thisX, monitor));
          boolean isLeft;
          if (!isBracketed) {
            assert thisPoint != null;
            isLeft = Math.abs(rightPoint.getPointRate() - thisPoint.getPointRate()) > Math
                .abs(leftPoint.getPointRate() - thisPoint.getPointRate());
          } else {
            assert thisPoint != null;
            isLeft = thisPoint.derivative < 0;
          }
          //monitor.log(String.format("isLeft=%s; isBracketed=%s; leftPoint=%s; rightPoint=%s", isLeft, isBracketed, leftPoint, rightPoint));
          monitor.log(RefString.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint.addRef(),
              thisMean() - initialPoint.getPointMean()));
          if (loops++ > 10) {
            monitor.log(RefString.format("Loops = %s", loops));
            return filter(cursor.addRef(), thisPoint.getPoint(), monitor);
          } else if (isSame(cursor.addRef(), monitor, getLeftPoint(), rightPoint.addRef())) {
            monitor.log(RefString.format("%s ~= %s", leftX, rightX));
            return filter(cursor.addRef(), thisPoint.getPoint(), monitor);
          } else if (isLeft) {
            if (thisMean() > leftMean()) {
              monitor.log(RefString.format("%s > %s", thisMean(), leftMean()));
              return filter(cursor.addRef(), leftPoint.getPoint(), monitor);
            } else {
              if (!isBracketed && leftMean() < rightMean()) {
                rightX = leftX;
                rightPoint.freeRef();
                rightPoint = leftPoint;
              } else {
                leftPoint.freeRef();
              }
              leftPoint = thisPoint.addRef();
              leftX = thisX;
              monitor.log(RefString.format("Left bracket at %s", thisX));
              return null;
            }
          } else {
            if (thisMean() > rightMean()) {
              monitor.log(RefString.format("%s > %s", thisMean(), rightMean()));
              return filter(cursor.addRef(), rightPoint.getPoint(), monitor);
            } else {
              if (!isBracketed && rightMean() < leftMean()) {
                leftX = rightX;
                if (null != leftPoint) leftPoint.freeRef();
                leftPoint = rightPoint;
              } else {
                rightPoint.freeRef();
              }
              rightX = thisX;
              rightPoint = thisPoint.addRef();
              monitor.log(RefString.format("Right bracket at %s", thisX));
              return null;
            }
          }
        }
      }
    }

    private double leftMean() {
      return leftPoint.getPointMean();
    }

    private double thisMean() {
      return thisPoint.getPointMean();
    }

    private double rightMean() {
      return rightPoint.getPointMean();
    }
  }
}
