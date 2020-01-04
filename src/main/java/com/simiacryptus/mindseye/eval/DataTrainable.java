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

import com.simiacryptus.mindseye.lang.Tensor;

import javax.annotation.Nonnull;
import java.util.List;
import com.simiacryptus.ref.wrappers.RefList;

public @com.simiacryptus.ref.lang.RefAware interface DataTrainable extends Trainable {
  Tensor[][] getData();

  @Nonnull
  Trainable setData(com.simiacryptus.ref.wrappers.RefList<Tensor[]> tensors);

  public void _free();

  public DataTrainable addRef();

  public static @SuppressWarnings("unused") DataTrainable[] addRefs(DataTrainable[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DataTrainable::addRef)
        .toArray((x) -> new DataTrainable[x]);
  }

  public static @SuppressWarnings("unused") DataTrainable[][] addRefs(DataTrainable[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DataTrainable::addRefs)
        .toArray((x) -> new DataTrainable[x][]);
  }
}
