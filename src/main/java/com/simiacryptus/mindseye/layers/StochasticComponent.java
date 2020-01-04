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

import java.util.Random;

public @com.simiacryptus.ref.lang.RefAware interface StochasticComponent extends Layer {
  ThreadLocal<Random> random = new ThreadLocal<Random>() {
    @Override
    protected Random initialValue() {
      return new Random();
    }
  };

  void shuffle(final long seed);

  void clearNoise();

  public void _free();

  public StochasticComponent addRef();

  public static @SuppressWarnings("unused") StochasticComponent[] addRefs(StochasticComponent[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(StochasticComponent::addRef)
        .toArray((x) -> new StochasticComponent[x]);
  }

  public static @SuppressWarnings("unused") StochasticComponent[][] addRefs(StochasticComponent[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(StochasticComponent::addRefs)
        .toArray((x) -> new StochasticComponent[x][]);
  }
}
