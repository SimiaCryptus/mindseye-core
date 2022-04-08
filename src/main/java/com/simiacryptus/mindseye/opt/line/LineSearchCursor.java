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
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * This is the LineSearchCursor interface.
 *
 * @docgenVersion 9
 */
public interface LineSearchCursor extends ReferenceCounting {

  /**
   * Returns the direction type of this object.
   *
   * @docgenVersion 9
   */
  CharSequence getDirectionType();

  /**
   * Returns the PointSample after taking a step.
   *
   * @docgenVersion 9
   */
  default PointSample afterStep(@Nonnull PointSample step) {
    return step;
  }

  /**
   * Returns the delta set of UUIDs for the positions.
   *
   * @docgenVersion 9
   */
  DeltaSet<UUID> position(double alpha);

  /**
   * Resets the object to its default state.
   *
   * @docgenVersion 9
   */
  void reset();

  /**
   * This method steps through the line search process.
   *
   * @docgenVersion 9
   */
  @javax.annotation.Nullable
  LineSearchPoint step(double alpha, TrainingMonitor monitor);

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * Adds a reference to the LineSearchCursor.
   *
   * @docgenVersion 9
   */
  LineSearchCursor addRef();
}
