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
import java.util.Arrays;

public class Step extends ReferenceCountingBase {
  public final long iteration;
  @Nullable
  public final PointSample point;
  public final long time = RefSystem.currentTimeMillis();

  Step(@Nullable final PointSample point, final long iteration) {
    PointSample temp_22_0001 = point == null ? null : point.addRef();
    this.point = temp_22_0001 == null ? null : temp_22_0001.addRef();
    if (null != temp_22_0001)
      temp_22_0001.freeRef();
    if (null != point)
      point.freeRef();
    this.iteration = iteration;
  }

  @Nullable
  public static @SuppressWarnings("unused")
  Step[] addRefs(@Nullable Step[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Step::addRef).toArray((x) -> new Step[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  Step[][] addRefs(@Nullable Step[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(Step::addRefs).toArray((x) -> new Step[x][]);
  }

  public @SuppressWarnings("unused")
  void _free() {
    if (null != point)
      point.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  Step addRef() {
    return (Step) super.addRef();
  }
}
