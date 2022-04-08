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

package com.simiacryptus.mindseye.layers;

import com.simiacryptus.mindseye.lang.Layer;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * This is the StochasticComponent interface.
 * It contains a ThreadLocal variable called "random" which is used to generate random numbers.
 *
 * @docgenVersion 9
 */
public interface StochasticComponent extends Layer {
  /**
   * The constant random.
   */
  ThreadLocal<Random> random = new ThreadLocal<Random>() {
    /**
     * Returns a random initial value.
     *
     *   @docgenVersion 9
     */
    @Nonnull
    @Override
    protected Random initialValue() {
      return new Random();
    }
  };

  /**
   * This function shuffles the deck of cards.
   *
   * @docgenVersion 9
   */
  void shuffle(final long seed);

  /**
   * Clears the noise from the signal.
   *
   * @docgenVersion 9
   */
  void clearNoise();

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * Adds a reference to the StochasticComponent.
   *
   * @docgenVersion 9
   */
  @Nonnull
  StochasticComponent addRef();
}
