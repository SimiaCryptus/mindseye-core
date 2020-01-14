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

public interface Layer extends ReferenceCounting, Serializable, ZipSerializable {
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
      if (null == invoke) {
        assert false;
        invoke.freeRef();
        throw new IllegalStateException();
      }
      return invoke;
    } catch (@Nonnull IllegalAccessException | InvocationTargetException | NoSuchMethodException
        | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static @SuppressWarnings("unused")
  Layer[] addRefs(@Nullable Layer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Layer::addRef).toArray((x) -> new Layer[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  Layer[][] addRefs(@Nullable Layer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Layer::addRefs).toArray((x) -> new Layer[x][]);
  }

  @Nonnull
  default int[] evalDims(int[] inputDims) {
    Tensor input = new Tensor(inputDims);
    Result temp_34_0013 = eval(input.addRef());
    assert temp_34_0013 != null;
    TensorList temp_34_0014 = temp_34_0013.getData();
    Tensor tensor = temp_34_0014.get(0);
    temp_34_0014.freeRef();
    temp_34_0013.freeRef();
    input.freeRef();
    int[] temp_34_0001 = tensor.getDimensions();
    tensor.freeRef();
    return temp_34_0001;
  }

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

  @Nonnull
  default PipelineNetwork andThen(@Nullable Layer append) {
    PipelineNetwork temp_34_0007 = PipelineNetwork.build(1, this.addRef(), append == null ? null : append.addRef());
    if (null != append)
      append.freeRef();
    return temp_34_0007;
  }

  @Nonnull
  default PipelineNetwork freeAndThen(@Nullable Layer append) {
    PipelineNetwork temp_34_0008 = andThen(append == null ? null : append.addRef());
    if (null != append)
      append.freeRef();
    return temp_34_0008;
  }

  default PipelineNetwork andThenWrap(@Nonnull Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    PipelineNetwork temp_34_0009 = PipelineNetwork.build(1, this.addRef(), append.addRef());
    append.freeRef();
    return temp_34_0009;
  }

  @Nonnull
  default PipelineNetwork freeAndThenWrap(@Nullable Layer append) {
    PipelineNetwork temp_34_0010 = PipelineNetwork.build(1, this.addRef(), append == null ? null : append.addRef());
    if (null != append)
      append.freeRef();
    return temp_34_0010;
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
  default Result eval(@Nullable Result... array) {
    assertAlive();
    Result temp_34_0011 = eval(Result.addRefs(array));
    if (null != array)
      ReferenceCounting.freeRefs(array);
    return temp_34_0011;
  }

  @Nullable
  default Result eval(@Nonnull final Tensor... array) {
    Result[] input = ConstantResult.singleResultArray(Tensor.addRefs(array));
    ReferenceCounting.freeRefs(array);
    Result temp_34_0004 = eval(Result.addRefs(input));
    ReferenceCounting.freeRefs(input);
    return temp_34_0004;
  }

  @Nullable
  default Result eval(@Nonnull final Tensor[][] array) {
    Result temp_34_0012 = eval(ConstantResult.singleResultArray(Tensor.addRefs(array)));
    ReferenceCounting.freeRefs(array);
    return temp_34_0012;
  }

  @Nonnull
  default Layer freeze() {
    return setFrozen(true);
  }

  @Nullable
  RefList<double[]> state();

  @Nonnull
  default UnaryOperator<Tensor> asTensorFunction() {
    return input -> {
      Result temp_34_0019 = eval(input == null ? null : input.addRef());
      assert temp_34_0019 != null;
      TensorList temp_34_0020 = temp_34_0019.getData();
      Tensor temp_34_0005 = temp_34_0020.get(0);
      temp_34_0020.freeRef();
      temp_34_0019.freeRef();
      if (null != input)
        input.freeRef();
      return temp_34_0005;
    };
  }

  public void _free();

  public Layer addRef();

}
