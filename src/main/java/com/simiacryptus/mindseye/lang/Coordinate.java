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

package com.simiacryptus.mindseye.lang;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

public final @com.simiacryptus.ref.lang.RefAware
class Coordinate implements Serializable {
  protected int[] coords;
  protected int index;

  public Coordinate() {
    this(-1, null);
  }

  public Coordinate(final int index, final int[] coords) {
    super();
    this.index = index;
    this.coords = coords;
  }

  public int[] getCoords() {
    return coords;
  }

  @Nonnull
  void setCoords(final int[] coords) {
    this.coords = coords;
  }

  public int getIndex() {
    return index;
  }

  @Nonnull
  void setIndex(final int index) {
    this.index = index;
  }

  @Nonnull
  public static int[] add(@Nonnull final int[] a, @Nonnull final int[] b) {
    @Nonnull final int[] r = new int[Math.max(a.length, b.length)];
    for (int i = 0; i < r.length; i++) {
      r[i] = (a.length <= i ? 0 : a[i]) + (b.length <= i ? 0 : b[i]);
    }
    return r;
  }

  public static int transposeXY(int rows, int cols, int index) {
    final int filterBandX = index % rows;
    final int filterBandY = (index - filterBandX) / rows;
    assert index == filterBandY * rows + filterBandX;
    return filterBandX * cols + filterBandY;
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return index == ((Coordinate) obj).index;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(index) ^ com.simiacryptus.ref.wrappers.RefArrays.hashCode(coords);
  }

  @Override
  public String toString() {
    return com.simiacryptus.ref.wrappers.RefArrays.toString(coords) + "<" + index + ">";
  }

  public Coordinate copy() {
    return new Coordinate(index, com.simiacryptus.ref.wrappers.RefArrays.copyOf(coords, coords.length));
  }
}
