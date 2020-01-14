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
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public interface Trainable extends ReferenceCounting {
  @Nonnull
  Layer getLayer();

  @Nullable
  public static @SuppressWarnings("unused")
  Trainable[] addRefs(@Nullable Trainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Trainable::addRef).toArray((x) -> new Trainable[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  Trainable[][] addRefs(@Nullable Trainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Trainable::addRefs).toArray((x) -> new Trainable[x][]);
  }

  @Nonnull
  default CachedTrainable<? extends Trainable> cached() {
    return new CachedTrainable<>(this.addRef());
  }

  PointSample measure(TrainingMonitor monitor);

  default boolean reseed(final long seed) {
    return false;
  }

  public void _free();

  public Trainable addRef();

}
