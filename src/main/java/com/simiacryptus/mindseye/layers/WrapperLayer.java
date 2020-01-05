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
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("serial")
public abstract @RefAware
class WrapperLayer extends LayerBase {
  @Nullable
  private Layer inner;

  protected WrapperLayer() {
    inner = null;
  }

  public WrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json);
    this.inner = Layer.fromJson(json.getAsJsonObject("inner"), rs);
  }

  public WrapperLayer(@org.jetbrains.annotations.Nullable final Layer inner) {
    this.inner = inner;
  }

  @Nullable
  public final Layer getInner() {
    return inner;
  }

  public WrapperLayer setInner(@Nullable Layer inner) {
    if (this.getInner() != null)
      this.getInner();
    this.inner = inner;
    this.getInner();
    return this;
  }

  @Override
  public boolean isFrozen() {
    if (null == inner)
      return true;
    return inner.isFrozen();
  }

  public static @SuppressWarnings("unused")
  WrapperLayer[] addRefs(WrapperLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(WrapperLayer::addRef)
        .toArray((x) -> new WrapperLayer[x]);
  }

  public static @SuppressWarnings("unused")
  WrapperLayer[][] addRefs(WrapperLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(WrapperLayer::addRefs)
        .toArray((x) -> new WrapperLayer[x][]);
  }

  @Nullable
  @Override
  public Result eval(final Result... array) {
    return inner.eval(array);
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources,
                            DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJsonStub();
    json.add("inner", getInner().getJson(resources, dataSerializer));
    return json;
  }

  @Nonnull
  @Override
  public Layer setFrozen(final boolean frozen) {
    if (null == inner)
      return this;
    inner.setFrozen(frozen);
    return this;
  }

  @Nullable
  @Override
  public RefList<double[]> state() {
    return inner.state();
  }

  public void _free() {
    super._free();
  }

  public @Override
  @SuppressWarnings("unused")
  WrapperLayer addRef() {
    return (WrapperLayer) super.addRef();
  }
}
