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
import javax.annotation.Nullable;
import java.util.Arrays;

public class SampledCachedTrainable<T extends SampledTrainable> extends CachedTrainable<T> implements SampledTrainable {

  private long seed;

  public SampledCachedTrainable(final T inner) {
    super(inner);
  }

  @Override
  public int getTrainingSize() {
    T temp_54_0002 = getInner();
    assert temp_54_0002 != null;
    int temp_54_0001 = temp_54_0002.getTrainingSize();
    temp_54_0002.freeRef();
    return temp_54_0001;
  }

  @Nonnull
  @Override
  public void setTrainingSize(final int trainingSize) {
    if (trainingSize != getTrainingSize()) {
      T temp_54_0003 = getInner();
      assert temp_54_0003 != null;
      temp_54_0003.setTrainingSize(trainingSize);
      temp_54_0003.freeRef();
      reseed(seed);
    }
  }

  @Nullable
  public static @SuppressWarnings("unused")
  SampledCachedTrainable[] addRefs(@Nullable SampledCachedTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SampledCachedTrainable::addRef)
        .toArray((x) -> new SampledCachedTrainable[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  SampledCachedTrainable[][] addRefs(@Nullable SampledCachedTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SampledCachedTrainable::addRefs)
        .toArray((x) -> new SampledCachedTrainable[x][]);
  }

  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this.addRef());
  }

  @Override
  public boolean reseed(final long seed) {
    this.seed = seed;
    return super.reseed(seed);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SampledCachedTrainable<T> addRef() {
    return (SampledCachedTrainable<T>) super.addRef();
  }

}
