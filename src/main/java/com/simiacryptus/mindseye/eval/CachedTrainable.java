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
import com.simiacryptus.mindseye.lang.State;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

public @RefAware
class CachedTrainable<T extends Trainable> extends TrainableWrapper<T> {
  private static final Logger log = LoggerFactory.getLogger(CachedTrainable.class);

  private final RefList<PointSample> history = new RefArrayList<>();
  private int historySize = 3;
  private boolean verbose = true;

  public CachedTrainable(final T inner) {
    super(inner);
    if (null != inner)
      inner.freeRef();
  }

  public int getHistorySize() {
    return historySize;
  }

  @Nonnull
  public CachedTrainable<T> setHistorySize(final int historySize) {
    this.historySize = historySize;
    return this.addRef();
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Nonnull
  public CachedTrainable<T> setVerbose(final boolean verbose) {
    this.verbose = verbose;
    return this.addRef();
  }

  public static @SuppressWarnings("unused")
  CachedTrainable[] addRefs(CachedTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(CachedTrainable::addRef)
        .toArray((x) -> new CachedTrainable[x]);
  }

  public static @SuppressWarnings("unused")
  CachedTrainable[][] addRefs(CachedTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(CachedTrainable::addRefs)
        .toArray((x) -> new CachedTrainable[x][]);
  }

  @Nonnull
  @Override
  public CachedTrainable<? extends Trainable> cached() {
    return this.addRef();
  }

  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    for (@Nonnull final PointSample result : history) {
      if (!result.weights.isDifferent()) {
        if (isVerbose()) {
          RefMap<UUID, State<UUID>> temp_53_0001 = result.weights
              .getMap();
          log.info(String.format("Returning cached value; %s buffers unchanged since %s => %s", temp_53_0001.size(),
              result.rate, result.getMean()));
          if (null != temp_53_0001)
            temp_53_0001.freeRef();
        }
        return result.copyFull();
      }
    }
    final PointSample result = super.measure(monitor).addRef();
    history.add(result.copyFull());
    while (getHistorySize() < history.size()) {
      RefUtil.freeRef(history.remove(0));
    }
    return result;
  }

  @Override
  public boolean reseed(final long seed) {
    history.clear();
    return super.reseed(seed);
  }

  public @SuppressWarnings("unused")
  void _free() {
    if (null != history)
      history.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  CachedTrainable<T> addRef() {
    return (CachedTrainable<T>) super.addRef();
  }
}
