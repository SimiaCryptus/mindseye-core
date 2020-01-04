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
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;

public @com.simiacryptus.ref.lang.RefAware class TensorArray extends RegisteredObjectBase
    implements TensorList, Serializable {
  @Nonnull
  private final Tensor[] data;

  public TensorArray(@Nonnull final Tensor... data) {
    if (null == data)
      throw new IllegalArgumentException();
    if (0 >= data.length)
      throw new IllegalArgumentException();
    this.data = com.simiacryptus.ref.wrappers.RefArrays.copyOf(data, data.length);
    assert null != this.getData();
    for (@Nonnull
    Tensor tensor : this.getData()) {
      assert com.simiacryptus.ref.wrappers.RefArrays.equals(tensor.getDimensions(),
          this.getData()[0].getDimensions()) : com.simiacryptus.ref.wrappers.RefArrays.toString(tensor.getDimensions())
              + " != " + com.simiacryptus.ref.wrappers.RefArrays.toString(tensor.getDimensions());
    }
    register();
  }

  @Nonnull
  public Tensor[] getData() {
    return data;
  }

  @Nonnull
  @Override
  public int[] getDimensions() {
    return getData()[0].getDimensions();
  }

  public static <T> CharSequence toString(int limit, @Nonnull T... data) {
    return (data.length < limit) ? com.simiacryptus.ref.wrappers.RefArrays.toString(data)
        : "[" + com.simiacryptus.ref.wrappers.RefArrays.stream(data).limit(limit).map(x -> x.toString())
            .reduce((a, b) -> a + ", " + b).get() + ", ...]";
  }

  @Override
  @Nonnull
  public Tensor get(final int i) {
    return getData()[i];
  }

  @Override
  public int length() {
    return getData().length;
  }

  @Nonnull
  @Override
  public com.simiacryptus.ref.wrappers.RefStream<Tensor> stream() {
    return com.simiacryptus.ref.wrappers.RefArrays.stream(getData());
  }

  @Override
  public String toString() {
    return String.format("TensorArray{data=%s}", toString(9, getData()));
  }

  public void _free() {
    try {
      getData();
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public @Override @SuppressWarnings("unused") TensorArray addRef() {
    return (TensorArray) super.addRef();
  }

  public static @SuppressWarnings("unused") TensorArray[] addRefs(TensorArray[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TensorArray::addRef)
        .toArray((x) -> new TensorArray[x]);
  }

  public static @SuppressWarnings("unused") TensorArray[][] addRefs(TensorArray[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TensorArray::addRefs)
        .toArray((x) -> new TensorArray[x][]);
  }
}
