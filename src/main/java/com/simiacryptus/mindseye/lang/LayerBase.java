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

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("serial")
public abstract class LayerBase extends RegisteredObjectBase implements Layer {
  private final UUID id;
  protected boolean frozen = false;
  @Nullable
  private String name;

  protected LayerBase() {
    id = UUID.randomUUID();
    name = getClass().getSimpleName();// + "/" + getId();
  }

  protected LayerBase(@Nonnull final JsonObject json) {
    if (!getClass().getCanonicalName().equals(json.get("class").getAsString())) {
      throw new IllegalArgumentException(getClass().getCanonicalName() + " != " + json.get("class").getAsString());
    }
    id = UUID.fromString(json.get("id").getAsString());
    if (json.has("isFrozen")) {
      this.frozen = json.get("isFrozen").getAsBoolean();
    }
    if (json.has("name")) {
      setName(json.get("name").getAsString());
    }
  }

  protected LayerBase(final UUID id, final String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public LayerBase addRef() {
    return (LayerBase) super.addRef();
  }

  @Override
  public final boolean equals(@Nullable final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    @Nullable final Layer other = (Layer) obj;
    if (getId() == null) {
      return other.getId() == null;
    } else return getId().equals(other.getId());
  }

  public List<Layer> getChildren() {
    return Arrays.asList(this);
  }

  @Nullable
  public UUID getId() {
    return id;
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nonnull
  public Layer setName(final String name) {
    this.name = name;
    return this;
  }

  @Override
  public final int hashCode() {
    return getId().hashCode();
  }

  public boolean isFrozen() {
    return frozen;
  }

  @Nonnull
  public Layer setFrozen(final boolean frozen) {
    this.frozen = frozen;
    return self();
  }

  @Nonnull
  protected final Layer self() {
    return this;
  }

  @Nullable
  @Override
  public String toString() {
    return getName();
  }

  @Override
  protected void _free() {

  }

}
