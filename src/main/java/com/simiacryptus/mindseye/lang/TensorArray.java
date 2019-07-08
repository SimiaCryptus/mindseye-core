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

import com.simiacryptus.lang.ref.ReferenceCounting;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;

public class TensorArray extends RegisteredObjectBase implements TensorList, Serializable {
  @Nonnull
  private final Tensor[] data;

  private TensorArray(@Nonnull final Tensor... data) {
    if (null == data) throw new IllegalArgumentException();
    if (0 >= data.length) throw new IllegalArgumentException();
    this.data = Arrays.copyOf(data, data.length);
    assert null != this.getData();
    for (@Nonnull Tensor tensor : this.getData()) {
      assert Arrays.equals(tensor.getDimensions(), this.getData()[0].getDimensions()) : Arrays.toString(tensor.getDimensions()) + " != " + Arrays.toString(tensor.getDimensions());
      tensor.addRef();
    }
    register();
  }

  public static TensorArray create(final Tensor... data) {
    return new TensorArray(data);
  }

  @Nonnull
  public static TensorArray wrap(@Nonnull final Tensor... data) {
    @Nonnull TensorArray tensorArray = TensorArray.create(data);
    Arrays.stream(data).forEach(ReferenceCounting::freeRef);
    return tensorArray;
  }

  public static <T> CharSequence toString(int limit, @Nonnull T... data) {
    return (data.length < limit) ? Arrays.toString(data) : "[" + Arrays.stream(data).limit(limit).map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get() + ", ...]";
  }

  @Override
  public TensorArray addRef() {
    return (TensorArray) super.addRef();
  }

  @Override
  @Nonnull
  public Tensor get(final int i) {
    Tensor datum = getData()[i];
    datum.addRef();
    return datum;
  }

  @Nonnull
  @Override
  public int[] getDimensions() {
    return getData()[0].getDimensions();
  }

  @Override
  public int length() {
    return getData().length;
  }

  @Nonnull
  @Override
  public Stream<Tensor> stream() {
    return Arrays.stream(getData()).map(x -> {
      x.addRef();
      return x;
    });
  }

  @Override
  public String toString() {
    return String.format("TensorArray{data=%s}", toString(9, getData()));
  }

  @Override
  protected void _free() {
    try {
      for (@Nonnull final Tensor d : getData()) {
        d.freeRef();
      }
    } catch (@Nonnull final RuntimeException e) {
      throw e;
    } catch (@Nonnull final Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public Tensor[] getData() {
    return data;
  }
}
