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
import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.mindseye.network.PipelineNetwork;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public interface Layer extends ReferenceCounting, Serializable, ZipSerializable {
  @Nonnull
  static Layer fromJson(@Nonnull final JsonObject json) {
    return fromJson(json, null);
  }

  @Nonnull
  static Layer fromZip(@Nonnull final ZipFile zipfile) {
    @Nonnull HashMap<CharSequence, byte[]> resources = ZipSerializable.extract(zipfile);
    return fromJson(ZipSerializable.toJson(resources.get("model.json")), resources);
  }

  @Nonnull
  static Layer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    JsonElement classElement = json.get("class");
    assert null != classElement : json.toString();
    final String className = classElement.getAsString();
    try {
      final Class<?> clazz = Class.forName(className);
      if (null == clazz) throw new ClassNotFoundException(className);
      final Method method = clazz.getMethod("fromJson", JsonObject.class, Map.class);
      if (method.getDeclaringClass() == Layer.class) {
        throw new IllegalArgumentException("Cannot find deserialization method for " + className);
      }
      @Nonnull Layer invoke = (Layer) method.invoke(null, json, rs);
      if (null == invoke) throw new IllegalStateException();
      return invoke;
    } catch (@Nonnull IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  default int[] evalDims(int[] inputDims) {
    Tensor input = new Tensor(inputDims);
    Tensor tensor = eval(input).getDataAndFree().getAndFree(0);
    input.freeRef();
    int[] dimensions = tensor.getDimensions();
    tensor.freeRef();
    return dimensions;
  }

  @NotNull
  default List<Tensor> map(Collection<? extends Tensor> values) {
    return values.stream().map(t -> {
      return eval(t).getDataAndFree().getAndFree(0);
    }).collect(Collectors.toList());
  }

  @NotNull
  default Stream<Tensor> map(Stream<? extends Tensor> values) {
    return values.map(t -> {
      return eval(t).getDataAndFree().getAndFree(0);
    });
  }

  @Override
  Layer addRef();

  default PipelineNetwork andThen(Layer append) {
    return PipelineNetwork.build(1,
        this,
        append
    );
  }

  default PipelineNetwork freeAndThen(Layer append) {
    PipelineNetwork build = andThen(append);
    this.freeRef();
    return build;
  }

  default PipelineNetwork andThenWrap(Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    PipelineNetwork wrap = PipelineNetwork.build(1,
        this,
        append
    );
    append.freeRef();
    return wrap;
  }

  default PipelineNetwork freeAndThenWrap(Layer append) {
    return PipelineNetwork.wrap(1,
        this,
        append
    );
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  default <T extends Layer> T as(@Nonnull final Class<T> targetClass) {
    @Nonnull HashMap<CharSequence, byte[]> resources = new HashMap<>();
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
    @Nonnull HashMap<CharSequence, byte[]> resources = new HashMap<>();
    final JsonObject json = getJson(resources, precision).getAsJsonObject();
    return Layer.fromJson(json, resources);
  }

  @Nullable
  default Result eval(Result... array) {
    assertAlive();
    Arrays.stream(array).forEach(ReferenceCounting::addRef);
    Arrays.stream(array).map(Result::getData).forEach(ReferenceCounting::addRef);
    return evalAndFree(array);
  }

  @Nullable
  default Result evalAndFree(Result... array) {
    assertAlive();
    Result result = eval(array);
    Arrays.stream(array).map(Result::getData).forEach(ReferenceCounting::freeRef);
    Arrays.stream(array).forEach(ReferenceCounting::freeRef);
    return result;
  }

  @Nullable
  default Result eval(@Nonnull final Tensor... array) {
    Result[] input = ConstantResult.singleResultArray(array);
    Result eval = eval(input);
    Arrays.stream(input).forEach(ReferenceCounting::freeRef);
    Arrays.stream(input).map(Result::getData).forEach(ReferenceCounting::freeRef);
    return eval;
  }

  @Nullable
  default Result eval(@Nonnull final Tensor[][] array) {
    Result[] input = ConstantResult.singleResultArray(array);
    Result eval = eval(input);
    Arrays.stream(input).forEach(ReferenceCounting::freeRef);
    Arrays.stream(input).map(Result::getData).forEach(ReferenceCounting::freeRef);
    return eval;
  }

  @Nonnull
  default Layer freeze() {
    return setFrozen(true);
  }

  List<Layer> getChildren();

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

  @Nullable
  List<double[]> state();

  default Layer copyAndFree() {
    Layer copy = copy();
    freeRef();
    return copy;
  }
}
