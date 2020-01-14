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

public class StaticLearningRate implements LineSearchStrategy {

  private double minimumRate = 1e-12;
  private double rate = 1e-4;

  public StaticLearningRate(double rate) {
    this.rate = rate;
  }

  public double getMinimumRate() {
    return minimumRate;
  }

  @Nonnull
  public StaticLearningRate setMinimumRate(final double minimumRate) {
    this.minimumRate = minimumRate;
    return this;
  }

  public double getRate() {
    return rate;
  }

  @Nonnull
  public StaticLearningRate setRate(final double rate) {
    this.rate = rate;
    return this;
  }

  @Nullable
  @Override
  public PointSample step(@Nonnull final LineSearchCursor cursor, @Nonnull final TrainingMonitor monitor) {
    double thisRate = rate;
    final LineSearchPoint startPoint = cursor.step(0, monitor);
    assert startPoint != null;
    assert startPoint.point != null;
    final double startValue = startPoint.point.sum; // theta(0)
    @Nullable
    LineSearchPoint lastStep = null;
    try {
      while (true) {
        lastStep = cursor.step(thisRate, monitor);
        assert lastStep != null;
        assert lastStep.point != null;
        double lastValue = lastStep.point.sum;
        if (!Double.isFinite(lastValue)) {
          lastValue = Double.POSITIVE_INFINITY;
        }
        if (lastValue + startValue * 1e-15 > startValue) {
          monitor.log(RefString.format("Non-decreasing runStep. %s > %s at " + thisRate, lastValue, startValue));
          thisRate /= 2;
          if (thisRate < getMinimumRate()) {
            PointSample temp_30_0001 = startPoint.point;
            startPoint.freeRef();
            lastStep.freeRef();
            return temp_30_0001;
          }
        } else {
          startPoint.freeRef();
          PointSample temp_30_0002 = lastStep.point;
          lastStep.freeRef();
          return temp_30_0002;
        }
      }
    } finally {
      cursor.freeRef();
    }
  }
}
