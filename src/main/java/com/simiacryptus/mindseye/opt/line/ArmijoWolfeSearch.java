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
 * ArmijoWolfeSearch class
 *
 * @param absoluteTolerance the absolute tolerance
 * @param alpha             the alpha
 * @param alphaGrowth       the alpha growth
 * @param c1                the c1
 * @param c2                the c2
 * @param maxAlpha          the max alpha
 * @param minAlpha          the min alpha
 * @param relativeTolerance the relative tolerance
 * @param strongWolfe       the strong wolfe
 * @docgenVersion 9
 */
public class ArmijoWolfeSearch implements LineSearchStrategy {

  private double absoluteTolerance = 1e-15;
  private double alpha = 1.0;
  private double alphaGrowth = Math.pow(10.0, Math.pow(3.0, -1.0));
  private double c1 = 1e-6;
  private double c2 = 0.9;
  private double maxAlpha = 1e8;
  private double minAlpha = 1e-15;
  private double relativeTolerance = 1e-2;
  private boolean strongWolfe = true;

  /**
   * Returns the absolute tolerance.
   *
   * @docgenVersion 9
   */
  public double getAbsoluteTolerance() {
    return absoluteTolerance;
  }

  /**
   * Sets the absolute tolerance for the line search.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setAbsoluteTolerance(final double absoluteTolerance) {
    this.absoluteTolerance = absoluteTolerance;
    return this;
  }

  /**
   * Returns the value of the 'alpha' property.
   *
   * @docgenVersion 9
   */
  public double getAlpha() {
    return alpha;
  }

  /**
   * ArmijoWolfeSearch sets the value of alpha.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setAlpha(final double alpha) {
    this.alpha = alpha;
    return this;
  }

  /**
   * Returns the alpha growth.
   *
   * @docgenVersion 9
   */
  public double getAlphaGrowth() {
    return alphaGrowth;
  }

  /**
   * ArmijoWolfeSearch sets the alpha growth.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setAlphaGrowth(final double alphaGrowth) {
    this.alphaGrowth = alphaGrowth;
    return this;
  }

  /**
   * Returns the value of c1.
   *
   * @docgenVersion 9
   */
  public double getC1() {
    return c1;
  }

  /**
   * Sets the c1 parameter for the ArmijoWolfeSearch algorithm.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setC1(final double c1) {
    this.c1 = c1;
    return this;
  }

  /**
   * Returns the value of c2.
   *
   * @docgenVersion 9
   */
  public double getC2() {
    return c2;
  }

  /**
   * Sets the c2 parameter for the ArmijoWolfeSearch algorithm.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setC2(final double c2) {
    this.c2 = c2;
    return this;
  }

  /**
   * Returns the maximum value of the alpha channel.
   *
   * @docgenVersion 9
   */
  public double getMaxAlpha() {
    return maxAlpha;
  }

  /**
   * ArmijoWolfeSearch sets the maximum value for alpha.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setMaxAlpha(final double maxAlpha) {
    this.maxAlpha = maxAlpha;
    return this;
  }

  /**
   * Returns the minimum value of the alpha field.
   *
   * @docgenVersion 9
   */
  public double getMinAlpha() {
    return minAlpha;
  }

  /**
   * Sets the minimum value for the alpha parameter in the Armijo-Wolfe line search algorithm.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setMinAlpha(final double minAlpha) {
    this.minAlpha = minAlpha;
    return this;
  }

  /**
   * Returns the relative tolerance.
   *
   * @docgenVersion 9
   */
  public double getRelativeTolerance() {
    return relativeTolerance;
  }

  /**
   * Sets the relative tolerance for the line search algorithm.
   *
   * @param tol the new relative tolerance
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setRelativeTolerance(final double relativeTolerance) {
    this.relativeTolerance = relativeTolerance;
    return this;
  }

  /**
   * Checks if the input is a valid alphabetical character.
   *
   * @return true if the input is a valid alphabetical character, false otherwise
   * @docgenVersion 9
   */
  private boolean isAlphaValid() {
    return Double.isFinite(alpha) && 0 <= alpha;
  }

  /**
   * Checks if the wolf is strong.
   *
   * @return true if the wolf is strong, false otherwise
   * @docgenVersion 9
   */
  public boolean isStrongWolfe() {
    return strongWolfe;
  }

  /**
   * Sets the strong Wolfe condition.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ArmijoWolfeSearch setStrongWolfe(final boolean strongWolfe) {
    this.strongWolfe = strongWolfe;
    return this;
  }

  /**
   * This function loosens the metaparameters.
   *
   * @docgenVersion 9
   */
  public void loosenMetaparameters() {
    c1 *= 0.2;
    c2 = Math.pow(c2, c2 < 1 ? 1.5 : 1 / 1.5);
    strongWolfe = false;
  }

  /**
   * Returns the next step in the PointSample.
   *
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    alpha = Math.min(maxAlpha, alpha * alphaGrowth); // Keep memory of alphaList from one iteration to next, but have a bias for growing the value
    double mu = 0;
    double nu = Double.POSITIVE_INFINITY;
    final LineSearchPoint startPoint = cursor.step(0, monitor);
    assert startPoint != null;
    final double startLineDeriv = startPoint.derivative; // theta'(0)
    final double startValue = startPoint.getPointMean(); // theta(0)
    if (0 <= startPoint.derivative) {
      monitor.log(RefString.format("th(0)=%s;dx=%s (ERROR: Starting derivative negative)", startValue, startLineDeriv));
      LineSearchPoint step = cursor.step(0, monitor);
      startPoint.freeRef();
      assert step != null;
      PointSample point = step.getPoint();
      step.freeRef();
      cursor.freeRef();
      return point;
    }
    monitor.log(RefString.format("th(0)=%s;dx=%s", startValue, startLineDeriv));
    int stepBias = 0;
    double bestAlpha = 0;
    double bestValue = startPoint.getPointMean();
    @Nullable LineSearchPoint lastStep = null;
    try {
      while (true) {
        if (!isAlphaValid()) {
          PointSample point = stepPoint(cursor.addRef(), monitor, bestAlpha);
          assert point != null;
          monitor.log(RefString.format("INVALID ALPHA (%s): th(%s)=%s", alpha, bestAlpha, point.getMean()));
          return point;
        }
        if (mu >= nu - absoluteTolerance) {
          loosenMetaparameters();
          PointSample point = stepPoint(cursor.addRef(), monitor, bestAlpha);
          assert point != null;
          monitor.log(RefString.format("mu >= nu (%s): th(%s)=%s", mu, bestAlpha, point.getMean()));
          return point;
        }
        if (nu - mu < nu * relativeTolerance) {
          loosenMetaparameters();
          PointSample point = stepPoint(cursor.addRef(), monitor, bestAlpha);
          assert point != null;
          monitor.log(RefString.format("mu ~= nu (%s): th(%s)=%s", mu, bestAlpha, point.getMean()));
          return point;
        }
        if (Math.abs(alpha) < minAlpha) {
          PointSample point = stepPoint(cursor.addRef(), monitor, bestAlpha);
          assert point != null;
          monitor.log(RefString.format("MIN ALPHA (%s): th(%s)=%s", alpha, bestAlpha, point.getMean()));
          alpha = minAlpha;
          return point;
        }
        if (Math.abs(alpha) > maxAlpha) {
          PointSample point = stepPoint(cursor.addRef(), monitor, bestAlpha);
          assert point != null;
          monitor.log(RefString.format("MAX ALPHA (%s): th(%s)=%s", alpha, bestAlpha, point.getMean()));
          alpha = maxAlpha;
          return point;
        }
        LineSearchPoint nextStep = cursor.step(alpha, monitor);
        if (null != lastStep) lastStep.freeRef();
        lastStep = nextStep;
        assert lastStep != null;
        double lastValue = lastStep.getPointMean();
        if (bestValue > lastValue) {
          bestAlpha = alpha;
          bestValue = lastValue;
        }
        if (!Double.isFinite(lastValue)) {
          lastValue = Double.POSITIVE_INFINITY;
        }
        double evalInputDelta = startValue - lastValue;
        if (evalInputDelta == 0.0) {
          monitor.log(RefString.format("END: th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue, lastStep.derivative,
              evalInputDelta));
          return lastStep.getPoint();
        } else if (lastValue > startValue + alpha * c1 * startLineDeriv) {
          // Value did not decrease (enough) - It is gauranteed to decrease given an infitefimal rate; the rate must be less than this; this is a new ceiling
          monitor.log(RefString.format("Armijo: th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue,
              lastStep.derivative, evalInputDelta));
          nu = alpha;
          stepBias = Math.min(-1, stepBias - 1);
        } else if (isStrongWolfe() && lastStep.derivative > 0) {
          // If the slope is increasing, then we can go lower by choosing a lower rate; this is a new ceiling
          monitor.log(RefString.format("WOLF (strong): th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue,
              lastStep.derivative, evalInputDelta));
          nu = alpha;
          stepBias = Math.min(-1, stepBias - 1);
        } else if (lastStep.derivative < c2 * startLineDeriv) {
          // Current slope decreases at no more than X - If it is still decreasing that fast, we know we want a rate of least this value; this is a new floor
          monitor.log(RefString.format("WOLFE (weak): th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue,
              lastStep.derivative, evalInputDelta));
          mu = alpha;
          stepBias = Math.max(1, stepBias + 1);
        } else {
          monitor.log(RefString.format("END: th(%s)=%s; dx=%s evalInputDelta=%s", alpha, lastValue, lastStep.derivative,
              evalInputDelta));
          return lastStep.getPoint();
        }
        if (!Double.isFinite(nu)) {
          alpha = (1 + Math.abs(stepBias)) * alpha;
        } else if (0.0 == mu) {
          alpha = nu / (1 + Math.abs(stepBias));
        } else {
          alpha = (mu + nu) / 2;
        }
      }
    } finally {
      if (null != lastStep)
        lastStep.freeRef();
      cursor.freeRef();
      startPoint.freeRef();
    }
  }

  /**
   * Returns the next point in the sample.
   *
   * @docgenVersion 9
   */
  @Nullable
  private PointSample stepPoint(@Nonnull LineSearchCursor cursor, TrainingMonitor monitor, double bestAlpha) {
    LineSearchPoint step = cursor.step(bestAlpha, monitor);
    cursor.freeRef();
    assert step != null;
    PointSample temp_39_0002 = step.getPoint();
    step.freeRef();
    return temp_39_0002;
  }
}
