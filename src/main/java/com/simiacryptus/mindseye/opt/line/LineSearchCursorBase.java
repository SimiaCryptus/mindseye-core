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

package com.simiacryptus.mindseye.opt.line;

import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Base class for a line search cursor.
 *
 * @docgenVersion 9
 */
public abstract class LineSearchCursorBase extends ReferenceCountingBase implements LineSearchCursor {

  /**
   * Returns the direction type of this object.
   *
   * @docgenVersion 9
   */
  public abstract CharSequence getDirectionType();

  /**
   * Returns the delta set of UUIDs for the positions.
   *
   * @docgenVersion 9
   */
  public abstract DeltaSet<UUID> position(double alpha);

  /**
   * Resets the value.
   *
   * @docgenVersion 9
   */
  public abstract void reset();

  /**
   * This method steps through the line search process.
   *
   * @docgenVersion 9
   */
  @javax.annotation.Nullable
  public abstract LineSearchPoint step(double alpha, TrainingMonitor monitor);

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
  }

  /**
   * Adds a reference to this LineSearchCursorBase.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LineSearchCursorBase addRef() {
    return (LineSearchCursorBase) super.addRef();
  }
}
