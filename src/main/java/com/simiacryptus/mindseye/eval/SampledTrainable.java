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
 * This is the SampledTrainable interface.
 *
 * @docgenVersion 9
 */
public interface SampledTrainable extends Trainable {
  /**
   * Returns the number of training examples.
   *
   * @docgenVersion 9
   */
  int getTrainingSize();

  /**
   * Sets the training size.
   *
   * @param trainingSize the new training size
   * @docgenVersion 9
   */
  void setTrainingSize(int trainingSize);

  /**
   * @return a new SampledCachedTrainable object
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  SampledCachedTrainable<? extends SampledTrainable> cached();

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * Add a reference to the sampled trainable.
   *
   * @return the sampled trainable
   * @docgenVersion 9
   */
  @Nonnull
  SampledTrainable addRef();
}
