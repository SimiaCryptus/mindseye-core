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

package com.simiacryptus.mindseye.opt;

import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represents a step in an iteration.
 *
 * @param iteration the iteration number
 * @param point     the point sample for this step
 * @param time      the current time
 * @docgenVersion 9
 */
public class Step extends ReferenceCountingBase {
  /**
   * The Iteration.
   */
  public final long iteration;
  /**
   * The Point.
   */
  @Nullable
  public final PointSample point;
  /**
   * The Time.
   */
  public final long time = RefSystem.currentTimeMillis();

  /**
   * Instantiates a new Step.
   *
   * @param point     the point
   * @param iteration the iteration
   */
  Step(@Nullable final PointSample point, final long iteration) {
    PointSample temp_22_0001 = point == null ? null : point.addRef();
    this.point = temp_22_0001 == null ? null : temp_22_0001.addRef();
    if (null != temp_22_0001)
      temp_22_0001.freeRef();
    if (null != point)
      point.freeRef();
    this.iteration = iteration;
  }


  /**
   * Frees this object and its associated resources.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    if (null != point)
      point.freeRef();
  }

  /**
   * Adds a reference to this Step.
   *
   * @return the Step with the added reference
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  Step addRef() {
    return (Step) super.addRef();
  }
}
