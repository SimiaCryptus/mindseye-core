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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
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
        .mapToObj(i -> Tensor.fromJson(values.get(i), resources)).toArray(i -> new Tensor[i]);
    if (null != data)
      ReferenceCounting.freeRefs(data);
    data = Tensor.addRefs(temp_14_0001);
    if (null != temp_14_0001)
      ReferenceCounting.freeRefs(temp_14_0001);
  }

  public ValueLayer(final @Nonnull Tensor... data) {
    super();
    Tensor[] temp_14_0002 = RefArrays.copyOf(Tensor.addRefs(data), data.length);
    if (null != this.data)
      ReferenceCounting.freeRefs(this.data);
    this.data = Tensor.addRefs(temp_14_0002);
    if (null != temp_14_0002)
      ReferenceCounting.freeRefs(temp_14_0002);
    ReferenceCounting.freeRefs(data);
    this.frozen = true;
  }

  @Nullable
  public Tensor[] getData() {
    return Tensor.addRefs(data);
  }

  public void setData(final Tensor... data) {
    Tensor[] temp_14_0003 = Tensor.addRefs(data);
    if (null != this.data)
      ReferenceCounting.freeRefs(this.data);
    this.data = Tensor.addRefs(temp_14_0003);
    if (null != temp_14_0003)
      ReferenceCounting.freeRefs(temp_14_0003);
    if (null != data)
      ReferenceCounting.freeRefs(data);
  }

  @SuppressWarnings("unused")
  public static ValueLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new ValueLayer(json, rs);
  }

  public static @SuppressWarnings("unused") ValueLayer[] addRefs(ValueLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValueLayer::addRef).toArray((x) -> new ValueLayer[x]);
  }

  public static @SuppressWarnings("unused") ValueLayer[][] addRefs(ValueLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValueLayer::addRefs).toArray((x) -> new ValueLayer[x][]);
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... array) {
    assert 0 == array.length;
    ReferenceCounting.freeRefs(array);
    final ValueLayer valueLayer = ValueLayer.this.addRef();
    try {
      return new Result(new TensorArray(Tensor.addRefs(this.data)), new Result.Accumulator() {
        {
        }

        @Override
        public void accept(DeltaSet<UUID> buffer, TensorList data) {
          if (!ValueLayer.this.isFrozen()) {
            ValueLayer.this.assertAlive();
            assert (1 == valueLayer.data.length || valueLayer.data.length == data.length());
            for (int i = 0; i < data.length(); i++) {
              Tensor delta = data.get(i);
              Tensor value = valueLayer.data[i % valueLayer.data.length].addRef();
              Delta<UUID> temp_14_0007 = buffer.get(value.getId(), value.getData());
              RefUtil.freeRef(temp_14_0007.addInPlace(delta.getData()));
              if (null != temp_14_0007)
                temp_14_0007.freeRef();
              if (null != value)
                value.freeRef();
              if (null != delta)
                delta.freeRef();
            }
          }
          if (null != data)
            data.freeRef();
          if (null != buffer)
            buffer.freeRef();
        }

        public @SuppressWarnings("unused") void _free() {
        }
      }) {

        {
        }

        @Override
        public boolean isAlive() {
          return !valueLayer.isFrozen();
        }

        public void _free() {
        }
      };
    } finally {
      if (null != valueLayer)
        valueLayer.freeRef();
    }
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, @Nonnull DataSerializer dataSerializer) {
    @Nonnull
    final JsonObject json = super.getJsonStub();
    JsonArray values = new JsonArray();
    RefArrays.stream(Tensor.addRefs(data)).map(datum -> {
      JsonElement temp_14_0005 = datum.getJson(resources, dataSerializer);
      if (null != datum)
        datum.freeRef();
      return temp_14_0005;
    }).forEach(values::add);
    json.add("values", values);
    return json;
  }

  @Nonnull
  @Override
  public RefList<double[]> state() {
    return RefArrays.stream(Tensor.addRefs(data)).map(x -> {
      double[] temp_14_0006 = x.getData();
      if (null != x)
        x.freeRef();
      return temp_14_0006;
    }).collect(RefCollectors.toList());
  }

  public void _free() {
    if (null != data)
      ReferenceCounting.freeRefs(data);
    data = null;
  }

  public @Override @SuppressWarnings("unused") ValueLayer addRef() {
    return (ValueLayer) super.addRef();
  }

  public static class RefWrapper<T> {
    public final T obj;

    public RefWrapper(T obj) {
      this.obj = obj;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      RefWrapper<?> that = (RefWrapper<?>) o;
      return obj == that.obj;
    }

    @Override
    public int hashCode() {
      return com.simiacryptus.ref.wrappers.RefSystem.identityHashCode(obj);
    }
  }
}
