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
 * This class represents a coordinate.
 * The coords array stores the coordinate values.
 * The index indicates the next empty slot in the coords array.
 *
 * @docgenVersion 9
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
   * Returns an array of integers containing the x- and y-coordinates
   * of the object.
   *
   * @docgenVersion 9
   */
  public int[] getCoords() {
    return coords;
  }

  /**
   * Sets the coordinates of the object.
   *
   * @docgenVersion 9
   */
  void setCoords(final int[] coords) {
    this.coords = coords;
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int getIndex() {
    return index;
  }

  /**
   * Sets the index.
   *
   * @docgenVersion 9
   */
  void setIndex(final int index) {
    this.index = index;
  }

  /**
   * Add two integers.
   *
   * @return an array containing the two integers.
   * @docgenVersion 9
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
   * Transposes the x and y coordinates of a point.
   *
   * @docgenVersion 9
   */
  public static int transposeXY(int rows, int cols, int index) {
    final int filterBandX = index % rows;
    final int filterBandY = (index - filterBandX) / rows;
    assert index == filterBandY * rows + filterBandX;
    return filterBandX * cols + filterBandY;
  }

  /**
   * Returns true if the object is the same as the one given, false otherwise.
   *
   * @docgenVersion 9
   */
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

  /**
   * Returns a hash code value for the object. This method is
   * supported for the benefit of hash tables such as those provided by
   * HashMap.
   *
   * @docgenVersion 9
   */
  @Override
  public int hashCode() {
    return Integer.hashCode(index) ^ RefArrays.hashCode(coords);
  }

  /**
   * Returns a string representation of this object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return RefArrays.toString(coords) + "<" + index + ">";
  }

  /**
   * Returns a copy of this Coordinate.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Coordinate copy() {
    return new Coordinate(index, RefArrays.copyOf(coords, coords.length));
  }
}
