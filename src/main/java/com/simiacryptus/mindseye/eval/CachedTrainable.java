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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * This class represents a trainable that can be cached.
 *
 * @param history     The history of this trainable.
 * @param historySize The size of the history.
 * @param verbose     Whether or not this trainable is verbose.
 * @docgenVersion 9
 */
public class CachedTrainable<T extends Trainable> extends TrainableWrapper<T> {
  private static final Logger log = LoggerFactory.getLogger(CachedTrainable.class);

  private final RefList<PointSample> history = new RefArrayList<>();
  private int historySize = 3;
  private boolean verbose = true;

  /**
   * Instantiates a new Cached trainable.
   *
   * @param inner the inner
   */
  public CachedTrainable(final T inner) {
    super(inner);
  }

  /**
   * Returns the number of elements in the history.
   *
   * @docgenVersion 9
   */
  public int getHistorySize() {
    return historySize;
  }

  /**
   * Sets the history size.
   *
   * @param historySize the new history size
   * @docgenVersion 9
   */
  public void setHistorySize(int historySize) {
    this.historySize = historySize;
  }

  /**
   * Returns true if the log is verbose, false otherwise.
   *
   * @docgenVersion 9
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets the verbose flag to the specified value.
   *
   * @param verbose the new value of the verbose flag
   * @docgenVersion 9
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Returns a cached version of this trainable.
   *
   * @return a cached version of this trainable
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public CachedTrainable<? extends Trainable> cached() {
    return this.addRef();
  }

  /**
   * @Nonnull
   * @Override public PointSample measure(final TrainingMonitor monitor);
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    for (int i = 0; i < history.size(); i++) {
      PointSample result = history.get(i);
      try {
        if (!result.weights.isDifferent()) {
          if (isVerbose()) {
            log.info(RefString.format("Returning cached value; %s buffers unchanged since %s => %s", result.weights.size(),
                result.rate, result.getMean()));
          }
          return result.copyFull();
        }
      } finally {
        result.freeRef();
      }
    }
    final PointSample result = super.measure(monitor);
    history.add(result.copyFull());
    while (getHistorySize() < history.size()) {
      RefUtil.freeRef(history.remove(0));
    }
    return result;
  }

  /**
   * Reseeds the random number generator with the given seed.
   *
   * @param seed the seed to use
   * @return true if the random number generator was successfully reseeded, false otherwise
   * @docgenVersion 9
   */
  @Override
  public boolean reseed(final long seed) {
    history.clear();
    return super.reseed(seed);
  }

  /**
   * This method suppresses warnings for unused variables.
   * It frees up memory used by this class and its superclass,
   * as well as any history associated with this class.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    if (null != history)
      history.freeRef();
  }

  /**
   * @return a cached trainable object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  CachedTrainable<T> addRef() {
    return (CachedTrainable<T>) super.addRef();
  }
}
