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

package com.simiacryptus.mindseye.lang;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefArrays;

import java.util.Arrays;
import java.util.UUID;

public @RefAware
class MutableResult extends Result {

  public MutableResult(final Tensor... tensors) {
    this(RefArrays.stream(tensors).map(Tensor::getId).toArray(i -> new UUID[i]), tensors);
  }

  public MutableResult(UUID[] objectId, final Tensor... tensors) {
    super(new TensorArray(tensors), handler(tensors, objectId));
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  public static @SuppressWarnings("unused")
  MutableResult[] addRefs(MutableResult[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MutableResult::addRef)
        .toArray((x) -> new MutableResult[x]);
  }

  public static @SuppressWarnings("unused")
  MutableResult[][] addRefs(MutableResult[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MutableResult::addRefs)
        .toArray((x) -> new MutableResult[x][]);
  }

  private static Result.Accumulator handler(final Tensor[] tensors, UUID[] objectId) {
    return new Accumulator() {
      @Override
      public void accept(DeltaSet<UUID> buffer, TensorList delta) {
        for (int index = 0; index < delta.length(); index++) {
          buffer.get(objectId[index], tensors[index].getData()).addInPlace(delta.get(index).getData());
        }
      }
    };
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  MutableResult addRef() {
    return (MutableResult) super.addRef();
  }
}
