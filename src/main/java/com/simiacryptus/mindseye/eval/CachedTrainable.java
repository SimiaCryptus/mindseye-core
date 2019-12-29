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

package com.simiacryptus.mindseye.eval;

import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CachedTrainable<T extends Trainable> extends TrainableWrapper<T> {
  private static final Logger log = LoggerFactory.getLogger(CachedTrainable.class);

  private final List<PointSample> history = new ArrayList<>();
  private int historySize = 3;
  private boolean verbose = true;

  public CachedTrainable(final T inner) {
    super(inner);
  }

  public int getHistorySize() {
    return historySize;
  }

  @Nonnull
  public CachedTrainable<T> setHistorySize(final int historySize) {
    this.historySize = historySize;
    return this;
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Nonnull
  public CachedTrainable<T> setVerbose(final boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  @Nonnull
  @Override
  public CachedTrainable<? extends Trainable> cached() {
    return this;
  }

  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    for (@Nonnull final PointSample result : history) {
      if (!result.weights.isDifferent()) {
        if (isVerbose()) {
          log.info(String.format("Returning cached value; %s buffers unchanged since %s => %s",
              result.weights.getMap().size(), result.rate, result.getMean()));
        }
        return result.copyFull();
      }
    }
    final PointSample result = super.measure(monitor);
    history.add(result.copyFull());
    while (getHistorySize() < history.size()) {
      history.remove(0);
    }
    return result;
  }

  @Override
  public boolean reseed(final long seed) {
    history.clear();
    return super.reseed(seed);
  }
}
