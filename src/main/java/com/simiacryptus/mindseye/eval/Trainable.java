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

import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;

/**
 * This is the Trainable interface.
 *
 * @docgenVersion 9
 */
public interface Trainable extends ReferenceCounting {
  /**
   * Returns the layer of the object.
   *
   * @docgenVersion 9
   */
  Layer getLayer();

  /**
   * Sets the data.
   *
   * @docgenVersion 9
   */
  default void setData(RefList<Tensor[]> tensors) {
    tensors.freeRef();
  }

  /**
   * Returns a cached trainable.
   *
   * @docgenVersion 9
   */
  @Nonnull
  default CachedTrainable<? extends Trainable> cached() {
    return new CachedTrainable<>(this.addRef());
  }

  /**
   * Returns a PointSample object.
   *
   * @docgenVersion 9
   */
  PointSample measure(TrainingMonitor monitor);

  /**
   * Reseeds the random number generator with the given seed.
   *
   * @param seed the seed
   * @return true if the random number generator was reseeded, false otherwise
   * @docgenVersion 9
   */
  default boolean reseed(final long seed) {
    return false;
  }

  /**
   * Adds a reference to the Trainable.
   *
   * @docgenVersion 9
   */
  default Trainable addRef() {
    return this;
  }

}
