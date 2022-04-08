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

/**
 * DataSerializer is an interface for...
 *
 * @docgenVersion 9
 */
public interface DataSerializer {

  /**
   * Returns the size of the element.
   *
   * @docgenVersion 9
   */
  int getElementSize();

  /**
   * Returns the size of the header.
   *
   * @return the size of the header
   * @docgenVersion 9
   */
  default int getHeaderSize() {
    return 0;
  }

  /**
   * Copies an array of doubles into an array of bytes.
   *
   * @param from the array of doubles to copy
   * @param to   the array of bytes to copy into
   * @docgenVersion 9
   */
  void copy(double[] from, byte[] to);

  /**
   * Copies an array of bytes to an array of doubles.
   *
   * @param from the array of bytes to copy
   * @param to   the array of doubles to copy to
   * @docgenVersion 9
   */
  void copy(byte[] from, double[] to);

  /**
   * Converts an array of doubles into an array of bytes.
   *
   * @param from the array of doubles to convert
   * @return the resulting array of bytes
   * @docgenVersion 9
   */
  @Nonnull
  default byte[] toBytes(@Nonnull double[] from) {
    @Nonnull
    byte[] to = new byte[encodedSize(from)];
    copy(from, to);
    return to;
  }

  /**
   * Returns the encoded size of the given array.
   *
   * @param from the array to get the encoded size of
   * @return the encoded size of the array
   * @throws IllegalStateException if the encoded size is too large to fit in an int
   * @docgenVersion 9
   */
  default int encodedSize(@Nonnull double[] from) {
    long size = (long) from.length * getElementSize() + getHeaderSize();
    if (size > Integer.MAX_VALUE)
      throw new IllegalStateException();
    return (int) size;
  }

  /**
   * @param from the byte array to convert
   * @return the double array representation of the byte array
   * @docgenVersion 9
   */
  @Nonnull
  default double[] fromBytes(@Nonnull byte[] from) {
    @Nonnull
    double[] to = new double[decodedSize(from)];
    copy(from, to);
    return to;
  }

  /**
   * Returns the decoded size of the given byte array.
   *
   * @param from the byte array to decode
   * @return the decoded size
   * @docgenVersion 9
   */
  default int decodedSize(@Nonnull byte[] from) {
    return (from.length - getHeaderSize()) / getElementSize();
  }
}
