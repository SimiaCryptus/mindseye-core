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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;

public @RefAware
class TensorArray extends RegisteredObjectBase implements TensorList, Serializable {
  @Nonnull
  private final Tensor[] data;

  public TensorArray(@Nonnull final Tensor... data) {
    if (null == data) {
      ReferenceCounting.freeRefs(data);
      throw new IllegalArgumentException();
    }
    if (0 >= data.length) {
      ReferenceCounting.freeRefs(data);
      throw new IllegalArgumentException();
    }
    {
      Tensor[] temp_23_0001 = RefArrays
          .copyOf(Tensor.addRefs(data), data.length);
      this.data = Tensor.addRefs(temp_23_0001);
      if (null != temp_23_0001)
        ReferenceCounting.freeRefs(temp_23_0001);
    }
    ReferenceCounting.freeRefs(data);
    Tensor[] temp_23_0002 = this.getData();
    assert null != temp_23_0002;
    if (null != temp_23_0002)
      ReferenceCounting.freeRefs(temp_23_0002);
    for (@Nonnull
        Tensor tensor : this.getData()) {
      assert RefArrays.equals(tensor.getDimensions(),
          this.getData()[0].getDimensions()) : RefArrays.toString(tensor.getDimensions()) + " != "
          + RefArrays.toString(tensor.getDimensions());
    }
    register();
  }

  @Nonnull
  public Tensor[] getData() {
    return Tensor.addRefs(data);
  }

  @Nonnull
  @Override
  public int[] getDimensions() {
    return getData()[0].getDimensions();
  }

  public static <T> CharSequence toString(int limit, @Nonnull T... data) {
    return (data.length < limit) ? RefArrays.toString(data)
        : "[" + RefArrays.stream(data).limit(limit).map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()
        + ", ...]";
  }

  public static @SuppressWarnings("unused")
  TensorArray[] addRefs(TensorArray[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TensorArray::addRef)
        .toArray((x) -> new TensorArray[x]);
  }

  public static @SuppressWarnings("unused")
  TensorArray[][] addRefs(TensorArray[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TensorArray::addRefs)
        .toArray((x) -> new TensorArray[x][]);
  }

  @Override
  @Nonnull
  public Tensor get(final int i) {
    return getData()[i];
  }

  @Override
  public int length() {
    Tensor[] temp_23_0004 = getData();
    int temp_23_0003 = temp_23_0004.length;
    if (null != temp_23_0004)
      ReferenceCounting.freeRefs(temp_23_0004);
    return temp_23_0003;
  }

  @Nonnull
  @Override
  public RefStream<Tensor> stream() {
    return RefArrays.stream(getData());
  }

  @Override
  public String toString() {
    return RefString.format("TensorArray{data=%s}", toString(9, getData()));
  }

  public void _free() {
    ReferenceCounting.freeRefs(data);
    try {
      ReferenceCounting.freeRefs(getData());
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public @Override
  @SuppressWarnings("unused")
  TensorArray addRef() {
    return (TensorArray) super.addRef();
  }
}
