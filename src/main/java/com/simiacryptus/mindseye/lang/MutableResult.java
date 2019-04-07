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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * The type Mutable result.
 */
public class MutableResult extends Result {

  /**
   * Instantiates a new Mutable result.
   *
   * @param tensors the tensors
   */
  public MutableResult(final Tensor... tensors) {
    this(Arrays.stream(tensors).map(Tensor::getId).toArray(i -> new UUID[i]), tensors);
  }

  /**
   * Instantiates a new Mutable result.
   *
   * @param objectId
   * @param tensors  the tensors
   */
  public MutableResult(UUID[] objectId, final Tensor... tensors) {
    super(TensorArray.create(tensors), handler(tensors, objectId));
  }

  private static BiConsumer<DeltaSet<UUID>, TensorList> handler(final Tensor[] tensors, UUID[] objectId) {
    return (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList delta) -> {
      for (int index = 0; index < delta.length(); index++) {
        buffer.get(objectId[index], tensors[index].getData())
            .addInPlaceAndFree(delta.get(index)).freeRef();
      }
      delta.freeRef();
    };
  }


  @Override
  public boolean isAlive() {
    return true;
  }
}
