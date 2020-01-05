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

import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.util.Arrays;

public @RefAware
class LineSearchPoint extends ReferenceCountingBase {

  public final double derivative;
  public final PointSample point;

  public LineSearchPoint(final PointSample point, final double derivative) {
    this.point = point;
    this.derivative = derivative;
  }

  public static @SuppressWarnings("unused")
  LineSearchPoint[] addRefs(LineSearchPoint[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LineSearchPoint::addRef)
        .toArray((x) -> new LineSearchPoint[x]);
  }

  public static @SuppressWarnings("unused")
  LineSearchPoint[][] addRefs(LineSearchPoint[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LineSearchPoint::addRefs)
        .toArray((x) -> new LineSearchPoint[x][]);
  }

  @Nonnull
  @Override
  public String toString() {
    @Nonnull final StringBuffer sb = new StringBuffer("LineSearchPoint{");
    sb.append("point=").append(point);
    sb.append(", derivative=").append(derivative);
    sb.append('}');
    return sb.toString();
  }

  public void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  LineSearchPoint addRef() {
    return (LineSearchPoint) super.addRef();
  }
}
