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
 * The interface Sampled trainable.
 */
public interface SampledTrainable extends Trainable {
  /**
   * Gets training size.
   *
   * @return the training size
   */
  int getTrainingSize();

  /**
   * Sets training size.
   *
   * @param trainingSize the training size
   */
  void setTrainingSize(int trainingSize);

  @Nonnull
  @Override
  SampledCachedTrainable<? extends SampledTrainable> cached();

  /**
   * Free.
   */
  void _free();

  @Nonnull
  SampledTrainable addRef();
}
