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
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * The type Value layer.
 */
@SuppressWarnings("serial")
public class ValueLayer extends LayerBase {

  @Nullable
  private Tensor[] data;

  /**
   * Instantiates a new Value layer.
   *
   * @param json      the json
   * @param resources the resources
   */
  protected ValueLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> resources) {
    super(json);
    RefUtil.freeRef(data);
    JsonArray values = json.getAsJsonArray("values");
    data = RefIntStream.range(0, values.size())
        .mapToObj(i -> Tensor.fromJson(values.get(i), resources))
        .toArray(Tensor[]::new);
  }

  /**
   * Instantiates a new Value layer.
   *
   * @param data the data
   */
  public ValueLayer(final @Nonnull Tensor... data) {
    super();
    RefUtil.freeRef(this.data);
    int length = data.length;
    this.data = RefArrays.copyOf(data, length);
    this.frozen = true;
  }

  /**
   * Get data tensor [ ].
   *
   * @return the tensor [ ]
   */
  @Nullable
  public Tensor[] getData() {
    return RefUtil.addRef(data);
  }

  /**
   * Sets data.
   *
   * @param data the data
   */
  public void setData(@Nullable final Tensor... data) {
    RefUtil.freeRef(this.data);
    this.data = data;
  }

  /**
   * From json value layer.
   *
   * @param json the json
   * @param rs   the rs
   * @return the value layer
   */
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
    Result.Accumulator accumulator = new Accumulator(this.addRef());
    TensorArray data = new TensorArray(RefUtil.addRef(this.data));
    return new Result(data, accumulator, !isFrozen());
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, @Nonnull DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJsonStub();
    JsonArray values = new JsonArray();
    RefArrays.stream(RefUtil.addRef(data)).map(datum -> {
      JsonElement element = datum.getJson(resources, dataSerializer);
      datum.freeRef();
      return element;
    }).forEach(values::add);
    json.add("values", values);
    return json;
  }

  @Nonnull
  @Override
  public RefList<double[]> state() {
    return RefArrays.stream(RefUtil.addRef(data)).map(tensor -> {
      double[] data = tensor.getData();
      tensor.freeRef();
      return data;
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

  private static class Accumulator extends Result.Accumulator {

    private final ValueLayer valueLayer;

    /**
     * Instantiates a new Accumulator.
     *
     * @param valueLayer the value layer
     */
    public Accumulator(ValueLayer valueLayer) {
      this.valueLayer = valueLayer;
    }

    @Override
    public void accept(@Nonnull DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      if (!valueLayer.isFrozen()) {
        valueLayer.assertAlive();
        assert valueLayer.data != null;
        assert 1 == valueLayer.data.length || valueLayer.data.length == data.length();
        for (int i = 0; i < data.length(); i++) {
          Tensor delta = data.get(i);
          Tensor value = valueLayer.data[i % valueLayer.data.length].addRef();
          Delta<UUID> valueDelta = buffer.get(value.getId(), value);
          valueDelta.addInPlace(delta);
          valueDelta.freeRef();
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
  }
}
