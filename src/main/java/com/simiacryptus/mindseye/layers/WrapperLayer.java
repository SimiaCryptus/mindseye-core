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

import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.lang.DataSerializer;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.LayerBase;
import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("serial")
public abstract class WrapperLayer extends LayerBase {
  @Nullable
  private Layer inner;

  protected WrapperLayer() {
    Layer temp_19_0001 = null;
    if (null != inner)
      inner.freeRef();
    inner = temp_19_0001 == null ? null : temp_19_0001.addRef();
    if (null != temp_19_0001)
      temp_19_0001.freeRef();
  }

  public WrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json);
    Layer temp_19_0002 = Layer.fromJson(json.getAsJsonObject("inner"), rs);
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = temp_19_0002 == null ? null : temp_19_0002.addRef();
    if (null != temp_19_0002)
      temp_19_0002.freeRef();
  }

  public WrapperLayer(@org.jetbrains.annotations.Nullable final Layer inner) {
    Layer temp_19_0003 = inner == null ? null : inner.addRef();
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = temp_19_0003 == null ? null : temp_19_0003.addRef();
    if (null != temp_19_0003)
      temp_19_0003.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  @Nullable
  public final Layer getInner() {
    return inner == null ? null : inner.addRef();
  }

  public WrapperLayer setInner(@Nullable Layer inner) {
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = inner;
    return this.addRef();
  }

  @Override
  public boolean isFrozen() {
    if (null == inner)
      return true;
    return inner.isFrozen();
  }

  public static @SuppressWarnings("unused") WrapperLayer[] addRefs(WrapperLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(WrapperLayer::addRef).toArray((x) -> new WrapperLayer[x]);
  }

  public static @SuppressWarnings("unused") WrapperLayer[][] addRefs(WrapperLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(WrapperLayer::addRefs)
        .toArray((x) -> new WrapperLayer[x][]);
  }

  @Nullable
  @Override
  public Result eval(final Result... array) {
    Result temp_19_0005 = inner.eval(Result.addRefs(array));
    if (null != array)
      ReferenceCounting.freeRefs(array);
    return temp_19_0005;
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    @Nonnull
    final JsonObject json = super.getJsonStub();
    Layer temp_19_0007 = getInner();
    json.add("inner", temp_19_0007.getJson(resources, dataSerializer));
    if (null != temp_19_0007)
      temp_19_0007.freeRef();
    return json;
  }

  @Nonnull
  @Override
  public Layer setFrozen(final boolean frozen) {
    if (null == inner)
      return this.addRef();
    RefUtil.freeRef(inner.setFrozen(frozen));
    return this.addRef();
  }

  @Nullable
  @Override
  public RefList<double[]> state() {
    return inner.state();
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
    inner = null;
    super._free();
  }

  public @Override @SuppressWarnings("unused") WrapperLayer addRef() {
    return (WrapperLayer) super.addRef();
  }
}
