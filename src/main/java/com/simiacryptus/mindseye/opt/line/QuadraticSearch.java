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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

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
  public PointSample _step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    double thisX = 0;
    LineSearchPoint thisPoint = cursor.step(thisX, monitor);
    final LineSearchPoint initialPoint = thisPoint == null ? null : thisPoint.addRef();
    double leftX = thisX;
    LineSearchPoint leftPoint = thisPoint == null ? null : thisPoint.addRef();
    monitor.log(RefString.format("F(%s) = %s", leftX, leftPoint.addRef()));
    assert leftPoint != null;
    if (0 == leftPoint.derivative) {
      thisPoint.freeRef();
      initialPoint.freeRef();
      PointSample temp_17_0007 = leftPoint.point;
      leftPoint.freeRef();
      cursor.freeRef();
      return temp_17_0007;
    }

    QuadraticSearch.LocateInitialRightPoint locateInitialRightPoint = new LocateInitialRightPoint(
        cursor.addRef(), monitor, leftPoint.addRef(), QuadraticSearch.this);
    locateInitialRightPoint.apply();
    @Nonnull
    LineSearchPoint rightPoint = locateInitialRightPoint.getRightPoint();
    double rightX = locateInitialRightPoint.getRightX();
    locateInitialRightPoint.freeRef();
    int loops = 0;
    try {
      while (true) {
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
          assert leftPoint.point != null;
          return filter(cursor.addRef(), leftPoint.point.addRef(), monitor);
        } else if (isSame(thisX, rightX, 1.0)) {
          monitor.log(RefString.format("Converged to right"));
          assert rightPoint.point != null;
          return filter(cursor.addRef(), rightPoint.point.addRef(), monitor);
        }
        if (null != thisPoint) thisPoint.freeRef();
        thisPoint = cursor.step(thisX, monitor);
        if (isSame(cursor.addRef(), monitor, leftPoint.addRef(),
            thisPoint == null ? null : thisPoint.addRef())) {
          monitor.log(RefString.format("%s ~= %s", leftX, thisX));
          assert leftPoint.point != null;
          return filter(cursor.addRef(), leftPoint.point.addRef(), monitor);
        }
        if (isSame(cursor.addRef(), monitor, thisPoint == null ? null : thisPoint.addRef(),
            rightPoint.addRef())) {
          monitor.log(RefString.format("%s ~= %s", thisX, rightX));
          assert rightPoint.point != null;
          return filter(cursor.addRef(), rightPoint.point.addRef(), monitor);
        }
        if (null != thisPoint)
          thisPoint.freeRef();
        thisPoint = cursor.step(thisX, monitor);
        boolean isLeft;
        if (!isBracketed) {
          assert leftPoint.point != null;
          assert thisPoint != null;
          assert thisPoint.point != null;
          assert rightPoint.point != null;
          isLeft = Math.abs(rightPoint.point.rate - thisPoint.point.rate) > Math
              .abs(leftPoint.point.rate - thisPoint.point.rate);
        } else {
          assert thisPoint != null;
          isLeft = thisPoint.derivative < 0;
        }
        //monitor.log(String.format("isLeft=%s; isBracketed=%s; leftPoint=%s; rightPoint=%s", isLeft, isBracketed, leftPoint, rightPoint));
        assert initialPoint.point != null;
        assert thisPoint.point != null;
        monitor.log(RefString.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint.addRef(),
            thisPoint.point.getMean() - initialPoint.point.getMean()));
        if (loops++ > 10) {
          monitor.log(RefString.format("Loops = %s", loops));
          PointSample temp_17_0005 = filter(cursor.addRef(), thisPoint.point.addRef(), monitor);
          return temp_17_0005;
        }
        if (isSame(cursor.addRef(), monitor, leftPoint.addRef(), rightPoint.addRef())) {
          monitor.log(RefString.format("%s ~= %s", leftX, rightX));
          return filter(cursor.addRef(), thisPoint.point.addRef(), monitor);
        }
        if (isLeft) {
          assert leftPoint.point != null;
          if (thisPoint.point.getMean() > leftPoint.point.getMean()) {
            monitor.log(RefString.format("%s > %s", thisPoint.point.getMean(), leftPoint.point.getMean()));
            return filter(cursor.addRef(), leftPoint.point.addRef(), monitor);
          }
          if (!isBracketed && leftPoint.point.getMean() < rightPoint.point.getMean()) {
            rightX = leftX;
            rightPoint.freeRef();
            rightPoint = leftPoint.addRef();
          }
          leftPoint.freeRef();
          leftPoint = thisPoint.addRef();
          leftX = thisX;
          monitor.log(RefString.format("Left bracket at %s", thisX));
        } else {
          assert rightPoint.point != null;
          if (thisPoint.point.getMean() > rightPoint.point.getMean()) {
            monitor.log(RefString.format("%s > %s", thisPoint.point.getMean(), rightPoint.point.getMean()));
            return filter(cursor.addRef(), rightPoint.point.addRef(), monitor);
          }
          if (!isBracketed && rightPoint.point.getMean() < leftPoint.point.getMean()) {
            leftX = rightX;
            leftPoint.freeRef();
            leftPoint = rightPoint.addRef();
          }
          rightX = thisX;
          rightPoint.freeRef();
          rightPoint = thisPoint.addRef();
          monitor.log(RefString.format("Right bracket at %s", thisX));
        }
      }
    } finally {
      leftPoint.freeRef();
      initialPoint.freeRef();
      thisPoint.freeRef();
      rightPoint.freeRef();
      cursor.freeRef();
    }
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
    final PointSample pointSample = _step(cursor, monitor);
    assert pointSample != null;
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
    assert b.point != null;
    assert a.point != null;
    if (isSame(a.point.rate, b.point.rate, 1.0)) {
      if (!isSame(a.point.getMean(), b.point.getMean(), 10.0)) {
        @Nonnull final String diagnose = diagnose(cursor, monitor, a,
            b);
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
  }

  @Nonnull
  private String diagnose(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                          @Nonnull final LineSearchPoint a, @Nonnull final LineSearchPoint b) {
    assert a.point != null;
    final LineSearchPoint verifyA = cursor.step(a.point.rate, monitor);
    assert verifyA != null;
    assert verifyA.point != null;
    final boolean validA = isSame(a.point.getMean(), verifyA.point.getMean(), 1.0);
    monitor.log(RefString.format("Verify %s: %s (%s)", a.point.rate, verifyA.point.getMean(), validA));
    verifyA.freeRef();
    if (!validA) {
      DescribeOrientationWrapper.render(a.point.weights.addRef(), a.point.delta.addRef());
      cursor.freeRef();
      String temp_17_0014 = "Non-Reproducable Point Found: " + a.point.rate;
      a.freeRef();
      b.freeRef();
      return temp_17_0014;
    }
    assert b.point != null;
    final LineSearchPoint verifyB = cursor.step(b.point.rate, monitor);
    cursor.freeRef();
    assert verifyB != null;
    assert verifyB.point != null;
    final boolean validB = isSame(b.point.getMean(), verifyB.point.getMean(), 1.0);
    monitor.log(RefString.format("Verify %s: %s (%s)", b.point.rate, verifyB.point.getMean(), validB));
    verifyB.freeRef();
    if (validB) {
      a.freeRef();
      b.freeRef();
      return "Function Discontinuity Found";
    }
    a.freeRef();
    String temp_17_0016 = "Non-Reproducable Point Found: " + b.point.rate;
    b.freeRef();
    return temp_17_0016;
  }

  @Nullable
  private PointSample filter(@Nonnull final LineSearchCursor cursor, @Nonnull final PointSample point,
                             final TrainingMonitor monitor) {
    if (stepSize == 1.0) {
      cursor.freeRef();
      return point;
    } else {
      LineSearchPoint step = cursor.step(point.rate * stepSize, monitor);
      assert step != null;
      PointSample temp_17_0013 = step.point;
      step.freeRef();
      cursor.freeRef();
      point.freeRef();
      return temp_17_0013;
    }
  }

  private static class LocateInitialRightPoint extends ReferenceCountingBase {
    @Nonnull
    private final LineSearchCursor cursor;
    @Nonnull
    private final LineSearchPoint initialPoint;
    @Nonnull
    private final TrainingMonitor monitor;
    @Nonnull
    private final QuadraticSearch parent;
    @Nullable
    private LineSearchPoint thisPoint;
    private double thisX;

    public LocateInitialRightPoint(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor,
                                   @Nonnull final LineSearchPoint leftPoint, @Nonnull QuadraticSearch parent) {
      LineSearchCursor temp_17_0001 = cursor.addRef();
      this.cursor = temp_17_0001 == null ? null : temp_17_0001.addRef();
      if (null != temp_17_0001)
        temp_17_0001.freeRef();
      this.monitor = monitor;
      LineSearchPoint temp_17_0002 = leftPoint.addRef();
      initialPoint = temp_17_0002.addRef();
      temp_17_0002.freeRef();
      this.parent = parent;
      assert leftPoint.point != null;
      thisX = parent.getCurrentRate() > 0 ? parent.getCurrentRate()
          : Math.abs(leftPoint.point.getMean() * 1e-4 / leftPoint.derivative);
      leftPoint.freeRef();
      LineSearchPoint temp_17_0003 = cursor.step(thisX, monitor);
      thisPoint = temp_17_0003 == null ? null : temp_17_0003.addRef();
      if (null != temp_17_0003)
        temp_17_0003.freeRef();
      cursor.freeRef();
      assert initialPoint.point != null;
      assert thisPoint != null;
      assert thisPoint.point != null;
      monitor.log(RefString.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint,
          thisPoint.point.getMean() - initialPoint.point.getMean()));
    }

    @Nullable
    public LineSearchPoint getRightPoint() {
      return thisPoint == null ? null : thisPoint.addRef();
    }

    public double getRightX() {
      return thisX;
    }

    @Nullable
    public static @SuppressWarnings("unused")
    LocateInitialRightPoint[] addRefs(@Nullable LocateInitialRightPoint[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(LocateInitialRightPoint::addRef)
          .toArray((x) -> new LocateInitialRightPoint[x]);
    }

    @Nonnull
    public void apply() {
      assertAlive();
      @Nullable
      LineSearchPoint lastPoint = null;
      int loops = 0;
      try {
        while (true) {
          if (null != lastPoint)
            lastPoint.freeRef();
          lastPoint = thisPoint == null ? null : thisPoint.addRef();
          if (parent.isSame(cursor.addRef(), monitor,
              initialPoint.addRef(), thisPoint == null ? null : thisPoint.addRef())) {
            assert initialPoint.point != null;
            monitor.log(RefString.format("%s ~= %s", initialPoint.point.rate, thisX));
            return;
          } else {
            assert initialPoint.point != null;
            assert thisPoint.point != null;
            if (thisPoint.point.getMean() > initialPoint.point.getMean() && thisX > parent.minRate) {
              thisX = thisX / 13;
            } else if (thisPoint.derivative < parent.initialDerivFactor * thisPoint.derivative && thisX < parent.maxRate) {
              thisX = thisX * 7;
            } else {
              monitor.log(RefString.format("%s <= %s", thisPoint.point.getMean(), initialPoint.point.getMean()));
              return;
            }
          }

          LineSearchPoint temp_17_0004 = cursor.step(thisX, monitor);
          if (null != thisPoint)
            thisPoint.freeRef();
          thisPoint = temp_17_0004 == null ? null : temp_17_0004.addRef();
          if (null != temp_17_0004)
            temp_17_0004.freeRef();
          if (parent.isSame(cursor.addRef(), monitor,
              lastPoint == null ? null : lastPoint.addRef(), thisPoint == null ? null : thisPoint.addRef())) {
            assert lastPoint != null;
            assert lastPoint.point != null;
            monitor.log(RefString.format("%s ~= %s", lastPoint.point.rate, thisX));
            return;
          }
          assert thisPoint.point != null;
          monitor.log(RefString.format("F(%s) = %s, evalInputDelta = %s", thisX, thisPoint.addRef(),
              thisPoint.point.getMean() - initialPoint.point.getMean()));
          if (loops++ > 50) {
            monitor.log(RefString.format("Loops = %s", loops));
            return;
          }
        }
      } finally {
        if (null != lastPoint)
          lastPoint.freeRef();
      }
    }

    public void _free() {
      super._free();
      if (null != thisPoint)
        thisPoint.freeRef();
      thisPoint = null;
      initialPoint.freeRef();
      cursor.freeRef();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    LocateInitialRightPoint addRef() {
      return (LocateInitialRightPoint) super.addRef();
    }
  }
}
