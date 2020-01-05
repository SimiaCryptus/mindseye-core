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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.Arrays;

public abstract @RefAware
class TrainableBase extends ReferenceCountingBase implements Trainable {

  public static @SuppressWarnings("unused")
  TrainableBase[] addRefs(TrainableBase[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TrainableBase::addRef)
        .toArray((x) -> new TrainableBase[x]);
  }

  public static @SuppressWarnings("unused")
  TrainableBase[][] addRefs(TrainableBase[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TrainableBase::addRefs)
        .toArray((x) -> new TrainableBase[x][]);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  TrainableBase addRef() {
    return (TrainableBase) super.addRef();
  }

}
