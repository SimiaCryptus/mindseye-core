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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;

import java.util.Arrays;
import java.util.UUID;

public class MutableResult extends Result {

  public MutableResult(final Tensor... tensors) {
    this(RefArrays.stream(Tensor.addRefs(tensors)).map(Tensor::getId).toArray(i -> new UUID[i]), tensors);
  }

  public MutableResult(UUID[] objectId, final Tensor... tensors) {
    super(new TensorArray(Tensor.addRefs(tensors)), handler(Tensor.addRefs(tensors), objectId));
    if (null != tensors)
      ReferenceCounting.freeRefs(tensors);
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  public static @SuppressWarnings("unused") MutableResult[] addRefs(MutableResult[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MutableResult::addRef)
        .toArray((x) -> new MutableResult[x]);
  }

  public static @SuppressWarnings("unused") MutableResult[][] addRefs(MutableResult[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MutableResult::addRefs)
        .toArray((x) -> new MutableResult[x][]);
  }

  private static Result.Accumulator handler(final Tensor[] tensors, UUID[] objectId) {
    try {
      return new Accumulator() {
        {
          Tensor.addRefs(tensors);
        }

        @Override
        public void accept(DeltaSet<UUID> buffer, TensorList delta) {
          for (int index = 0; index < delta.length(); index++) {
            Delta<UUID> temp_50_0002 = buffer.get(objectId[index], tensors[index].getData());
            Tensor temp_50_0003 = delta.get(index);
            RefUtil.freeRef(temp_50_0002.addInPlace(temp_50_0003.getData()));
            if (null != temp_50_0003)
              temp_50_0003.freeRef();
            if (null != temp_50_0002)
              temp_50_0002.freeRef();
          }
          if (null != delta)
            delta.freeRef();
          if (null != buffer)
            buffer.freeRef();
        }

        public @SuppressWarnings("unused") void _free() {
          if (null != tensors)
            ReferenceCounting.freeRefs(tensors);
          RefUtil.freeRefs(objectId);
        }
      };
    } finally {
      if (null != tensors)
        ReferenceCounting.freeRefs(tensors);
    }
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") MutableResult addRef() {
    return (MutableResult) super.addRef();
  }
}
