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

import com.simiacryptus.ref.wrappers.RefArrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * The type Coordinate.
 */
public final class Coordinate implements Serializable {
  /**
   * The Coords.
   */
  protected int[] coords;
  /**
   * The Index.
   */
  protected int index;

  /**
   * Instantiates a new Coordinate.
   */
  public Coordinate() {
    this(-1, null);
  }

  /**
   * Instantiates a new Coordinate.
   *
   * @param index  the index
   * @param coords the coords
   */
  public Coordinate(final int index, final int[] coords) {
    super();
    this.index = index;
    this.coords = coords;
  }

  /**
   * Get coords int [ ].
   *
   * @return the int [ ]
   */
  public int[] getCoords() {
    return coords;
  }

  /**
   * Sets coords.
   *
   * @param coords the coords
   */
  void setCoords(final int[] coords) {
    this.coords = coords;
  }

  /**
   * Gets index.
   *
   * @return the index
   */
  public int getIndex() {
    return index;
  }

  /**
   * Sets index.
   *
   * @param index the index
   */
  void setIndex(final int index) {
    this.index = index;
  }

  /**
   * Add int [ ].
   *
   * @param a the a
   * @param b the b
   * @return the int [ ]
   */
  @Nonnull
  public static int[] add(@Nonnull final int[] a, @Nonnull final int[] b) {
    @Nonnull final int[] r = new int[Math.max(a.length, b.length)];
    for (int i = 0; i < r.length; i++) {
      r[i] = (a.length <= i ? 0 : a[i]) + (b.length <= i ? 0 : b[i]);
    }
    return r;
  }

  /**
   * Transpose xy int.
   *
   * @param rows  the rows
   * @param cols  the cols
   * @param index the index
   * @return the int
   */
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
    return Integer.hashCode(index) ^ RefArrays.hashCode(coords);
  }

  @Nonnull
  @Override
  public String toString() {
    return RefArrays.toString(coords) + "<" + index + ">";
  }

  /**
   * Copy coordinate.
   *
   * @return the coordinate
   */
  @Nonnull
  public Coordinate copy() {
    return new Coordinate(index, RefArrays.copyOf(coords, coords.length));
  }
}
