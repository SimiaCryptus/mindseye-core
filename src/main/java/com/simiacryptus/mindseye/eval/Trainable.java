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
 * The interface Trainable.
 */
public interface Trainable extends ReferenceCounting {
  /**
   * Gets layer.
   *
   * @return the layer
   */
  Layer getLayer();

  /**
   * Sets data.
   *
   * @param tensors the tensors
   */
  default void setData(RefList<Tensor[]> tensors) {
    tensors.freeRef();
  }

  /**
   * Cached cached trainable.
   *
   * @return the cached trainable
   */
  @Nonnull
  default CachedTrainable<? extends Trainable> cached() {
    return new CachedTrainable<>(this.addRef());
  }

  /**
   * Measure point sample.
   *
   * @param monitor the monitor
   * @return the point sample
   */
  PointSample measure(TrainingMonitor monitor);

  /**
   * Reseed boolean.
   *
   * @param seed the seed
   * @return the boolean
   */
  default boolean reseed(final long seed) {
    return false;
  }

  Trainable addRef();

}
