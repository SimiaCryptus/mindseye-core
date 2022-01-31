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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.network.PipelineNetwork;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefCollection;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.util.JsonUtil;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.zip.ZipFile;

/**
 * The interface Layer.
 */
public interface Layer extends ReferenceCounting, Serializable, ZipSerializable {
  /**
   * Gets children.
   *
   * @return the children
   */
  RefList<Layer> getChildren();

  /**
   * Gets id.
   *
   * @return the id
   */
  @Nullable
  UUID getId();

  /**
   * Gets json string.
   *
   * @return the json string
   */
  default CharSequence getJsonString() {
    return new GsonBuilder().setPrettyPrinting().create().toJson(getJson());
  }

  /**
   * Gets json stub.
   *
   * @return the json stub
   */
  @Nonnull
  default JsonObject getJsonStub() {
    assertAlive();
    @Nonnull final JsonObject json = new JsonObject();
    json.addProperty("class", getClass().getCanonicalName());
    json.addProperty("id", getId().toString());
    json.addProperty("isFrozen", isFrozen());
    json.addProperty("name", getName());
    return json;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  @Nullable
  String getName();

  /**
   * Sets name.
   *
   * @param name the name
   */
  void setName(final String name);

  /**
   * Is frozen boolean.
   *
   * @return the boolean
   */
  boolean isFrozen();

  /**
   * Sets frozen.
   *
   * @param frozen the frozen
   */
  void setFrozen(final boolean frozen);

  /**
   * From json layer.
   *
   * @param json the json
   * @return the layer
   */
  @Nonnull
  static Layer fromJson(@Nonnull final JsonObject json) {
    return fromJson(json, null);
  }

  /**
   * From zip layer.
   *
   * @param zipfile the zipfile
   * @return the layer
   */
  @Nonnull
  static Layer fromZip(@Nonnull final ZipFile zipfile) {
    @Nonnull
    HashMap<CharSequence, byte[]> resources = ZipSerializable.extract(zipfile);
    return fromJson(JsonUtil.toJson(resources.get("model.json")), resources);
  }

  /**
   * From json layer.
   *
   * @param json the json
   * @param rs   the rs
   * @return the layer
   */
  @Nonnull
  static Layer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    JsonElement classElement = json.get("class");
    assert null != classElement : json.toString();
    final String className = classElement.getAsString();
    try {
      final Class<?> clazz = Class.forName(className);
      if (null == clazz)
        throw new ClassNotFoundException(className);
      final Method method = clazz.getMethod("fromJson", JsonObject.class, Map.class);
      if (method.getDeclaringClass() == Layer.class) {
        throw new IllegalArgumentException("Cannot find deserialization method for " + className);
      }
      @Nonnull
      Layer invoke = (Layer) method.invoke(null, json, rs);
      if (null == invoke) {
        assert false;
        invoke.freeRef();
        throw new IllegalStateException();
      }
      return invoke;
    } catch (@Nonnull IllegalAccessException | InvocationTargetException | NoSuchMethodException
        | ClassNotFoundException e) {
      e.printStackTrace();
      throw Util.throwException(e);
    }
  }


  /**
   * Eval dims int [ ].
   *
   * @param inputDims the input dims
   * @return the int [ ]
   */
  @Nonnull
  default int[] evalDims(int[] inputDims) {
    Tensor input = new Tensor(inputDims);
    Result temp_34_0013 = eval(input.addRef());
    assert temp_34_0013 != null;
    TensorList temp_34_0014 = temp_34_0013.getData();
    temp_34_0013.freeRef();
    Tensor tensor = temp_34_0014.get(0);
    temp_34_0014.freeRef();
    input.freeRef();
    int[] temp_34_0001 = tensor.getDimensions();
    tensor.freeRef();
    return temp_34_0001;
  }

  default Tensor simpleEval(Tensor... input) {
    return Result.getData0(eval(input));
  }

  /**
   * Map ref list.
   *
   * @param values the values
   * @return the ref list
   */
  @Nonnull
  default RefList<Tensor> map(@Nonnull RefCollection<? extends Tensor> values) {
    RefList<Tensor> temp_34_0006 = values.stream().map(t -> {
      Result temp_34_0015 = eval(RefUtil.addRef(t));
      assert temp_34_0015 != null;
      TensorList temp_34_0016 = temp_34_0015.getData();
      Tensor temp_34_0002 = temp_34_0016.get(0);
      temp_34_0016.freeRef();
      temp_34_0015.freeRef();
      if (null != t)
        t.freeRef();
      return temp_34_0002;
    }).collect(RefCollectors.toList());
    values.freeRef();
    return temp_34_0006;
  }

  /**
   * Map ref stream.
   *
   * @param values the values
   * @return the ref stream
   */
  @Nonnull
  default RefStream<Tensor> map(@Nonnull RefStream<Tensor> values) {
    return values.map(t -> {
      Result temp_34_0017 = eval(RefUtil.addRef(t));
      assert temp_34_0017 != null;
      TensorList temp_34_0018 = temp_34_0017.getData();
      Tensor temp_34_0003 = temp_34_0018.get(0);
      temp_34_0018.freeRef();
      temp_34_0017.freeRef();
      if (null != t)
        t.freeRef();
      return temp_34_0003;
    });
  }

  /**
   * And then pipeline network.
   *
   * @param append the append
   * @return the pipeline network
   */
  default PipelineNetwork andThen(@Nonnull Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    return PipelineNetwork.build(1, this.addRef(), append);
  }

  /**
   * As t.
   *
   * @param <T>         the type parameter
   * @param targetClass the target class
   * @return the t
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  default <T extends Layer> T as(@Nonnull final Class<T> targetClass) {
    @Nonnull
    HashMap<CharSequence, byte[]> resources = new HashMap<>();
    final JsonObject json = getJson(resources, SerialPrecision.Double).getAsJsonObject();
    json.remove("class");
    json.addProperty("class", targetClass.getCanonicalName());
    return (T) fromJson(json, resources);
  }

  /**
   * Copy layer.
   *
   * @return the layer
   */
  @Nonnull
  default Layer copy() {
    return copy(SerialPrecision.Double);
  }

  /**
   * Copy layer.
   *
   * @param precision the precision
   * @return the layer
   */
  @Nonnull
  default Layer copy(SerialPrecision precision) {
    assertAlive();
    @Nonnull
    HashMap<CharSequence, byte[]> resources = new HashMap<>();
    final JsonObject json = getJson(resources, precision).getAsJsonObject();
    return Layer.fromJson(json, resources);
  }

  /**
   * Eval result.
   *
   * @param array the array
   * @return the result
   */
  @Nullable
  Result eval(@Nullable Result... array);

  /**
   * Eval result.
   *
   * @param array the array
   * @return the result
   */
  @Nullable
  default Result eval(@Nonnull final Tensor... array) {
    for (int i = 0; i < array.length; i++) {
      assert array[i] != null;
      assert array[i].assertAlive();
    }
    return eval(ConstantResult.singleResultArray(array));
  }

  /**
   * Eval result.
   *
   * @param array the array
   * @return the result
   */
  @Nullable
  default Result eval(@Nonnull final Tensor[][] array) {
    return eval(ConstantResult.singleResultArray(array));
  }

  /**
   * Freeze.
   */
  default void freeze() {
    setFrozen(true);
  }

  /**
   * State ref list.
   *
   * @return the ref list
   */
  @Nullable
  RefList<double[]> state();

  /**
   * As tensor function unary operator.
   *
   * @return the unary operator
   */
  @Nonnull
  default UnaryOperator<Tensor> asTensorFunction() {
    return input -> {
      Result result = eval(input == null ? null : input.addRef());
      assert result != null;
      TensorList tensorList = result.getData();
      result.freeRef();
      Tensor tensor = tensorList.get(0);
      tensorList.freeRef();
      if (null != input)
        input.freeRef();
      return tensor;
    };
  }

  /**
   * Free.
   */
  void _free();

  Layer addRef();

}
