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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

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
    this(ids(tensors), tensors);
  }

  /**
   * Instantiates a new Mutable result.
   *
   * @param objectId the object id
   * @param tensors  the tensors
   */
  public MutableResult(@Nonnull UUID[] objectId, @Nullable final Tensor... tensors) {
    super(new TensorArray(tensors), handler(RefUtil.addRef(tensors), objectId));
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  @NotNull
  private static UUID[] ids(@RefIgnore Tensor[] tensors) {
    UUID[] uuids = new UUID[tensors.length];
    for (int i = 0; i < uuids.length; i++) {
      uuids[i] = tensors[i].getId();
    }
    return uuids;
  }

  private static Result.Accumulator handler(final Tensor[] tensors, UUID[] objectId) {
    return new MutableAccumulator(tensors, objectId);
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  MutableResult addRef() {
    return (MutableResult) super.addRef();
  }

  @Override
  public void _free() {
    super._free();
  }

  private static class MutableAccumulator extends Accumulator {

    private final Tensor[] tensors;
    private final UUID[] objectId;

    /**
     * Instantiates a new Mutable accumulator.
     *
     * @param tensors  the tensors
     * @param objectId the object id
     */
    public MutableAccumulator(Tensor[] tensors, UUID[] objectId) {
      this.tensors = tensors;
      this.objectId = objectId;
    }

    @Override
    public void accept(@Nonnull DeltaSet<UUID> buffer, @Nonnull TensorList delta) {
      for (int index = 0; index < delta.length(); index++) {
        assert tensors != null;
        Delta<UUID> tensorDelta = buffer.get(objectId[index], tensors[index].addRef());
        assert tensorDelta != null;
        tensorDelta.addInPlace(delta.get(index));
        tensorDelta.freeRef();
      }
      delta.freeRef();
      buffer.freeRef();
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      if (null != tensors)
        RefUtil.freeRef(tensors);
      RefUtil.freeRef(objectId);
    }
  }
}
