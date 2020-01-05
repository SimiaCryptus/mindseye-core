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
import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefAware;
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
public @RefAware
class ValueLayer extends LayerBase {

  @Nullable
  private Tensor[] data;

  protected ValueLayer(@Nonnull final JsonObject json,
                       Map<CharSequence, byte[]> resources) {
    super(json);
    JsonArray values = json.getAsJsonArray("values");
    data = RefIntStream.range(0, values.size())
        .mapToObj(i -> Tensor.fromJson(values.get(i), resources)).toArray(i -> new Tensor[i]);
  }

  public ValueLayer(final @Nonnull Tensor... data) {
    super();
    this.data = RefArrays.copyOf(data, data.length);
    this.frozen = true;
  }

  @Nullable
  public Tensor[] getData() {
    return data;
  }

  public void setData(final Tensor... data) {
    this.data = data;
  }

  @SuppressWarnings("unused")
  public static ValueLayer fromJson(@Nonnull final JsonObject json,
                                    Map<CharSequence, byte[]> rs) {
    return new ValueLayer(json, rs);
  }

  public static @SuppressWarnings("unused")
  ValueLayer[] addRefs(ValueLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValueLayer::addRef)
        .toArray((x) -> new ValueLayer[x]);
  }

  public static @SuppressWarnings("unused")
  ValueLayer[][] addRefs(ValueLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(ValueLayer::addRefs)
        .toArray((x) -> new ValueLayer[x][]);
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... array) {
    assert 0 == array.length;
    return new Result(new TensorArray(this.data),
        (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList data) -> {
          if (!isFrozen()) {
            assertAlive();
            assert (1 == ValueLayer.this.data.length || ValueLayer.this.data.length == data.length());
            for (int i = 0; i < data.length(); i++) {
              Tensor delta = data.get(i);
              Tensor value = ValueLayer.this.data[i % ValueLayer.this.data.length];
              buffer.get(value.getId(), value.getData()).addInPlace(delta.getData());
            }
          }
        }) {

      @Override
      public boolean isAlive() {
        return !ValueLayer.this.isFrozen();
      }

      public void _free() {
      }
    };
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources,
                            @Nonnull DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJsonStub();
    JsonArray values = new JsonArray();
    RefArrays.stream(data).map(datum -> datum.getJson(resources, dataSerializer))
        .forEach(values::add);
    json.add("values", values);
    return json;
  }

  @Nonnull
  @Override
  public RefList<double[]> state() {
    return RefArrays.stream(data).map(x -> x.getData())
        .collect(RefCollectors.toList());
  }

  public void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  ValueLayer addRef() {
    return (ValueLayer) super.addRef();
  }

  public static @RefAware
  class RefWrapper<T> {
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
      return System.identityHashCode(obj);
    }
  }
}
