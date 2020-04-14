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
 * The interface Line search cursor.
 */
public interface LineSearchCursor extends ReferenceCounting {

  /**
   * Gets direction type.
   *
   * @return the direction type
   */
  CharSequence getDirectionType();

  /**
   * After step point sample.
   *
   * @param step the step
   * @return the point sample
   */
  default PointSample afterStep(@Nonnull PointSample step) {
    return step;
  }

  /**
   * Position delta set.
   *
   * @param alpha the alpha
   * @return the delta set
   */
  DeltaSet<UUID> position(double alpha);

  /**
   * Reset.
   */
  void reset();

  /**
   * Step line search point.
   *
   * @param alpha   the alpha
   * @param monitor the monitor
   * @return the line search point
   */
  @javax.annotation.Nullable
  LineSearchPoint step(double alpha, TrainingMonitor monitor);

  /**
   * Free.
   */
  void _free();

  LineSearchCursor addRef();
}
