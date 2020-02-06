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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("serial")
public class ValueLayer extends LayerBase {

  @Nullable
  private Tensor[] data;

  protected ValueLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> resources) {
    super(json);
    JsonArray values = json.getAsJsonArray("values");
    Tensor[] temp_14_0001 = RefIntStream.range(0, values.size())
        .mapToObj(i -> Tensor.fromJson(values.get(i), resources)).toArray(Tensor[]::new);
    if (null != data)
      RefUtil.freeRef(data);
    data = RefUtil.addRefs(temp_14_0001);
    RefUtil.freeRef(temp_14_0001);
  }

  public ValueLayer(final @Nonnull Tensor... data) {
    super();
    Tensor[] temp_14_0002 = RefArrays.copyOf(RefUtil.addRefs(data), data.length);
    if (null != this.data)
      RefUtil.freeRef(this.data);
    this.data = RefUtil.addRefs(temp_14_0002);
    RefUtil.freeRef(temp_14_0002);
    RefUtil.freeRef(data);
    this.frozen = true;
  }

  @Nullable
  public Tensor[] getData() {
    return RefUtil.addRefs(data);
  }

  public void setData(@Nullable final Tensor... data) {
    Tensor[] temp_14_0003 = RefUtil.addRefs(data);
    if (null != this.data)
      RefUtil.freeRef(this.data);
    this.data = RefUtil.addRefs(temp_14_0003);
    if (null != temp_14_0003)
      RefUtil.freeRef(temp_14_0003);
    if (null != data)
      RefUtil.freeRef(data);
  }

  @Nonnull
  @SuppressWarnings("unused")
  public static ValueLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new ValueLayer(json, rs);
  }


  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... array) {
    assert 0 == array.length;
    RefUtil.freeRef(array);
    final ValueLayer valueLayer = ValueLayer.this.addRef();
    try {
      Result.Accumulator accumulator = new Result.Accumulator() {
        {
          valueLayer.addRef();
        }

        @Override
        public void accept(@Nonnull DeltaSet<UUID> buffer, @Nonnull TensorList data) {
          if (!ValueLayer.this.isFrozen()) {
            ValueLayer.this.assertAlive();
            assert valueLayer.data != null;
            assert 1 == valueLayer.data.length || valueLayer.data.length == data.length();
            for (int i = 0; i < data.length(); i++) {
              Tensor delta = data.get(i);
              Tensor value = valueLayer.data[i % valueLayer.data.length].addRef();
              Delta<UUID> temp_14_0007 = buffer.get(value.getId(), value.getData());
              assert temp_14_0007 != null;
              temp_14_0007.addInPlace(delta.getData());
              temp_14_0007.freeRef();
              value.freeRef();
              delta.freeRef();
            }
          }
          data.freeRef();
          buffer.freeRef();
        }

        public @SuppressWarnings("unused")
        void _free() {
          super._free();
          valueLayer.freeRef();
        }
      };
      TensorArray data = new TensorArray(RefUtil.addRefs(this.data));
      return new Result(data, accumulator) {
        {
          valueLayer.addRef();
        }
        @Override
        public boolean isAlive() {
          return !valueLayer.isFrozen();
        }

        @Override
        public void _free() {
          valueLayer.freeRef();
          super._free();
        }
      };
    } finally {
      valueLayer.freeRef();
    }
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, @Nonnull DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJsonStub();
    JsonArray values = new JsonArray();
    RefArrays.stream(RefUtil.addRefs(data)).map(datum -> {
      JsonElement temp_14_0005 = datum.getJson(resources, dataSerializer);
      datum.freeRef();
      return temp_14_0005;
    }).forEach(values::add);
    json.add("values", values);
    return json;
  }

  @Nonnull
  @Override
  public RefList<double[]> state() {
    return RefArrays.stream(RefUtil.addRefs(data)).map(x -> {
      double[] temp_14_0006 = x.getData();
      x.freeRef();
      return temp_14_0006;
    }).collect(RefCollectors.toList());
  }

  public void _free() {
    super._free();
    if (null != data)
      RefUtil.freeRef(data);
    data = null;
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ValueLayer addRef() {
    return (ValueLayer) super.addRef();
  }

  public static class RefWrapper<T> {
    public final T obj;

    public RefWrapper(T obj) {
      this.obj = obj;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      RefWrapper<?> that = (RefWrapper<?>) o;
      return obj == that.obj;
    }

    @Override
    public int hashCode() {
      return RefSystem.identityHashCode(obj);
    }
  }
}
