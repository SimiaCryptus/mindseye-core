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
 * This class represents a value layer, which is composed of an
 * array of Tensors. This array can be null.
 *
 * @docgenVersion 9
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
   * @return an array of Tensors, or null if no data exists
   * @docgenVersion 9
   */
  @Nullable
  public Tensor[] getData() {
    return RefUtil.addRef(data);
  }

  /**
   * Sets the data for this layer.
   *
   * @param data The data to set.
   * @docgenVersion 9
   */
  public void setData(@Nullable final Tensor... data) {
    RefUtil.freeRef(this.data);
    this.data = data;
  }

  /**
   * Creates a new ValueLayer from the given JSON object and map of raw data.
   *
   * @param json the JSON object to create the layer from
   * @param rs   the map of raw data
   * @return the new ValueLayer
   * @docgenVersion 9
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static ValueLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new ValueLayer(json, rs);
  }


  /**
   * @param array the result array
   * @return the result
   * @throws NullPointerException if array is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... array) {
    assert 0 == array.length;
    RefUtil.freeRef(array);
    Result.Accumulator accumulator = new Accumulator(this.addRef());
    TensorArray data = new TensorArray(RefUtil.addRef(this.data));
    return new Result(data, accumulator, !isFrozen());
  }

  /**
   * @param resources      the resources to get the JSON from
   * @param dataSerializer the data serializer to use
   * @return the JSON object
   * @docgenVersion 9
   */
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

  /**
   * Returns a list of double arrays, each representing the data of a tensor in this data set.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefList<double[]> state() {
    return RefArrays.stream(RefUtil.addRef(data)).map(tensor -> {
      double[] data = tensor.getData();
      tensor.freeRef();
      return data;
    }).collect(RefCollectors.toList());
  }

  /**
   * Frees this object's resources.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    RefUtil.freeRef(data);
    data = null;
  }

  /**
   * Adds a reference to this layer.
   *
   * @return a reference to this layer
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ValueLayer addRef() {
    return (ValueLayer) super.addRef();
  }

  /**
   * This class represents an accumulator, which is a data structure that stores
   * a running total of numeric values.
   * <p>
   * The accumulator is implemented as a value layer, which is a data structure
   * that stores values in a way that allows them to be efficiently updated.
   *
   * @docgenVersion 9
   */
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

    /**
     * @Override public void accept(@Nonnull DeltaSet<UUID> buffer, @Nonnull TensorList data);
     * @docgenVersion 9
     */
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

    /**
     * Frees this object and its resources.
     *
     * @docgenVersion 9
     */
    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      valueLayer.freeRef();
    }
  }
}
