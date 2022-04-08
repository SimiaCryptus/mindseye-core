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
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * This class represents a WrapperLayer.
 *
 * @Nullable indicates that the 'inner' field may be null.
 * @docgenVersion 9
 */
@SuppressWarnings("serial")
public class WrapperLayer extends LayerBase {
  /**
   * The Inner.
   */
  @Nullable
  protected Layer inner;

  /**
   * Instantiates a new Wrapper layer.
   */
  protected WrapperLayer() {
    if (null != inner)
      inner.freeRef();
    inner = null;
  }

  /**
   * Instantiates a new Wrapper layer.
   *
   * @param json the json
   * @param rs   the rs
   */
  public WrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json);
    if (null != this.inner) this.inner.freeRef();
    this.inner = Layer.fromJson(json.getAsJsonObject("inner"), rs);
  }

  /**
   * Instantiates a new Wrapper layer.
   *
   * @param inner the inner
   */
  public WrapperLayer(@Nullable final Layer inner) {
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = inner;
  }

  /**
   * Returns the inner layer.
   *
   * @docgenVersion 9
   */
  @Nullable
  public final Layer getInner() {
    return RefUtil.addRef(this.inner);
  }

  /**
   * Sets the inner value.
   *
   * @docgenVersion 9
   */
  public synchronized void setInner(@Nullable Layer inner) {
    if (null != this.inner)
      this.inner.freeRef();
    this.inner = inner;
  }

  /**
   * Returns true if the object is frozen, false otherwise.
   *
   * @docgenVersion 9
   */
  @Override
  public boolean isFrozen() {
    if (null == inner)
      return true;
    return inner.isFrozen();
  }

  /**
   * Sets the frozen state.
   *
   * @docgenVersion 9
   */
  @Override
  public void setFrozen(final boolean frozen) {
    if (null == inner)
      return;
    inner.setFrozen(frozen);
  }

  /**
   * Evaluates the result of the code.
   *
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public Result eval(@Nullable final Result... array) {
    return inner.eval(array);
  }

  /**
   * Returns the JSON object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJsonStub();
    if (null != inner) json.add("inner", inner.getJson(resources, dataSerializer));
    return json;
  }

  /**
   * Returns a list of references to double arrays representing the state.
   *
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public RefList<double[]> state() {
    return inner.state();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    if (null != inner) {
      inner.freeRef();
      inner = null;
    }
    super._free();
  }

  /**
   * Adds a reference to the WrapperLayer.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  WrapperLayer addRef() {
    return (WrapperLayer) super.addRef();
  }
}
