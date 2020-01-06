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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public @RefAware
class ArmijoWolfeSearch implements LineSearchStrategy {

  private double absoluteTolerance = 1e-15;
  private double alpha = 1.0;
  private double alphaGrowth = Math.pow(10.0, Math.pow(3.0, -1.0));
  private double c1 = 1e-6;
  private double c2 = 0.9;
  private double maxAlpha = 1e8;
  private double minAlpha = 1e-15;
  private double relativeTolerance = 1e-2;
  private boolean strongWolfe = true;

  public double getAbsoluteTolerance() {
    return absoluteTolerance;
  }

  @Nonnull
  public ArmijoWolfeSearch setAbsoluteTolerance(final double absoluteTolerance) {
    this.absoluteTolerance = absoluteTolerance;
    return this;
  }

  public double getAlpha() {
    return alpha;
  }

  @Nonnull
  public ArmijoWolfeSearch setAlpha(final double alpha) {
    this.alpha = alpha;
    return this;
  }

  public double getAlphaGrowth() {
    return alphaGrowth;
  }

  @Nonnull
  public ArmijoWolfeSearch setAlphaGrowth(final double alphaGrowth) {
    this.alphaGrowth = alphaGrowth;
    return this;
  }

  public double getC1() {
    return c1;
  }

  @Nonnull
  public ArmijoWolfeSearch setC1(final double c1) {
    this.c1 = c1;
    return this;
  }

  public double getC2() {
    return c2;
  }

  @Nonnull
  public ArmijoWolfeSearch setC2(final double c2) {
    this.c2 = c2;
    return this;
  }

  public double getMaxAlpha() {
    return maxAlpha;
  }

  @Nonnull
  public ArmijoWolfeSearch setMaxAlpha(final double maxAlpha) {
    this.maxAlpha = maxAlpha;
    return this;
  }

  public double getMinAlpha() {
    return minAlpha;
  }

  @Nonnull
  public ArmijoWolfeSearch setMinAlpha(final double minAlpha) {
    this.minAlpha = minAlpha;
    return this;
  }

  public double getRelativeTolerance() {
    return relativeTolerance;
  }

  @Nonnull
  public ArmijoWolfeSearch setRelativeTolerance(final double relativeTolerance) {
    this.relativeTolerance = relativeTolerance;
    return this;
  }

  private boolean isAlphaValid() {
    return Double.isFinite(alpha) && 0 <= alpha;
  }

  public boolean isStrongWolfe() {
    return strongWolfe;
  }

  @Nonnull
  public ArmijoWolfeSearch setStrongWolfe(final boolean strongWolfe) {
    this.strongWolfe = strongWolfe;
    return this;
  }

  public void loosenMetaparameters() {
    c1 *= 0.2;
    c2 = Math.pow(c2, c2 < 1 ? 1.5 : 1 / 1.5);
    strongWolfe = false;
  }

  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    alpha = Math.min(maxAlpha, alpha * alphaGrowth); // Keep memory of alphaList from one iteration to next, but have a bias for growing the value
    double mu = 0;
    double nu = Double.POSITIVE_INFINITY;
    final LineSearchPoint startPoint = cursor.step(0, monitor);
    @Nullable
    LineSearchPoint lastStep = null;
    {
      final double startLineDeriv = startPoint.derivative; // theta'(0)
      final double startValue = startPoint.point.getMean(); // theta(0)
      if (0 <= startPoint.derivative) {
        monitor.log(RefString.format("th(0)=%s;dx=%s (ERROR: Starting derivative negative)", startValue, startLineDeriv));
        LineSearchPoint step = cursor.step(0, monitor);
        if (null != startPoint)
          startPoint.freeRef();
        if (null != lastStep)
          lastStep.freeRef();
        PointSample temp_39_0001 = step.point;
        if (null != step)
          step.freeRef();
        cursor.freeRef();
        return temp_39_0001;
      }
      monitor.log(RefString.format("th(0)=%s;dx=%s", startValue, startLineDeriv));
      int stepBias = 0;
      double bestAlpha = 0;
      double bestValue = startPoint.point.getMean();
      while (true) {
        if (!isAlphaValid()) {
          PointSample point = stepPoint(cursor == null ? null : cursor, monitor, bestAlpha);
          monitor.log(RefString.format("INVALID ALPHA (%s): th(%s)=%s", alpha, bestAlpha, point.getMean()));
          if (null != startPoint)
            startPoint.freeRef();
          if (null != lastStep)
            lastStep.freeRef();
          return point;
        }
        if (mu >= nu - absoluteTolerance) {
          loosenMetaparameters();
          PointSample point = stepPoint(cursor == null ? null : cursor, monitor, bestAlpha);
          monitor.log(RefString.format("mu >= nu (%s): th(%s)=%s", mu, bestAlpha, point.getMean()));
          if (null != startPoint)
            startPoint.freeRef();
          if (null != lastStep)
            lastStep.freeRef();
          return point;
        }
        if (nu - mu < nu * relativeTolerance) {
          loosenMetaparameters();
          PointSample point = stepPoint(cursor == null ? null : cursor, monitor, bestAlpha);
          monitor.log(RefString.format("mu ~= nu (%s): th(%s)=%s", mu, bestAlpha, point.getMean()));
          if (null != startPoint)
            startPoint.freeRef();
          if (null != lastStep)
            lastStep.freeRef();
          return point;
        }
        if (Math.abs(alpha) < minAlpha) {
          PointSample point = stepPoint(cursor == null ? null : cursor, monitor, bestAlpha);
          monitor.log(RefString.format("MIN ALPHA (%s): th(%s)=%s", alpha, bestAlpha, point.getMean()));
          alpha = minAlpha;
          if (null != startPoint)
            startPoint.freeRef();
          if (null != lastStep)
            lastStep.freeRef();
          return point;
        }
        if (Math.abs(alpha) > maxAlpha) {
          PointSample point = stepPoint(cursor == null ? null : cursor, monitor, bestAlpha);
          monitor.log(RefString.format("MAX ALPHA (%s): th(%s)=%s", alpha, bestAlpha, point.getMean()));
          alpha = maxAlpha;
          if (null != startPoint)
            startPoint.freeRef();
          if (null != lastStep)
            lastStep.freeRef();
          return point;
        }
        lastStep = cursor.step(alpha, monitor);
        double lastValue = lastStep.point.getMean();
        if (bestValue > lastValue) {
          bestAlpha = alpha;
          bestValue = lastValue;
        }
        if (!Double.isFinite(lastValue)) {
          lastValue = Double.POSITIVE_INFINITY;
        }
        if (lastValue > startValue + alpha * c1 * startLineDeriv) {
          // Value did not decrease (enough) - It is gauranteed to decrease given an infitefimal rate; the rate must be less than this; this is a new ceiling
          monitor.log(RefString.format("Armijo: th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue, lastStep.derivative,
              startValue - lastValue));
          nu = alpha;
          stepBias = Math.min(-1, stepBias - 1);
        } else if (isStrongWolfe() && lastStep.derivative > 0) {
          // If the slope is increasing, then we can go lower by choosing a lower rate; this is a new ceiling
          monitor.log(RefString.format("WOLF (strong): th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue,
              lastStep.derivative, startValue - lastValue));
          nu = alpha;
          stepBias = Math.min(-1, stepBias - 1);
        } else if (lastStep.derivative < c2 * startLineDeriv) {
          // Current slope decreases at no more than X - If it is still decreasing that fast, we know we want a rate of least this value; this is a new floor
          monitor.log(RefString.format("WOLFE (weak): th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue,
              lastStep.derivative, startValue - lastValue));
          mu = alpha;
          stepBias = Math.max(1, stepBias + 1);
        } else {
          monitor.log(RefString.format("END: th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue, lastStep.derivative,
              startValue - lastValue));
          return lastStep.point;
        }
        if (!Double.isFinite(nu)) {
          alpha = (1 + Math.abs(stepBias)) * alpha;
        } else if (0.0 == mu) {
          alpha = nu / (1 + Math.abs(stepBias));
        } else {
          alpha = (mu + nu) / 2;
        }
      }
    }
  }

  private PointSample stepPoint(@Nonnull LineSearchCursor cursor, TrainingMonitor monitor, double bestAlpha) {
    LineSearchPoint step = cursor.step(bestAlpha, monitor);
    cursor.freeRef();
    PointSample temp_39_0002 = step.point;
    if (null != step)
      step.freeRef();
    return temp_39_0002;
  }
}
