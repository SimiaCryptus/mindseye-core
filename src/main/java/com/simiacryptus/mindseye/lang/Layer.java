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
 * This is the Layer interface.
 *
 * @docgenVersion 9
 */
public interface Layer extends ReferenceCounting, Serializable, ZipSerializable {
  /**
   * Returns a list of child layers.
   *
   * @docgenVersion 9
   */
  RefList<Layer> getChildren();

  /**
   * @return the id, or null if it does not exist
   * @docgenVersion 9
   */
  @Nullable
  UUID getId();

  /**
   * Returns a JSON string representation of this object,
   * using the Gson library to pretty-print the output.
   *
   * @docgenVersion 9
   */
  default CharSequence getJsonString() {
    return new GsonBuilder().setPrettyPrinting().create().toJson(getJson());
  }

  /**
   * @return a JsonObject that is not null
   * @docgenVersion 9
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
   * @return the name, or null if not available
   * @docgenVersion 9
   */
  @Nullable
  String getName();

  /**
   * Sets the name of the object.
   *
   * @param name the new name for the object
   * @docgenVersion 9
   */
  void setName(final String name);

  /**
   * Returns true if the object is frozen, false otherwise.
   *
   * @docgenVersion 9
   */
  boolean isFrozen();

  /**
   * Sets the frozen state of the object.
   *
   * @param frozen the new frozen state
   * @docgenVersion 9
   */
  void setFrozen(final boolean frozen);

  /**
   * @param json the JSON object to convert to a layer
   * @return the layer represented by the JSON object
   * @docgenVersion 9
   */
  @Nonnull
  static Layer fromJson(@Nonnull final JsonObject json) {
    return fromJson(json, null);
  }

  /**
   * Extracts the resources from the given zipfile and creates a layer from the JSON model.
   *
   * @param zipfile the zipfile containing the resources
   * @return the layer created from the resources
   * @docgenVersion 9
   */
  @Nonnull
  static Layer fromZip(@Nonnull final ZipFile zipfile) {
    @Nonnull
    HashMap<CharSequence, byte[]> resources = ZipSerializable.extract(zipfile);
    return fromJson(JsonUtil.toJson(resources.get("model.json")), resources);
  }

  /**
   * @param json the JSON object to convert
   * @param rs   the map of resources
   * @return the new layer
   * @throws NullPointerException if json or rs is null
   * @docgenVersion 9
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
   * @param inputDims the dimensions of the input array
   * @return the dimensions of the output array
   * @docgenVersion 9
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

  /**
   * Returns the first data element of the result of evaluating the given input tensors.
   *
   * @docgenVersion 9
   */
  default Tensor simpleEval(Tensor... input) {
    return Result.getData0(eval(input));
  }

  /**
   * Maps the given values to a new RefList.
   *
   * @param values the values to map
   * @return the new RefList
   * @docgenVersion 9
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
   * Maps the given stream of values to another stream.
   *
   * @param values the stream of values to map
   * @return the mapped stream
   * @docgenVersion 9
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
   * Builds a new pipeline network by appending the given layer to this network.
   *
   * @param append the layer to append
   * @return the new pipeline network
   * @docgenVersion 9
   */
  default PipelineNetwork andThen(@Nonnull Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    return PipelineNetwork.build(1, this.addRef(), append);
  }

  /**
   * @param targetClass the target class to cast to
   * @return the layer cast to the target class
   * @docgenVersion 9
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
   * Returns a copy of this layer, using the specified serial precision.
   *
   * @param precision the precision to use for serialization
   * @return a copy of this layer
   * @docgenVersion 9
   */
  @Nonnull
  default Layer copy() {
    return copy(SerialPrecision.Double);
  }

  /**
   * Returns a copy of this layer with the specified serialization precision.
   *
   * @param precision the new serialization precision
   * @return a copy of this layer with the specified serialization precision
   * @docgenVersion 9
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
   * @Nullable Result eval(@Nullable Result... array);
   * @docgenVersion 9
   */
  @Nullable
  Result eval(@Nullable Result... array);

  /**
   * @param array the array of Tensors to evaluate
   * @return the Result of the evaluation
   * @docgenVersion 9
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
   * @param array the array to evaluate
   * @return the evaluation result
   * @docgenVersion 9
   */
  @Nullable
  default Result eval(@Nonnull final Tensor[][] array) {
    return eval(ConstantResult.singleResultArray(array));
  }

  /**
   * Sets the frozen flag to true.
   *
   * @docgenVersion 9
   */
  default void freeze() {
    setFrozen(true);
  }

  /**
   * @return a list of states, or null if there are no states
   * @docgenVersion 9
   */
  @Nullable
  RefList<double[]> state();

  /**
   * @return a unary operator that converts an object to a Tensor
   * @throws NullPointerException if the object is null
   * @docgenVersion 9
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
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * Add a reference to this layer.
   *
   * @docgenVersion 9
   */
  Layer addRef();

}
