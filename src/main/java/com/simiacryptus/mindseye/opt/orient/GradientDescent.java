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

package com.simiacryptus.mindseye.opt.orient;

import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * This class implements the gradient descent algorithm.
 *
 * @docgenVersion 9
 */
public class GradientDescent extends OrientationStrategyBase<SimpleLineSearchCursor> {

  /**
   * @param subject     The subject to orient.
   * @param measurement The measurement to use.
   * @param monitor     The training monitor.
   * @return A new SimpleLineSearchCursor.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public SimpleLineSearchCursor orient(@Nullable final Trainable subject, @Nonnull final PointSample measurement,
                                       @Nonnull final TrainingMonitor monitor) {
    @Nonnull final DeltaSet<UUID> direction = measurement.delta.scale(-1);
    final double magnitude = direction.getMagnitude();
    if (Math.abs(magnitude) < 1e-10) {
      monitor.log(RefString.format("Zero gradient: %s", magnitude));
    } else if (Math.abs(magnitude) < 1e-5) {
      monitor.log(RefString.format("Low gradient: %s", magnitude));
    }
    SimpleLineSearchCursor simpleLineSearchCursor = new SimpleLineSearchCursor(subject, measurement, direction);
    simpleLineSearchCursor.setDirectionType("GD");
    return simpleLineSearchCursor;
  }

  /**
   * Resets the object.
   *
   * @docgenVersion 9
   */
  @Override
  public void reset() {
  }

  /**
   * Frees this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
  }

  /**
   * @return a new reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  GradientDescent addRef() {
    return (GradientDescent) super.addRef();
  }
}
