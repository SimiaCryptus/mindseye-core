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
import com.simiacryptus.mindseye.lang.LayerBase;
import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * This class represents a placeholder layer.
 * The key field may be null.
 *
 * @docgenVersion 9
 */
@SuppressWarnings("serial")
public final class PlaceholderLayer<T> extends LayerBase {

  @Nullable
  private final T key;

  /**
   * Instantiates a new Placeholder layer.
   *
   * @param key the key
   */
  public PlaceholderLayer(@Nullable final T key) {
    if (null == key)
      throw new UnsupportedOperationException();
    this.key = key;
    setName(getClass().getSimpleName() + "/" + getId());
  }

  /**
   * Returns the UUID of this object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public UUID getId() {
    assertAlive();
    T key = this.getKey();
    return key == null ? UUID.randomUUID() : UUID.nameUUIDFromBytes(key.toString().getBytes());
  }

  /**
   * Returns the key.
   *
   * @docgenVersion 9
   */
  @Nullable
  public T getKey() {
    return key;
  }

  /**
   * Evaluates the result of the code.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Result eval(@Nullable final Result... array) {
    if (null != array)
      RefUtil.freeRef(array);
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the JSON object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a list of references to double arrays representing the state.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefList<double[]> state() {
    throw new UnsupportedOperationException();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    RefUtil.freeRef(key);
    super._free();
  }

  /**
   * Add a reference to the PlaceholderLayer.
   *
   * @return the PlaceholderLayer
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  PlaceholderLayer<T> addRef() {
    return (PlaceholderLayer<T>) super.addRef();
  }
}
