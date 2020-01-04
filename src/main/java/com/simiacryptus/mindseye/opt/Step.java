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

public @com.simiacryptus.ref.lang.RefAware
class Step extends ReferenceCountingBase {
  public final long iteration;
  public final PointSample point;
  public final long time = System.currentTimeMillis();

  Step(final PointSample point, final long iteration) {
    this.point = point;
    this.iteration = iteration;
  }

  public static @SuppressWarnings("unused")
  Step[] addRefs(Step[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(Step::addRef).toArray((x) -> new Step[x]);
  }

  public static @SuppressWarnings("unused")
  Step[][] addRefs(Step[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(Step::addRefs).toArray((x) -> new Step[x][]);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  Step addRef() {
    return (Step) super.addRef();
  }
}
