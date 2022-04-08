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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.simiacryptus.lang.SerializableFunction;
import com.simiacryptus.ref.lang.RecycleBin;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.FastRandom;
import com.simiacryptus.util.data.DoubleStatistics;
import com.simiacryptus.util.data.ScalarStatistics;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.*;
import java.util.function.*;
import java.util.stream.DoubleStream;

/**
 * This class represents a Tensor.
 *
 * @param dimensions the dimensions of the Tensor
 * @param strides    the strides of the Tensor
 * @param data       the data of the Tensor
 * @param id         the id of the Tensor
 * @docgenVersion 9
 */
@SuppressWarnings("serial")
public final class Tensor extends ReferenceCountingBase implements Serializable, ZipSerializable {
  /**
   * The constant json_precision.
   */
  @Nonnull
  public static final DataSerializer json_precision = SerialPrecision.Float;
  /**
   * The Dimensions.
   */
  @Nullable
  protected final int[] dimensions;
  /**
   * The Strides.
   */
  @Nullable
  protected final int[] strides;
  /**
   * The Data.
   */
  @Nullable
  protected volatile double[] data;
  /**
   * The Id.
   */
  @Nullable
  protected volatile UUID id;

  private Tensor() {
    super();
    data = null;
    strides = null;
    dimensions = null;
  }

  /**
   * Instantiates a new Tensor.
   *
   * @param ds the ds
   */
  public Tensor(@Nonnull final double... ds) {
    this(ds, ds.length);
  }

  /**
   * Instantiates a new Tensor.
   *
   * @param data the data
   * @param dims the dims
   */
  public Tensor(@Nullable final double[] data, @Nonnull final int... dims) {
    if (null != data && 0 >= data.length)
      throw new IllegalArgumentException();
    Tensor.length(dims);
    if (Tensor.length(dims) <= 0)
      throw new IllegalArgumentException();
    if (null != data && Tensor.length(dims) != data.length)
      throw new IllegalArgumentException(RefArrays.toString(dims) + " != " + data.length);
    dimensions = 0 == dims.length ? new int[]{} : RefArrays.copyOf(dims, dims.length);
    strides = Tensor.getSkips(dims);
    //this.data = data;// Arrays.copyOf(data, data.length);
    if (null != data) {
      this.data = RecycleBin.DOUBLES.copyOf(data, data.length);
    }
    assert isValid();
    //assert (null == data || Tensor.length(dims) == data.length);
  }

  private Tensor(@Nonnull int[] dims, @Nullable double[] data) {
    this(dims, Tensor.getSkips(dims), data);
  }

  private Tensor(@Nullable int[] dimensions,
                 @Nullable int[] strides, @Nullable double[] data) {
    assert dimensions != null;
    if (Tensor.length(dimensions) >= Integer.MAX_VALUE)
      throw new IllegalArgumentException();
    assert null == data || data.length == Tensor.length(dimensions);
    this.dimensions = dimensions;
    this.strides = strides;
    this.data = data;
    assert isValid();
  }

  /**
   * Instantiates a new Tensor.
   *
   * @param data the data
   * @param dims the dims
   */
  public Tensor(@Nullable final float[] data, @Nonnull final int... dims) {
    if (Tensor.length(dims) >= Integer.MAX_VALUE)
      throw new IllegalArgumentException();
    dimensions = RefArrays.copyOf(dims, dims.length);
    strides = Tensor.getSkips(dims);
    if (null != data) {
      this.data = RecycleBin.DOUBLES.obtain(data.length);// Arrays.copyOf(data, data.length);
      RefArrays.parallelSetAll(this.data, i -> {
        final double v = data[i];
        return Double.isFinite(v) ? v : 0;
      });
      assert RefArrays.stream(this.data).allMatch(Double::isFinite);
    }
    assert isValid();
    //assert (null == data || Tensor.length(dims) == data.length);
  }

  /**
   * Instantiates a new Tensor.
   *
   * @param dims the dims
   */
  public Tensor(@Nonnull final int... dims) {
    this((double[]) null, dims);
    assert dims.length > 0;
  }

  /**
   * Converts this BufferedImage into a new Image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static BufferedImage toImage(Tensor tensor) {
    BufferedImage image = tensor.toImage();
    tensor.freeRef();
    return image;
  }

  /**
   * Converts a BufferedImage to an RGB image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static BufferedImage toRgbImage(Tensor tensor) {
    BufferedImage image = tensor.toRgbImage();
    tensor.freeRef();
    return image;
  }

  /**
   * Returns an array of doubles.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public double[] getData() {
    assertAlive();
    if (null == data) {
      synchronized (this) {
        if (null == data) {
          assert dimensions != null;
          final int length = Tensor.length(dimensions);
          data = RecycleBin.DOUBLES.obtain(length);
          assert null != data;
          assert length == data.length;
        }
      }
    }
    assert isValid();
    assert null != data;
    return data;
  }

  /**
   * Returns an array of floats containing the data.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public float[] getDataAsFloats() {
    return Tensor.toFloats(getData());
  }

  /**
   * Returns an array of integers representing the dimensions of the object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public final int[] getDimensions() {
    assert dimensions != null;
    return RefArrays.copyOf(dimensions, dimensions.length);
  }

  /**
   * Returns an object containing statistical information about the
   * doubles in this RDD.
   *
   * @docgenVersion 9
   */
  @NotNull
  public DoubleStatistics getDoubleStatistics() {
    return new DoubleStatistics().accept(getData());
  }

  /**
   * Returns the UUID of this object.
   *
   * @docgenVersion 9
   */
  @Nullable
  public UUID getId() {
    if (id == null) {
      synchronized (this) {
        if (id == null) {
          id = UUID.randomUUID();
        }
      }
    }
    return id;
  }

  /**
   * Sets the id.
   *
   * @docgenVersion 9
   */
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  /**
   * @return RefStream<double [ ]>
   * @JsonIgnore
   * @docgenVersion 9
   */
  @Nonnull
  //@JsonIgnore
  public RefStream<double[]> getPixelStream() {
    int[] dimensions = getDimensions();
    int width = dimensions[0];
    int height = dimensions[1];
    int bands = dimensions[2];
    return RefIntStream.range(0, width).mapToObj(x -> x).parallel()
        .flatMap((Function<Integer, RefStream<double[]>>) x1 -> {
          return RefIntStream.range(0, height).mapToObj(y -> y).map(y -> {
            return this.getPixel(x1, y, bands);
          });
        });
  }

  /**
   * Returns the scalar statistics.
   *
   * @docgenVersion 9
   */
  public ScalarStatistics getScalarStatistics() {
    return new ScalarStatistics().add(getData());
  }

  /**
   * Checks if the current state is valid.
   *
   * @return true if the state is valid, false otherwise
   * @docgenVersion 9
   */
  public boolean isValid() {
    assert dimensions != null;
    return !isFreed() && (null == this.data || this.data.length == Tensor.length(dimensions));
  }

  /**
   * Sets all the fields in the object.
   *
   * @docgenVersion 9
   */
  public void setAll(double v) {
    @Nullable final double[] data = getData();
    for (int i = 0; i < data.length; i++) {
      data[i] = v;
    }
  }

  /**
   * Sets the value of this cell using its coordinates.
   *
   * @docgenVersion 9
   */
  public void setByCoord(@RefAware @Nonnull ToDoubleFunction<Coordinate> f) {
    setByCoord(f, true);
  }

  /**
   * Sets the bytes for this object.
   *
   * @docgenVersion 9
   */
  public void setBytes(byte[] bytes) {
    setBytes(bytes, json_precision);
  }

  /**
   * Sets the parallelism for the RDD by index.
   *
   * @docgenVersion 9
   */
  public void setParallelByIndex(@Nonnull final IntToDoubleFunction f) {
    RefIntStream.range(0, length()).parallel().forEach(c -> {
      set(c, f.applyAsDouble(c));
    });
  }

  /**
   * Returns a Tensor from a JSON object.
   *
   * @docgenVersion 9
   */
  @Nullable
  @SuppressWarnings("unused")
  public static Tensor fromJson(@Nullable final JsonElement json, @Nullable Map<CharSequence, byte[]> resources) {
    if (null == json)
      return null;
    if (json.isJsonArray()) {
      final JsonArray array = json.getAsJsonArray();
      final int size = array.size();
      if (array.get(0).isJsonPrimitive()) {
        final double[] doubles = RefIntStream.range(0, size).mapToObj(array::get).mapToDouble(JsonElement::getAsDouble).toArray();
        @Nonnull
        Tensor tensor = new Tensor(doubles);
        assert tensor.isValid();
        return tensor;
      } else {
        final RefList<Tensor> elements = RefIntStream.range(0, size).mapToObj(array::get).map(element -> {
          return Tensor.fromJson(element, resources);
        }).collect(RefCollectors.toList());
        Tensor temp_33_0010 = elements.get(0);
        @Nonnull final int[] dimensions = temp_33_0010.getDimensions();
        temp_33_0010.freeRef();
        if (!elements.stream().allMatch(t -> {
          boolean temp_33_0001 = RefArrays.equals(dimensions, t.getDimensions());
          t.freeRef();
          return temp_33_0001;
        })) {
          elements.freeRef();
          throw new IllegalArgumentException();
        }
        @Nonnull final int[] newDdimensions = RefArrays.copyOf(dimensions, dimensions.length + 1);
        newDdimensions[dimensions.length] = size;
        @Nonnull final Tensor tensor = new Tensor(newDdimensions);
        @Nullable final double[] data = tensor.getData();
        for (int i = 0; i < size; i++) {
          Tensor temp_33_0011 = elements.get(i);
          @Nullable final double[] e = temp_33_0011.getData();
          temp_33_0011.freeRef();
          RefSystem.arraycopy(e, 0, data, i * e.length, e.length);
        }
        elements.freeRef();
        assert tensor.isValid();
        return tensor;
      }
    } else if (json.isJsonObject()) {
      JsonObject jsonObject = json.getAsJsonObject();
      @Nonnull
      int[] dims = fromJsonArray(jsonObject.getAsJsonArray("dims"));
      @Nonnull
      Tensor tensor = new Tensor(dims);
      SerialPrecision precision = SerialPrecision.valueOf(jsonObject.getAsJsonPrimitive("precision").getAsString());
      JsonElement base64 = jsonObject.get("base64");
      if (null == base64) {
        if (null == resources) {
          tensor.freeRef();
          throw new IllegalArgumentException("No Data Resources");
        }
        CharSequence resourceId = jsonObject.getAsJsonPrimitive("resource").getAsString();
        tensor.setBytes(resources.get(resourceId), precision);
      } else {
        tensor.setBytes(Base64.getDecoder().decode(base64.getAsString()), precision);
      }
      assert tensor.isValid();
      JsonElement id = jsonObject.get("id");
      if (null != id) {
        tensor.setId(UUID.fromString(id.getAsString()));
      }
      return tensor;
    } else {
      @Nonnull
      Tensor tensor = new Tensor(json.getAsJsonPrimitive().getAsDouble());
      assert tensor.isValid();
      return tensor;
    }
  }

  /**
   * Returns the length of the string.
   *
   * @docgenVersion 9
   */
  public static int length(@Nonnull int... dims) {
    long total = 1;
    if (null == dims) return 0;
    if (0 == dims.length) return 0;
    for (final int dim : dims) {
      assert 0 <= dim : RefArrays.toString(dims);
      total *= dim;
      assert 0 <= total : RefArrays.toString(dims);
      assert total < Integer.MAX_VALUE : RefArrays.toString(dims);
    }
    return (int) total;
  }

  /**
   * Returns a Tensor with the given RGB values.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static Tensor fromRGB(@Nonnull final BufferedImage img) {
    final int width = img.getWidth();
    final int height = img.getHeight();
    @Nonnull final Tensor a = new Tensor(width, height, 3);
    //a.watch();
    RefIntStream.range(0, width).parallel().forEach(x -> {
      @Nonnull final int[] coords = {0, 0, 0};
      RefIntStream.range(0, height).forEach(y -> {
        coords[0] = x;
        coords[1] = y;
        coords[2] = 0;
        a.set(coords, img.getRGB(x, y) & 0xFF);
        coords[2] = 1;
        a.set(coords, img.getRGB(x, y) >> 8 & 0xFF);
        coords[2] = 2;
        a.set(coords, img.getRGB(x, y) >> 16 & 0x0FF);
      });
    });
    return a;
  }

  /**
   * Returns an array of doubles.
   *
   * @docgenVersion 9
   */
  public static double[] getDoubles(@Nonnull final RefDoubleStream stream, final int dim) {
    final double[] doubles = RecycleBin.DOUBLES.obtain(dim);
    stream.forEach(new DoubleConsumer() {
      int j = 0;

      /**
       * Accepts an input.
       *
       *   @docgenVersion 9
       */
      @Override
      public void accept(final double value) {
        doubles[j++] = value;
      }
    });
    return doubles;
  }

  /**
   * Tensor product
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static Tensor product(@Nonnull final Tensor left, @Nonnull final Tensor right) {
    if (left.length() == 1 && right.length() != 1) {
      return Tensor.product(right, left);
    }
    assert left.length() == right.length() || 1 == right.length();
    @Nonnull final Tensor result = new Tensor(left.getDimensions());
    @Nullable final double[] resultData = result.getData();
    @Nullable final double[] leftData = left.getData();
    left.freeRef();
    @Nullable final double[] rightData = right.getData();
    right.freeRef();
    for (int i = 0; i < resultData.length; i++) {
      final double l = leftData[i];
      final double r = rightData[1 == rightData.length ? 0 : i];
      resultData[i] = l * r;
    }
    return result;
  }

  /**
   * Converts an array of floats to an array of bytes.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static float[] toFloats(@Nonnull final double[] data) {
    return copy(data, new float[data.length]);
  }

  /**
   * Returns a copy of the array of floats.
   *
   * @docgenVersion 9
   */
  public static float[] copy(@Nonnull double[] src, float[] buffer) {
    for (int i = 0; i < src.length; i++) {
      buffer[i] = (float) src[i];
    }
    return buffer;
  }

  /**
   * Converts this JsonArray to a Scala JsonArray.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static JsonArray toJsonArray(@Nonnull int[] ints) {
    @Nonnull
    JsonArray dim = new JsonArray();
    for (int i = 0; i < ints.length; i++) {
      dim.add(new JsonPrimitive(ints[i]));
    }
    return dim;
  }

  /**
   * Returns an array of integers from a JSON array.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static int[] fromJsonArray(@Nonnull JsonArray ints) {
    @Nonnull
    int[] array = new int[ints.size()];
    for (int i = 0; i < ints.size(); i++) {
      array[i] = ints.get(i).getAsInt();
    }
    return array;
  }

  /**
   * Inverts the dimensions of a tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static Tensor invertDimensions(@Nonnull Tensor tensor) {
    Tensor temp_33_0005 = tensor.rearrange(Tensor::reverse);
    tensor.freeRef();
    return temp_33_0005;
  }

  /**
   * Returns an array of all permutations of the given int array.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static int[] permute(@Nonnull int[] key, int[] data, final int[] dimensions) {
    @Nonnull
    int[] copy = new int[key.length];
    for (int i = 0; i < key.length; i++) {
      int k = key[i];
      if (k == Integer.MAX_VALUE) {
        copy[i] = dimensions[0] - data[0] - 1;
      } else if (k < 0) {
        copy[i] = dimensions[-k] - data[-k] - 1;
      } else {
        copy[i] = data[k];
      }
    }
    return copy;
  }

  /**
   * Reverses the order of the elements in the array.
   *
   * @return the array with its elements in reverse order
   * @docgenVersion 9
   */
  @Nonnull
  public static int[] reverse(@Nonnull int[] dimensions) {
    return reverseInPlace(RefArrays.copyOf(dimensions, dimensions.length));
  }

  /**
   * Reverses the array in place.
   *
   * @return the reversed array
   * @docgenVersion 9
   */
  @Nullable
  public static int[] reverseInPlace(@Nullable final int[] array) {
    if (array == null) {
      return null;
    }
    int i = 0;
    int j = array.length - 1;
    int tmp;
    while (i < j) {
      tmp = array[j];
      array[j] = array[i];
      array[i] = tmp;
      j--;
      i++;
    }
    return array;
  }

  /**
   * Pretty-prints this CharSequence.
   *
   * @docgenVersion 9
   */
  public static CharSequence prettyPrint(double[] doubles) {
    @Nonnull
    Tensor t = new Tensor(doubles);
    String temp_33_0002 = t.prettyPrint();
    t.freeRef();
    return temp_33_0002;
  }

  /**
   * Returns a SerializableFunction that can be used to select a Tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static SerializableFunction<Tensor, Tensor> select(@Nonnull Coordinate... reducedCoords) {
    return tensor -> {
      Tensor reduced = new Tensor(reducedCoords.length);
      final ToDoubleFunction<Coordinate> f = RefUtil.wrapInterface(c2 -> tensor.get(reducedCoords[c2.getIndex()]),
          tensor == null ? null : tensor.addRef());
      reduced.setByCoord(f, false);
      if (null != tensor)
        tensor.freeRef();
      return reduced;
    };
  }

  /**
   * Adds two tensors together.
   *
   * @docgenVersion 9
   */
  @NotNull
  public static Tensor add(Tensor a, Tensor b) {
    if (1 == a.currentRefCount()) {
      a.addInPlace(b);
      return a;
    } else {
      try {
        return a.add(b);
      } finally {
        a.freeRef();
      }
    }
  }

  /**
   * Returns a double that is bound to 8 bits.
   *
   * @docgenVersion 9
   */
  private static double bound8bit(final double value) {
    final int max = 0xFF;
    final int min = 0;
    return value < min ? min : value > max ? max : value;
  }

  /**
   * Returns an 8-bit integer.
   *
   * @docgenVersion 9
   */
  private static int bound8bit(final int value) {
    final int max = 0xFF;
    final int min = 0;
    return value < min ? min : value > max ? max : value;
  }

  /**
   * Returns an array of integers that represent
   * the number of skips needed for each element in
   * the array.
   *
   * @docgenVersion 9
   */
  @Nonnull
  private static int[] getSkips(@Nonnull final int[] dims) {
    @Nonnull final int[] skips = new int[dims.length];
    for (int i = 0; i < skips.length; i++) {
      if (i == 0) {
        skips[0] = 1;
      } else {
        skips[i] = skips[i - 1] * dims[i - 1];
      }
    }
    return skips;
  }

  /**
   * Fills the object with a value.
   *
   * @docgenVersion 9
   */
  public void fill(int fromIndex, int toIndex, double val) {
    Arrays.fill(getData(), fromIndex, toIndex, val);
  }

  /**
   * Returns a copy of the data in the form of an array of doubles.
   *
   * @docgenVersion 9
   */
  public double[] copyData() {
    return Arrays.copyOf(getData(), length());
  }

  /**
   * Returns a new stream consisting of the elements of this stream,
   * each doubled.
   *
   * @docgenVersion 9
   */
  public RefDoubleStream doubleStream() {
    return RefDoubleStream.of(getData()).track(addRef());
  }

  /**
   * Returns an array of doubles representing the pixel.
   *
   * @docgenVersion 9
   */
  public double[] getPixel(int... coords) {
    return getPixel(coords[0], coords[1], getDimensions()[2]);
  }

  /**
   * Returns an array of doubles representing the pixel.
   *
   * @docgenVersion 9
   */
  public double[] getPixel(int x, int y, int bands) {
    return RefIntStream.range(0, bands).mapToDouble(c -> get(x, y, c)).toArray();
  }

  /**
   * Retrieves the value of a pixel.
   *
   * @docgenVersion 9
   */
  public void getPixel(int x, int y, double[] refPixel) {
    final double[] data = getData();
    RefIntStream.range(0, dimensions[2]).forEach(c -> {
      final int index = index(x, y, c);
      assert index >= 0;
      if (index >= data.length) {
        throw new IllegalArgumentException(Arrays.toString(new int[]{x, y, c}));
      }
      refPixel[c] = data[index];
    });
  }

  /**
   * Sets the value of a pixel.
   *
   * @docgenVersion 9
   */
  public void setPixel(int x, int y, double[] refPixel) {
    final double[] data = getData();
    RefIntStream.range(0, dimensions[2]).forEach(c -> {
      final int index = index(x, y, c);
      assert index >= 0;
      if (index >= data.length) {
        throw new IllegalArgumentException(Arrays.toString(new int[]{x, y, c}));
      }
      data[index] = refPixel[c];
    });
  }

  /**
   * Rearranges this tensor according to the specified permutation.
   *
   * @param permutation the permutation to apply
   * @return the rearranged tensor
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor rearrange(@Nonnull UnaryOperator<int[]> fn) {
    return rearrange(fn, fn.apply(getDimensions()));
  }

  /**
   * Rearranges this tensor according to the specified permutation.
   *
   * @param permutation the permutation to apply
   * @return the rearranged tensor
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor rearrange(@Nonnull UnaryOperator<int[]> fn, int[] outputDims) {
    @Nonnull
    Tensor result = new Tensor(outputDims);
    coordStream(false).forEach(c -> {
      int[] inCoords = c.getCoords();
      int[] outCoords = fn.apply(inCoords);
      result.set(outCoords, get(c));
    });
    return result;
  }

  /**
   * Add the elements of this buffer in-place.
   *
   * @docgenVersion 9
   */
  public void addInPlace(@Nonnull final Tensor tensor) {
    try {
      assertAlive();
      int[] tensorDimensions = tensor.getDimensions();
      if (!Arrays.equals(getDimensions(), tensorDimensions)) {
        throw new AssertionError(String.format("%s != %s", Arrays.toString(getDimensions()), Arrays.toString(tensorDimensions)));
      }
      double[] toAdd = tensor.getData();
      double[] data = getData();
      int length = length();
      int shards = Math.max(1, Math.min(8, length / 64));
      double shardSize = (double) length / shards;
      RefDoubleStream.iterate(0, x -> x + shardSize).limit(shards).parallel().forEach(start -> {
        int end = (int) Math.min(length, Math.floor(start + shardSize));
        for (int i = (int) Math.floor(start); i < end; i++) {
          data[i] += toAdd[i];
        }
      });
    } finally {
      if (tensor != null) tensor.freeRef();
    }
  }

  /**
   * Adds an element to the end of the list.
   *
   * @docgenVersion 9
   */
  public void add(@Nonnull final Coordinate coords, final double value) {
    add(coords.getIndex(), value);
  }

  /**
   * Adds an element to the end of the list.
   *
   * @docgenVersion 9
   */
  public final void add(final int index, final double value) {
    getData()[index] += value;
  }

  /**
   * Adds an element to the end of the list.
   *
   * @docgenVersion 9
   */
  public void add(@Nonnull final int[] coords, final double value) {
    add(index(coords), value);
  }

  /**
   * Adds two tensors together.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor add(@Nonnull final Tensor right) {
    try {
      int[] dimensions = getDimensions();
      assert RefArrays.equals(dimensions, right.getDimensions());
      final double[] data = getData();
      final double[] rightData = right.getData();
      return new Tensor(dimensions,
          RefIntStream.range(0, length()).mapToDouble(i -> rightData[i] + data[i]).toArray());
    } finally {
      right.freeRef();
    }
  }

  /**
   * Returns a stream of coordinates.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public RefStream<Coordinate> coordStream(boolean parallel) {
    //ConcurrentHashSet<Object> distinctBuffer = new ConcurrentHashSet<>();
    //assert distinctBuffer.add(coordinate.copy()) : String.format("Duplicate: %s in %s", coordinate, distinctBuffer);
    assert dimensions != null;
    return RefStreamSupport.stream(RefSpliterators.spliterator(new RefIteratorBase<Coordinate>() {

      @Nonnull
      final Coordinate coordinate = new Coordinate();
      @Nonnull
      final int[] val = new int[dimensions.length];
      @Nonnull
      final int[] safeCopy = new int[dimensions.length];
      int cnt = 0;

      /**
       * Checks if there is another element in the iterator.
       *
       * @return true if there is another element in the iterator, false otherwise
       *
       *   @docgenVersion 9
       */
      @Override
      public boolean hasNext() {
        return cnt < length();
      }

      /**
       * Returns the next coordinate.
       *
       *   @docgenVersion 9
       */
      @Nonnull
      @Override
      public synchronized Coordinate next() {
        if (0 < cnt) {
          for (int i = 0; i < val.length; i++) {
            if (++val[i] >= dimensions[i]) {
              val[i] = 0;
            } else {
              break;
            }
          }
        }
        RefSystem.arraycopy(val, 0, safeCopy, 0, val.length);
        coordinate.setIndex(cnt++);
        coordinate.setCoords(safeCopy);
        return parallel ? coordinate.copy() : coordinate;
      }

      /**
       * Frees the memory associated with this object.
       *
       *   @docgenVersion 9
       */
      public @SuppressWarnings("unused")
      void _free() {
        super._free();
      }
    }, length(), Spliterator.ORDERED), parallel);
  }

  /**
   * Returns the length of the string.
   *
   * @docgenVersion 9
   */
  public int length() {
    assertAlive();
    if (null != data) {
      return data.length;
    } else {
      assert dimensions != null;
      return Tensor.length(dimensions);
    }
  }

  /**
   * Returns a copy of this Tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor copy() {
    assertAlive();
    assert dimensions != null;
    return new Tensor(RecycleBin.DOUBLES.copyOf(getData(), getData().length),
        RefArrays.copyOf(dimensions, dimensions.length));
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
    @Nullable final Tensor other = (Tensor) obj;
    if (0 == currentRefCount()) {
      other.freeRef();
      return false;
    }
    if (0 == other.currentRefCount()) {
      other.freeRef();
      return false;
    }
    if (!RefArrays.equals(dimensions, other.dimensions)) {
      other.freeRef();
      return false;
    }
    boolean temp_33_0003 = RefArrays.equals(getData(), other.getData());
    other.freeRef();
    return temp_33_0003;
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(@Nonnull final Coordinate coords) {
    return get(coords.getIndex());
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(final int index) {
    return getData()[index];
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(final int c1, final int c2) {
    return get(index(c1, c2));
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(final int c1, final int c2, final int c3) {
    final int index = index(c1, c2, c3);
    final double[] data = getData();
    assert index >= 0;
    if (index >= data.length) {
      throw new IllegalArgumentException(Arrays.toString(new int[]{c1, c2, c3}));
    }
    return data[index];
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(final int c1, final int c2, final int c3, final int c4, final int... coords) {
    return get(index(c1, c2, c3, c4, coords));
  }

  /**
   * This function returns nothing.
   *
   * @docgenVersion 9
   */
  public void get(@Nonnull final double[] bufferArray) {
    RefSystem.arraycopy(getData(), 0, bufferArray, 0, length());
  }

  /**
   * Returns the double value of this object.
   *
   * @docgenVersion 9
   */
  public double get(@Nonnull final int[] coords) {
    return get(index(coords));
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
    final int prime = 31;
    int result = 1;
    result = prime * result + RefArrays.hashCode(getData());
    result = prime * result + RefArrays.hashCode(dimensions);
    return result;
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int index(final int c1) {
    int v = 0;
    assert strides != null;
    v += strides[0] * c1;
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int index(final int c1, final int c2) {
    int v = 0;
    assert strides != null;
    v += strides[0] * c1;
    v += strides[1] * c2;
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int index(final int c1, final int c2, final int c3) {
    int v = 0;
    if (c1 != 0) {
      assert strides != null;
      v += strides[0] * c1;
    }
    if (c2 != 0) {
      assert strides != null;
      v += strides[1] * c2;
    }
    if (c3 != 0) {
      assert strides != null;
      v += strides[2] * c3;
    }
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int index(@Nonnull final Coordinate coords) {
    return coords.getIndex();
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int index(final int c1, final int c2, final int c3, final int c4, @Nullable final int... coords) {
    int v = 0;
    if (c1 != 0) {
      assert strides != null;
      v += strides[0] * c1;
    }
    if (c2 != 0) {
      assert strides != null;
      v += strides[1] * c2;
    }
    if (c3 != 0) {
      assert strides != null;
      v += strides[2] * c3;
    }
    if (c4 != 0) {
      assert strides != null;
      v += strides[3] * c4;
    }
    if (null != coords && 0 < coords.length) {
      assert strides != null;
      for (int i = 0; 4 + i < strides.length && i < coords.length; i++) {
        v += strides[4 + i] * coords[4 + i];
      }
    }
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  /**
   * Returns the value of l1 as a double.
   *
   * @docgenVersion 9
   */
  public double l1() {
    return RefArrays.stream(getData()).sum();
  }

  /**
   * Calculates the L2 norm of a vector.
   *
   * @docgenVersion 9
   */
  public double l2() {
    return Math.sqrt(RefArrays.stream(getData()).map(x -> x * x).sum());
  }

  /**
   * Returns the index of the element.
   *
   * @docgenVersion 9
   */
  public int index(@Nonnull final int[] coords) {
    int v = 0;
    assert strides != null;
    for (int i = 0; i < strides.length && i < coords.length; i++) {
      v += strides[i] * coords[i];
    }
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  /**
   * Maps the elements of this tensor according to the given function.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor map(@Nonnull @RefAware final DoubleUnaryOperator f) {
    return map(f, true);
  }

  /**
   * Maps the elements of this tensor according to the given function.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor map(@Nonnull @RefAware final DoubleUnaryOperator f, boolean parallel) {
    @Nullable final double[] data = getData();
    assert dimensions != null;
    Tensor tensor = new Tensor(dimensions);
    @Nonnull final double[] cpy = tensor.getData();
    RefIntStream stream = RefIntStream.range(0, data.length);
    if (parallel)
      stream = stream.parallel();
    stream.forEach(RefUtil.wrapInterface(i -> cpy[i] = f.applyAsDouble(data[i]), f));
    return tensor;
  }

  /**
   * Maps the coordinates of this tensor according to a given function.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor mapCoords(@Nonnull @RefAware final ToDoubleFunction<Coordinate> f) {
    return mapCoords(f, false);
  }

  /**
   * Maps the coordinates of this tensor according to a given function.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor mapCoords(@Nonnull @RefAware final ToDoubleFunction<Coordinate> f, boolean parallel) {
    assert dimensions != null;
    Tensor tensor = new Tensor(Tensor.getDoubles(coordStream(parallel).mapToDouble(f::applyAsDouble), length()),
        dimensions);
    RefUtil.freeRef(f);
    return tensor;
  }

  /**
   * Maps the index of the tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor mapIndex(@Nonnull @RefAware final TupleOperator f) {
    assert dimensions != null;
    Tensor tensor = new Tensor(Tensor.getDoubles(RefIntStream.range(0, length()).mapToDouble(i -> f.eval(get(i), i)), length()),
        dimensions);
    RefUtil.freeRef(f);
    return tensor;
  }

  /**
   * Calculates the mean of a dataset.
   *
   * @return the mean of the dataset
   * @docgenVersion 9
   */
  public double mean() {
    return sum() / length();
  }

  /**
   * Maps the elements of this tensor in parallel.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor mapParallel(@Nonnull @RefAware final DoubleUnaryOperator f) {
    @Nullable final double[] data = getData();
    assert dimensions != null;
    Tensor tensor = new Tensor(
        Tensor.getDoubles(RefIntStream.range(0, length()).mapToDouble(i -> f.applyAsDouble(data[i])), length()),
        dimensions);
    RefUtil.freeRef(f);
    return tensor;
  }

  /**
   * Returns the negation of this tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor minus(@Nonnull final Tensor right) {
    if (!RefArrays.equals(getDimensions(), right.getDimensions())) {
      IllegalArgumentException temp_33_0006 = new IllegalArgumentException(
          RefArrays.toString(getDimensions()) + " != " + RefArrays.toString(right.getDimensions()));
      right.freeRef();
      throw temp_33_0006;
    }
    @Nonnull final Tensor copy = new Tensor(getDimensions());
    @Nullable final double[] thisData = getData();
    @Nullable final double[] rightData = right.getData();
    right.freeRef();
    RefArrays.parallelSetAll(copy.getData(), i -> thisData[i] == rightData[i] ? 0 : thisData[i] - rightData[i]);
    return copy;
  }

  /**
   * Pretty-prints this object.
   *
   * @docgenVersion 9
   */
  public String prettyPrint() {
    assertAlive();
    return toString(true);
  }

  /**
   * Tensor multiply
   *
   * @return the result of the multiplication
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor multiply(final double d) {
    @Nonnull final Tensor tensor = new Tensor(getDimensions());
    @Nullable final double[] resultData = tensor.getData();
    @Nullable final double[] thisData = getData();
    for (int i = 0; i < thisData.length; i++) {
      resultData[i] = d * thisData[i];
    }
    return tensor;
  }

  /**
   * Calculates the root mean square of a given value.
   *
   * @docgenVersion 9
   */
  public double rms() {
    return Math.sqrt(sumSq() / length());
  }

  /**
   * Reduces this tensor in parallel.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor reduceParallel(@Nonnull final Tensor right, @Nonnull final DoubleBinaryOperator f) {
    if (!RefArrays.equals(right.getDimensions(), getDimensions())) {
      IllegalArgumentException temp_33_0007 = new IllegalArgumentException(
          RefArrays.toString(right.getDimensions()) + " != " + RefArrays.toString(getDimensions()));
      right.freeRef();
      throw temp_33_0007;
    }
    @Nullable final double[] dataL = getData();
    @Nullable final double[] dataR = right.getData();
    right.freeRef();
    assert dimensions != null;
    return new Tensor(Tensor.getDoubles(
        RefIntStream.range(0, length()).mapToDouble(i -> f.applyAsDouble(dataL[i], dataR[i])), length()), dimensions);
  }

  /**
   * Returns a new tensor with each element rounded to the nearest integer.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor round(final int precision) {
    if (precision > 8)
      return this.addRef();
    if (precision < 1)
      throw new IllegalArgumentException();
    return round(precision, 10);
  }

  /**
   * Returns a new tensor with each element rounded to the nearest integer.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor round(final int precision, final int base) {
    return map(v -> {
      final double units = Math.pow(base, Math.ceil(Math.log(v) / Math.log(base)) - precision);
      return Math.round(v / units) * units;
    });
  }

  /**
   * Scales the Tensor.
   *
   * @return the scaled Tensor
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor scale(final double d) {
    if (!Double.isFinite(d))
      throw new IllegalArgumentException();
    return map(v -> v * d);
  }

  /**
   * Scales the object in place.
   *
   * @docgenVersion 9
   */
  public void scaleInPlace(double d) {
    if (!Double.isFinite(d))
      throw new IllegalArgumentException();
    @Nullable final double[] data = getData();
    for (int i = 0; i < data.length; i++) {
      data[i] *= d;
    }
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(@Nonnull Coordinate coords, double value) {
    if (Double.isFinite(value)) {
      set(coords.getIndex(), value);
    }
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(double[] data) {
    for (int i = 0; i < getData().length; i++) {
      getData()[i] = data[i];
    }
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(@Nonnull DoubleSupplier f) {
    RefArrays.setAll(getData(), i -> f.getAsDouble());
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(final int coord1, final int coord2, final double value) {
    assert Double.isFinite(value);
    set(index(coord1, coord2), value);
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(final int coord1, final int coord2, final int coord3, final double value) {
    assert Double.isFinite(value);
    set(index(coord1, coord2, coord3), value);
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(final int coord1, final int coord2, final int coord3, final int coord4, final double value) {
    assert Double.isFinite(value);
    set(index(coord1, coord2, coord3, coord4), value);
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(int index, double value) {
    // assert Double.isFinite(value);
    assert index >= 0 : index;
    assert index < length() : RefString.format("%d>%d (%s)", index, length(), RefArrays.toString(dimensions));
    getData()[index] = value;
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(@Nonnull final int[] coords, final double value) {
    set(index(coords), value);
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(@Nonnull IntToDoubleFunction f) {
    try {
      RefArrays.parallelSetAll(getData(), f);
    } finally {
      RefUtil.freeRef(f);
    }
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public void set(@Nonnull final Tensor right) {
    assertAlive();
    @Nullable final double[] src = right.getData();
    double[] dst = getData();
    if (dst.length != src.length) {
      right.freeRef();
      throw new IllegalArgumentException(dst.length + " != " + src.length);
    }
    RefSystem.arraycopy(src, 0, dst, 0, src.length);
    right.freeRef();
  }

  /**
   * Sets the value of this cell using its coordinates.
   *
   * @docgenVersion 9
   */
  public void setByCoord(@RefAware @Nonnull ToDoubleFunction<Coordinate> f, boolean parallel) {
    coordStream(parallel).forEach(c -> {
      set(c, f.applyAsDouble(c));
    });
    RefUtil.freeRef(f);
  }

  /**
   * Returns the sum of the elements in the array.
   *
   * @docgenVersion 9
   */
  public double sum() {
    double v = 0;
    for (final double element : getData()) {
      v += element;
    }
    // assert Double.isFinite(v);
    return v;
  }

  /**
   * Calculates the sum of the squares of all the numbers in the list.
   *
   * @docgenVersion 9
   */
  public double sumSq() {
    double v = 0;
    for (final double element : getData()) {
      if (Double.isFinite(element))
        v += element * element;
    }
    if (v < 0)
      throw new RuntimeException("RMS is negative");
    if (Double.isNaN(v))
      throw new RuntimeException("RMS is NaN");
    // assert Double.isFinite(v);
    return v;
  }

  /**
   * Converts a BufferedImage to a grayscale image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public BufferedImage toGrayImage() {
    return toGrayImage(0);
  }

  /**
   * Converts a BufferedImage to a grayscale image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public BufferedImage toGrayImage(final int band) {
    final int width = getDimensions()[0];
    final int height = getDimensions()[1];
    @Nonnull final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        final double v = get(x, y, band);
        image.getRaster().setSample(x, y, 0, v < 0 ? 0 : v > 255 ? 255 : v);
      }
    }
    return image;
  }

  /**
   * Converts this BufferedImage into a new Image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public BufferedImage toImage() {
    @Nonnull final int[] dims = getDimensions();
    if (3 == dims.length) {
      if (3 == dims[2]) {
        return toRgbImage();
      } else {
        assert 1 == dims[2] : dims[2];
        return toGrayImage();
      }
    } else {
      assert 2 == dims.length;
      return toGrayImage();
    }
  }

  /**
   * Converts this RefList into a list of BufferedImages.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public RefList<BufferedImage> toImages() {
    @Nonnull final int[] dims = getDimensions();
    if (3 == dims.length) {
      if (3 == dims[2]) {
        return RefArrays.asList(toRgbImage());
      } else if (0 == dims[2] % 3) {
        @Nonnull final RefArrayList<BufferedImage> list = new RefArrayList<>();
        for (int i = 0; i < dims[2]; i += 3) {
          list.add(toRgbImage(i, i + 1, i + 2));
        }
        return list;
      } else if (1 == dims[2]) {
        return RefArrays.asList(toGrayImage());
      } else {
        @Nonnull final RefArrayList<BufferedImage> list = new RefArrayList<>();
        for (int i = 0; i < dims[2]; i++) {
          list.add(toGrayImage(i));
        }
        return list;
      }
    } else {
      assert 2 == dims.length : "order: " + dims.length;
      return RefArrays.asList(toGrayImage());
    }
  }

  /**
   * Returns the JSON element.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public JsonElement getJson(@Nullable Map<CharSequence, byte[]> resources, @Nonnull DataSerializer dataSerializer) {
    if (length() > 1024) {
      @Nonnull
      JsonObject obj = new JsonObject();
      @Nonnull
      int[] dimensions = getDimensions();
      obj.add("dims", toJsonArray(dimensions));
      if (null != id)
        obj.addProperty("id", id.toString());
      @Nonnull
      byte[] bytes = getBytes(dataSerializer);
      obj.addProperty("precision", ((SerialPrecision) dataSerializer).name());
      if (null != resources) {
        @Nonnull
        String id = UUID.randomUUID().toString();
        obj.addProperty("resource", id);
        resources.put(id, bytes);
      } else {
        obj.addProperty("base64", Base64.getEncoder().encodeToString(bytes));
      }
      return obj;
    } else {
      return getJson(new int[]{});
    }
  }

  /**
   * Returns the underlying JSON element.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public JsonElement getJsonRaw() {
    if (length() > 1024) {
      @Nonnull
      JsonObject obj = new JsonObject();
      @Nonnull
      int[] dimensions = getDimensions();
      obj.add("dims", toJsonArray(dimensions));
      if (null != id)
        obj.addProperty("id", id.toString());
      obj.add("values", toJson(getData()));
      return obj;
    } else {
      return getJson(new int[]{});
    }
  }

  /**
   * Converts this JsonArray to a JSON string.
   *
   * @docgenVersion 9
   */
  @NotNull
  public static JsonArray toJson(@Nonnull double[] data) {
    JsonArray jsonArray = new JsonArray();
    for (double v : data) {
      jsonArray.add(v);
    }
    return jsonArray;
  }

  /**
   * Returns an array of bytes representing the data in this ByteString.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public byte[] getBytes(@Nonnull DataSerializer precision) {
    return precision.toBytes(getData());
  }

  /**
   * Sets the bytes for this object.
   *
   * @docgenVersion 9
   */
  public void setBytes(byte[] bytes, @Nonnull DataSerializer precision) {
    precision.copy(bytes, getData());
  }

  /**
   * Converts a BufferedImage to an RGB image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public BufferedImage toRgbImage() {
    return toRgbImage(0, 1, 2);
  }

  /**
   * Converts a BufferedImage to an RGB image.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public BufferedImage toRgbImage(final int redBand, final int greenBand, final int blueBand) {
    assertAlive();
    @Nonnull final int[] dims = getDimensions();
    @Nonnull final BufferedImage img = new BufferedImage(dims[0], dims[1], BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < img.getWidth(); x++) {
      for (int y = 0; y < img.getHeight(); y++) {
        if (getDimensions()[2] == 1) {
          final double value = this.get(x, y, 0);
          img.setRGB(x, y, Tensor.bound8bit((int) value) * 0x010101);
        } else {
          final double red = Tensor.bound8bit(this.get(x, y, redBand));
          final double green = Tensor.bound8bit(this.get(x, y, greenBand));
          final double blue = Tensor.bound8bit(this.get(x, y, blueBand));
          img.setRGB(x, y, (int) (red + ((int) green << 8) + ((int) blue << 16)));
        }
      }
    }
    return img;
  }

  /**
   * Returns a string representation of this object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    assertAlive();
    return (null == data ? "0" : Integer.toHexString(RefSystem.identityHashCode(data)))
        + "@" + toString(false);
  }

  /**
   * Inverts the dimensions of a tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor invertDimensions() {
    return invertDimensions(this.addRef());
  }

  /**
   * Permute the dimensions of a tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor permuteDimensions(@Nonnull int... key) {
    assertAlive();
    int[] inputDims = getDimensions();
    int[] absKey = RefArrays.stream(key).map(a -> a == Integer.MAX_VALUE ? 0 : Math.abs(a)).toArray();
    int[] outputDims = permute(absKey, inputDims, inputDims);
    return rearrange(in -> permute(key, in, inputDims), outputDims);
  }

  /**
   * Reshapes the tensor while casting its data.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @RefAware
  public Tensor reshapeCast(@Nonnull int... dims) {
    if (0 == dims.length)
      throw new IllegalArgumentException();
    if (length(dims) != length())
      throw new IllegalArgumentException(RefArrays.toString(dims) + " != " + length());
    return new Tensor(data, dims);
  }

  /**
   * Applies the given function [f] to each element in the collection.
   *
   * @docgenVersion 9
   */
  public void forEach(@Nonnull @RefAware CoordOperator fn, boolean parallel) {
    coordStream(parallel).forEach(RefUtil.wrapInterface(c -> {
      fn.eval(get(c), c);
    }, fn));
  }

  /**
   * Returns the dot product of two vectors.
   *
   * @docgenVersion 9
   */
  public double dot(@Nonnull final Tensor right) {
    double[] l = getData();
    double[] r = right.getData();
    right.freeRef();
    double v = 0;
    for (int i = 0; i < l.length; i++) {
      v += l[i] * r[i];
    }
    return v;
  }

  /**
   * Returns the unit tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor unit() {
    return scale(1.0 / Math.sqrt(sumSq()));
  }

  /**
   * Selects a band from a tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor selectBand(final int band) {
    assert band >= 0;
    int[] dimensions = getDimensions();
    assert 3 == dimensions.length;
    assert band < dimensions[2];
    Tensor temp_33_0009 = new Tensor(dimensions[0], dimensions[1], 1);
    temp_33_0009.setByCoord(c -> {
      int[] coords = c.getCoords();
      return get(coords[0], coords[1], band);
    });
    Tensor temp_33_0008 = temp_33_0009.addRef();
    temp_33_0009.freeRef();
    return temp_33_0008;
  }

  /**
   * This function randomizes the order of the elements in the array.
   *
   * @docgenVersion 9
   */
  public void randomize(double amplitude) {
    double[] data = getData();
    for (int i = 0; i < data.length; i++) {
      data[i] = (FastRandom.INSTANCE.random() - 0.5) * 2 * amplitude;
    }
  }

  /**
   * Returns the magnitude of this vector.
   *
   * @docgenVersion 9
   */
  public double mag() {
    return Math.sqrt(sumSq());
  }

  /**
   * Maps the pixels of a tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Tensor mapPixels(@Nonnull UnaryOperator<double[]> fn) {
    assert dimensions != null;
    final Tensor copy = new Tensor(dimensions);
    RefIntStream.range(0, dimensions[0]).parallel().forEach(RefUtil.wrapInterface(x -> {
      RefIntStream.range(0, dimensions[1]).forEach(RefUtil.wrapInterface(y -> {
        final double[] finalPixel = fn
            .apply(RefIntStream.range(0, dimensions[2]).mapToDouble(c1 -> get(x, y, c1)).toArray());
        RefIntStream.range(0, dimensions[2])
            .forEach(RefUtil.wrapInterface(c -> copy.set(x, y, c, finalPixel[c]), copy.addRef()));
      }, copy.addRef()));
    }, copy.addRef()));
    return copy;
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != data) {
      if (RecycleBin.DOUBLES.want(data.length)) {
        RecycleBin.DOUBLES.recycle(data, data.length);
      }
      data = null;
    }
  }

  /**
   * Adds a reference to the Tensor.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  Tensor addRef() {
    return (Tensor) super.addRef();
  }

  /**
   * Fills the object with a value.
   *
   * @docgenVersion 9
   */
  public void fill(double v) {
    Arrays.fill(getData(), v);
  }

  /**
   * Returns the JSON element.
   *
   * @docgenVersion 9
   */
  @Nonnull
  private JsonElement getJson(@Nonnull final int[] coords) {
    assert dimensions != null;
    if (coords.length == dimensions.length) {
      final double d = get(coords);
      return new JsonPrimitive(d);
    } else {
      @Nonnull final JsonArray jsonArray = new JsonArray();
      RefIntStream.range(0, dimensions[dimensions.length - (coords.length + 1)]).mapToObj(i -> {
        @Nonnull final int[] newCoord = new int[coords.length + 1];
        RefSystem.arraycopy(coords, 0, newCoord, 1, coords.length);
        newCoord[0] = i;
        return getJson(newCoord);
      }).forEach(jsonArray::add);
      return jsonArray;
    }
  }

  /**
   * Returns a string representation of this object.
   *
   * @docgenVersion 9
   */
  private String toString(final boolean prettyPrint, @Nonnull final int... coords) {
    assert dimensions != null;
    if (coords.length == dimensions.length) {
      return Double.toString(get(coords));
    } else {
      RefList<CharSequence> list = RefIntStream.range(0, dimensions[coords.length]).mapToObj(i -> {
        @Nonnull final int[] newCoord = RefArrays.copyOf(coords, coords.length + 1);
        newCoord[coords.length] = i;
        return toString(prettyPrint, newCoord);
      }).limit(15).collect(RefCollectors.toList());
      if (list.size() > 10) {
        RefList<CharSequence> subList = list.subList(0, 8);
        list.freeRef();
        list = subList;
        list.add("...");
      }
      if (prettyPrint) {
        if (coords.length < dimensions.length - 2) {
          final CharSequence str = list.stream().limit(10).map(s -> "\t" + s.toString().replaceAll("\n", "\n\t"))
              .reduce((a, b) -> a + ",\n" + b).orElse("");
          list.freeRef();
          return "[\n" + str + "\n]";
        } else {
          final CharSequence str = list.stream().reduce((a, b) -> a + ", " + b).orElse("");
          list.freeRef();
          return "[ " + str + " ]";
        }
      } else {
        final CharSequence str = list.stream().reduce((a, b) -> a + "," + b).orElse("");
        list.freeRef();
        return "[ " + str + " ]";
      }
    }
  }

  /**
   * This is the CoordOperator interface.
   *
   * @docgenVersion 9
   */
  public interface CoordOperator {
    /**
     * Evaluates the code.
     *
     * @docgenVersion 9
     */
    void eval(double value, Coordinate index);
  }

  /**
   * This is the TupleOperator interface.
   *
   * @docgenVersion 9
   */
  public interface TupleOperator {
    /**
     * Evaluates the expression and returns the result as a double.
     *
     * @docgenVersion 9
     */
    double eval(double value, int index);
  }
}
