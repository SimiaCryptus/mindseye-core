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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@SuppressWarnings("serial")
public abstract class WrapperLayer extends LayerBase {
  @Nullable
  private Layer inner;

  protected WrapperLayer() {
    if (null != inner)
      inner.freeRef();
    inner = null;
  }

  public WrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json);
    Layer temp_19_0002 = Layer.fromJson(json.getAsJsonObject("inner"), rs);
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = temp_19_0002.addRef();
    temp_19_0002.freeRef();
  }

  public WrapperLayer(@Nullable final Layer inner) {
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

  public void setInner(@Nullable Layer inner) {
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = inner;
  }

  @Override
  public boolean isFrozen() {
    if (null == inner)
      return true;
    return inner.isFrozen();
  }

  @Nullable
  @Override
  public Result eval(@Nullable final Result... array) {
    assert inner != null;
    Result temp_19_0005 = inner.eval(RefUtil.addRefs(array));
    if (null != array)
      ReferenceCounting.freeRefs(array);
    return temp_19_0005;
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJsonStub();
    Layer temp_19_0007 = getInner();
    assert temp_19_0007 != null;
    json.add("inner", temp_19_0007.getJson(resources, dataSerializer));
    temp_19_0007.freeRef();
    return json;
  }

  @Nonnull
  @Override
  public void setFrozen(final boolean frozen) {
    if (null == inner)
      return;
    inner.setFrozen(frozen);
  }

  @Nullable
  @Override
  public RefList<double[]> state() {
    assert inner != null;
    return inner.state();
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
    inner = null;
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  WrapperLayer addRef() {
    return (WrapperLayer) super.addRef();
  }
}
