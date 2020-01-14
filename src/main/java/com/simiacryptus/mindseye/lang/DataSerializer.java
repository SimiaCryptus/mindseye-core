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

public interface DataSerializer {

  int getElementSize();

  default int getHeaderSize() {
    return 0;
  }

  void copy(double[] from, byte[] to);

  void copy(byte[] from, double[] to);

  @Nonnull
  default byte[] toBytes(@Nonnull double[] from) {
    @Nonnull
    byte[] to = new byte[encodedSize(from)];
    copy(from, to);
    return to;
  }

  default int encodedSize(@Nonnull double[] from) {
    long size = (long) from.length * getElementSize() + getHeaderSize();
    if (size > Integer.MAX_VALUE)
      throw new IllegalStateException();
    return (int) size;
  }

  @Nonnull
  default double[] fromBytes(@Nonnull byte[] from) {
    @Nonnull
    double[] to = new double[decodedSize(from)];
    copy(from, to);
    return to;
  }

  default int decodedSize(@Nonnull byte[] from) {
    return (from.length - getHeaderSize()) / getElementSize();
  }
}
