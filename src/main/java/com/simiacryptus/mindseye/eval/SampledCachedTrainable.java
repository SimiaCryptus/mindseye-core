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

import javax.annotation.Nonnull;

public @com.simiacryptus.ref.lang.RefAware class SampledCachedTrainable<T extends SampledTrainable>
    extends CachedTrainable<T> implements SampledTrainable {

  private long seed;

  public SampledCachedTrainable(final T inner) {
    super(inner);
  }

  @Override
  public int getTrainingSize() {
    return getInner().getTrainingSize();
  }

  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this);
  }

  @Override
  public boolean reseed(final long seed) {
    this.seed = seed;
    return super.reseed(seed);
  }

  @Nonnull
  @Override
  public void setTrainingSize(final int trainingSize) {
    if (trainingSize != getTrainingSize()) {
      getInner().setTrainingSize(trainingSize);
      reseed(seed);
    }
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") SampledCachedTrainable<T> addRef() {
    return (SampledCachedTrainable<T>) super.addRef();
  }

  public static @SuppressWarnings("unused") SampledCachedTrainable[] addRefs(SampledCachedTrainable[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SampledCachedTrainable::addRef)
        .toArray((x) -> new SampledCachedTrainable[x]);
  }

  public static @SuppressWarnings("unused") SampledCachedTrainable[][] addRefs(SampledCachedTrainable[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SampledCachedTrainable::addRefs)
        .toArray((x) -> new SampledCachedTrainable[x][]);
  }

}
