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

/**
 * This is the SampledCachedTrainable class.
 * It contains a private field called "seed".
 *
 * @docgenVersion 9
 */
public class SampledCachedTrainable<T extends SampledTrainable> extends CachedTrainable<T> implements SampledTrainable {

  private long seed;

  /**
   * Instantiates a new Sampled cached trainable.
   *
   * @param inner the inner
   */
  public SampledCachedTrainable(final T inner) {
    super(inner);
  }

  /**
   * Returns the training size.
   *
   * @docgenVersion 9
   */
  @Override
  public int getTrainingSize() {
    T temp_54_0002 = getInner();
    assert temp_54_0002 != null;
    int temp_54_0001 = temp_54_0002.getTrainingSize();
    temp_54_0002.freeRef();
    return temp_54_0001;
  }

  /**
   * @Override public void setTrainingSize(final int trainingSize) {
   * if (trainingSize != getTrainingSize()) {
   * T temp_54_0003 = getInner();
   * assert temp_54_0003 != null;
   * temp_54_0003.setTrainingSize(trainingSize);
   * temp_54_0003.freeRef();
   * reseed(seed);
   * }
   * }
   * @docgenVersion 9
   */
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

  /**
   * Returns a new SampledCachedTrainable that is a cached version of this SampledTrainable.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this.addRef());
  }

  /**
   * Reseeds this RNG with the given long seed.
   *
   * @param seed the new seed
   * @return true if reseeding succeeded, false otherwise
   * @docgenVersion 9
   */
  @Override
  public boolean reseed(final long seed) {
    this.seed = seed;
    return super.reseed(seed);
  }

  /**
   * This method is unused.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  /**
   * Returns a new reference to this object.
   *
   * @return a new reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SampledCachedTrainable<T> addRef() {
    return (SampledCachedTrainable<T>) super.addRef();
  }

}
