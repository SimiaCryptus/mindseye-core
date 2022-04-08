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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * This class represents a base layer.
 * It contains a stack trace of the thread that created it,
 * as well as a UUID and a name.
 * The layer is initially not frozen.
 *
 * @docgenVersion 9
 */
@SuppressWarnings("serial")
public abstract class LayerBase extends ReferenceCountingBase implements Layer {
  //  public final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  private final UUID id;
  /**
   * The Frozen.
   */
  protected boolean frozen = false;
  @Nullable
  private String name;

  /**
   * Instantiates a new Layer base.
   */
  protected LayerBase() {
    id = UUID.randomUUID();
    name = getClass().getSimpleName();// + "/" + getId();
  }

  /**
   * Instantiates a new Layer base.
   *
   * @param json the json
   */
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

  /**
   * Instantiates a new Layer base.
   *
   * @param id   the id
   * @param name the name
   */
  protected LayerBase(final UUID id, @Nullable final String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Returns a list of child layers.
   *
   * @docgenVersion 9
   */
  public RefList<Layer> getChildren() {
    return RefArrays.asList(this.addRef());
  }

  /**
   * Returns the UUID of this object.
   *
   * @docgenVersion 9
   */
  @Nullable
  public UUID getId() {
    return id;
  }

  /**
   * Returns the name of the object.
   *
   * @docgenVersion 9
   */
  @Nullable
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @docgenVersion 9
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Returns true if the object is frozen, false otherwise.
   *
   * @docgenVersion 9
   */
  public boolean isFrozen() {
    return frozen;
  }

  /**
   * Sets the frozen state.
   *
   * @docgenVersion 9
   */
  public void setFrozen(final boolean frozen) {
    this.frozen = frozen;
  }

  /**
   * Returns true if the object is the same as the one given, false otherwise.
   *
   * @docgenVersion 9
   */
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
      boolean temp_45_0001 = other.getId() == null;
      other.freeRef();
      return temp_45_0001;
    } else {
      boolean temp_45_0002 = getId().equals(other.getId());
      other.freeRef();
      return temp_45_0002;
    }
  }

  /**
   * Returns a hash code value for the object. This method is
   * supported for the benefit of hash tables such as those provided by
   * HashMap.
   *
   * @docgenVersion 9
   */
  @Override
  public final int hashCode() {
    assert getId() != null;
    return getId().hashCode();
  }

  /**
   * Returns a string representation of this object.
   *
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public String toString() {
    return getName();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
  }

  /**
   * Add a reference to the LayerBase.
   *
   * @docgenVersion 9
   */
  public @Override
  @SuppressWarnings("unused")
  LayerBase addRef() {
    return (LayerBase) super.addRef();
  }

}
