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

public @com.simiacryptus.ref.lang.RefAware interface OrientationStrategy<T extends LineSearchCursor>
    extends ReferenceCounting {

  T orient(Trainable subject, PointSample measurement, TrainingMonitor monitor);

  void reset();

  public void _free();

  public OrientationStrategy<T> addRef();

  public static @SuppressWarnings("unused") OrientationStrategy[] addRefs(OrientationStrategy[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(OrientationStrategy::addRef)
        .toArray((x) -> new OrientationStrategy[x]);
  }

  public static @SuppressWarnings("unused") OrientationStrategy[][] addRefs(OrientationStrategy[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(OrientationStrategy::addRefs)
        .toArray((x) -> new OrientationStrategy[x][]);
  }
}
