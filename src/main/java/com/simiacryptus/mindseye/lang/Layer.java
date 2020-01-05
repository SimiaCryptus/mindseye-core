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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.JsonUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.zip.ZipFile;

public @RefAware
interface Layer extends ReferenceCounting, Serializable, ZipSerializable {
  RefList<Layer> getChildren();

  @Nullable
  UUID getId();

  default CharSequence getJsonString() {
    return new GsonBuilder().setPrettyPrinting().create().toJson(getJson());
  }

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

  @Nullable
  String getName();

  @Nonnull
  Layer setName(final String name);

  boolean isFrozen();

  @Nonnull
  Layer setFrozen(final boolean frozen);

  @Nonnull
  static Layer fromJson(@Nonnull final JsonObject json) {
    return fromJson(json, null);
  }

  @Nonnull
  static Layer fromZip(@Nonnull final ZipFile zipfile) {
    @Nonnull
    HashMap<CharSequence, byte[]> resources = ZipSerializable.extract(zipfile);
    return fromJson(JsonUtil.toJson(resources.get("model.json")), resources);
  }

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
      if (null == invoke)
        throw new IllegalStateException();
      return invoke;
    } catch (@Nonnull IllegalAccessException | InvocationTargetException | NoSuchMethodException
        | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static @SuppressWarnings("unused")
  Layer[] addRefs(Layer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Layer::addRef).toArray((x) -> new Layer[x]);
  }

  public static @SuppressWarnings("unused")
  Layer[][] addRefs(Layer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Layer::addRefs).toArray((x) -> new Layer[x][]);
  }

  default int[] evalDims(int[] inputDims) {
    Tensor input = new Tensor(inputDims);
    Tensor tensor = eval(input).getData().get(0);
    return tensor.getDimensions();
  }

  @NotNull
  default RefList<Tensor> map(
      RefCollection<? extends Tensor> values) {
    return values.stream().map(t -> {
      return eval(t).getData().get(0);
    }).collect(RefCollectors.toList());
  }

  @NotNull
  default RefStream<Tensor> map(
      RefStream<? extends Tensor> values) {
    return values.map(t -> {
      return eval(t).getData().get(0);
    });
  }

  default PipelineNetwork andThen(Layer append) {
    return PipelineNetwork.build(1, this, append);
  }

  default PipelineNetwork freeAndThen(Layer append) {
    return andThen(append);
  }

  default PipelineNetwork andThenWrap(Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    return PipelineNetwork.build(1, this, append);
  }

  default PipelineNetwork freeAndThenWrap(Layer append) {
    return PipelineNetwork.build(1, this, append);
  }

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

  @Nonnull
  default Layer copy() {
    return copy(SerialPrecision.Double);
  }

  @Nonnull
  default Layer copy(SerialPrecision precision) {
    assertAlive();
    @Nonnull
    HashMap<CharSequence, byte[]> resources = new HashMap<>();
    final JsonObject json = getJson(resources, precision).getAsJsonObject();
    return Layer.fromJson(json, resources);
  }

  @Nullable
  default Result eval(Result... array) {
    assertAlive();
    return eval(array);
  }

  @Nullable
  default Result eval(@Nonnull final Tensor... array) {
    Result[] input = ConstantResult.singleResultArray(array);
    return eval(input);
  }

  @Nullable
  default Result eval(@Nonnull final Tensor[][] array) {
    return eval(ConstantResult.singleResultArray(array));
  }

  @Nonnull
  default Layer freeze() {
    return setFrozen(true);
  }

  @Nullable
  RefList<double[]> state();

  default UnaryOperator<Tensor> asTensorFunction() {
    return input -> {
      return eval(input).getData().get(0);
    };
  }

  public void _free();

  public Layer addRef();
}
