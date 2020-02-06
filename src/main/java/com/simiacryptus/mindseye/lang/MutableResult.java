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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class MutableResult extends Result {

  public MutableResult(final Tensor... tensors) {
    this(RefArrays.stream(RefUtil.addRefs(tensors)).map(tensor -> {
      UUID id = tensor.getId();
      tensor.freeRef();
      return id;
    }).toArray(UUID[]::new), tensors);
  }

  public MutableResult(@Nonnull UUID[] objectId, @Nullable final Tensor... tensors) {
    super(new TensorArray(tensors), handler(RefUtil.addRefs(tensors), objectId));
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  @Nonnull
  private static Result.Accumulator handler(@Nullable final Tensor[] tensors, @Nonnull UUID[] objectId) {
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

    public MutableAccumulator(Tensor[] tensors, UUID[] objectId) {
      this.tensors = tensors;
      this.objectId = objectId;
    }

    @Override
    public void accept(@Nonnull DeltaSet<UUID> buffer, @Nonnull TensorList delta) {
      for (int index = 0; index < delta.length(); index++) {
        assert tensors != null;
        Delta<UUID> temp_50_0002 = buffer.get(objectId[index], tensors[index].getData());
        Tensor temp_50_0003 = delta.get(index);
        assert temp_50_0002 != null;
        temp_50_0002.addInPlace(temp_50_0003.getData());
        temp_50_0003.freeRef();
        temp_50_0002.freeRef();
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
