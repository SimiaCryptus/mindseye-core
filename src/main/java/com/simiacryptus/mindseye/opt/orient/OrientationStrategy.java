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
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.ref.lang.ReferenceCounting;

/**
 * The interface Orientation strategy.
 *
 * @param <T> the type parameter
 */
public interface OrientationStrategy<T extends LineSearchCursor> extends ReferenceCounting {

  /**
   * Orient t.
   *
   * @param subject     the subject
   * @param measurement the measurement
   * @param monitor     the monitor
   * @return the t
   */
  T orient(Trainable subject, PointSample measurement, TrainingMonitor monitor);

  /**
   * Reset.
   */
  void reset();

  /**
   * Free.
   */
  void _free();

  OrientationStrategy<T> addRef();
}
