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

public @com.simiacryptus.ref.lang.RefAware interface SampledTrainable extends Trainable {
  int getTrainingSize();

  @Nonnull
  void setTrainingSize(int trainingSize);

  @Nonnull
  @Override
  SampledCachedTrainable<? extends SampledTrainable> cached();

  public void _free();

  public SampledTrainable addRef();

  public static @SuppressWarnings("unused") SampledTrainable[] addRefs(SampledTrainable[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SampledTrainable::addRef)
        .toArray((x) -> new SampledTrainable[x]);
  }

  public static @SuppressWarnings("unused") SampledTrainable[][] addRefs(SampledTrainable[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SampledTrainable::addRefs)
        .toArray((x) -> new SampledTrainable[x][]);
  }
}
