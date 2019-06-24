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
import com.simiacryptus.lang.ref.RecycleBin;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.util.FastRandom;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@SuppressWarnings("serial")
public final class Tensor extends ReferenceCountingBase implements Serializable, ZipSerializable {
  @Nonnull
  public static DataSerializer json_precision = SerialPrecision.Float;
  @Nullable
  protected final int[] dimensions;
  @Nullable
  protected final int[] strides;
  @Nullable
  protected volatile double[] data;
  @Nullable
  protected volatile UUID id;

  private Tensor() {
    super();
    data = null;
    strides = null;
    dimensions = null;
  }

  public Tensor(@Nonnull final double... ds) {
    this(ds, ds.length);
  }

  public Tensor(@Nullable final double[] data, @Nonnull final int... dims) {
    if (Tensor.length(dims) > Integer.MAX_VALUE) throw new IllegalArgumentException();
    if (null != data && Tensor.length(dims) != data.length)
      throw new IllegalArgumentException(Arrays.toString(dims) + " != " + data.length);
    dimensions = (null == dims || 0 == dims.length) ? new int[]{} : Arrays.copyOf(dims, dims.length);
    strides = Tensor.getSkips(dims);
    //this.data = data;// Arrays.copyOf(data, data.length);
    if (null != data) {
      this.data = RecycleBin.DOUBLES.copyOf(data, data.length);
    }
    assert isValid();
    //assert (null == data || Tensor.length(dims) == data.length);
  }

  private Tensor(int[] dims, @Nullable double[] data) {
    this(dims, Tensor.getSkips(dims), data);
  }

  private Tensor(int[] dimensions, int[] strides, @Nullable double[] data) {
    if (Tensor.length(dimensions) >= Integer.MAX_VALUE) throw new IllegalArgumentException();
    assert null == data || data.length == Tensor.length(dimensions);
    this.dimensions = dimensions;
    this.strides = strides;
    this.data = data;
    assert isValid();
  }

  public Tensor(@Nullable final float[] data, @Nonnull final int... dims) {
    if (Tensor.length(dims) >= Integer.MAX_VALUE) throw new IllegalArgumentException();
    dimensions = Arrays.copyOf(dims, dims.length);
    strides = Tensor.getSkips(dims);
    if (null != data) {
      this.data = RecycleBin.DOUBLES.obtain(data.length);// Arrays.copyOf(data, data.length);
      Arrays.parallelSetAll(this.data, i -> {
        final double v = data[i];
        return Double.isFinite(v) ? v : 0;
      });
      assert Arrays.stream(this.data).allMatch(v -> Double.isFinite(v));
    }
    assert isValid();
    //assert (null == data || Tensor.length(dims) == data.length);
  }

  public Tensor(@Nonnull final int... dims) {
    this((double[]) null, dims);
    assert dims.length > 0;
  }

  @Nullable
  public static Tensor fromJson(@Nullable final JsonElement json, @Nullable Map<CharSequence, byte[]> resources) {
    if (null == json) return null;
    if (json.isJsonArray()) {
      final JsonArray array = json.getAsJsonArray();
      final int size = array.size();
      if (array.get(0).isJsonPrimitive()) {
        final double[] doubles = IntStream.range(0, size).mapToObj(i -> {
          return array.get(i);
        }).mapToDouble(element -> {
          return element.getAsDouble();
        }).toArray();
        @Nonnull Tensor tensor = new Tensor(doubles);
        assert tensor.isValid();
        return tensor;
      } else {
        final List<Tensor> elements = IntStream.range(0, size).mapToObj(i -> {
          return array.get(i);
        }).map(element -> {
          return Tensor.fromJson(element, resources);
        }).collect(Collectors.toList());
        @Nonnull final int[] dimensions = elements.get(0).getDimensions();
        if (!elements.stream().allMatch(t -> Arrays.equals(dimensions, t.getDimensions()))) {
          throw new IllegalArgumentException();
        }
        @Nonnull final int[] newDdimensions = Arrays.copyOf(dimensions, dimensions.length + 1);
        newDdimensions[dimensions.length] = size;
        @Nonnull final Tensor tensor = new Tensor(newDdimensions);
        @Nullable final double[] data = tensor.getData();
        for (int i = 0; i < size; i++) {
          @Nullable final double[] e = elements.get(i).getData();
          System.arraycopy(e, 0, data, i * e.length, e.length);
        }
        for (@Nonnull Tensor t : elements) {
          t.freeRef();
        }
        assert tensor.isValid();
        return tensor;
      }
    } else if (json.isJsonObject()) {
      JsonObject jsonObject = json.getAsJsonObject();
      @Nonnull int[] dims = fromJsonArray(jsonObject.getAsJsonArray("length"));
      @Nonnull Tensor tensor = new Tensor(dims);
      SerialPrecision precision = SerialPrecision.valueOf(jsonObject.getAsJsonPrimitive("precision").getAsString());
      JsonElement base64 = jsonObject.get("base64");
      if (null == base64) {
        if (null == resources) throw new IllegalArgumentException("No Data Resources");
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
      @Nonnull Tensor tensor = new Tensor(json.getAsJsonPrimitive().getAsDouble());
      assert tensor.isValid();
      return tensor;
    }
  }

  private static double bound8bit(final double value) {
    final int max = 0xFF;
    final int min = 0;
    return value < min ? min : value > max ? max : value;
  }

  private static int bound8bit(final int value) {
    final int max = 0xFF;
    final int min = 0;
    return value < min ? min : value > max ? max : value;
  }

  public static int length(@Nonnull int... dims) {
    long total = 1;
    for (final int dim : dims) {
      assert 0 <= dim : Arrays.toString(dims);
      total *= dim;
      assert 0 <= total : Arrays.toString(dims);
      assert total < Integer.MAX_VALUE : Arrays.toString(dims);
    }
    return (int) total;
  }

  @Nonnull
  public static Tensor fromRGB(@Nonnull final BufferedImage img) {
    final int width = img.getWidth();
    final int height = img.getHeight();
    @Nonnull final Tensor a = new Tensor(width, height, 3);
    IntStream.range(0, width).parallel().forEach(x -> {
      @Nonnull final int[] coords = {0, 0, 0};
      IntStream.range(0, height).forEach(y -> {
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

  public static double[] getDoubles(@Nonnull final DoubleStream stream, final int dim) {
    final double[] doubles = RecycleBin.DOUBLES.obtain(dim);
    stream.forEach(new DoubleConsumer() {
      int j = 0;

      @Override
      public void accept(final double value) {
        doubles[j++] = value;
      }
    });
    return doubles;
  }

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

  @Nonnull
  public static Tensor product(@Nonnull final Tensor left, @Nonnull final Tensor right) {
    if (left.length() == 1 && right.length() != 1) return Tensor.product(right, left);
    assert left.length() == right.length() || 1 == right.length();
    @Nonnull final Tensor result = new Tensor(left.getDimensions());
    @Nullable final double[] resultData = result.getData();
    @Nullable final double[] leftData = left.getData();
    @Nullable final double[] rightData = right.getData();
    for (int i = 0; i < resultData.length; i++) {
      final double l = leftData[i];
      final double r = rightData[1 == rightData.length ? 0 : i];
      resultData[i] = l * r;
    }
    return result;
  }

  public static double[] toDoubles(@Nonnull final float[] data) {
    final double[] buffer = RecycleBin.DOUBLES.obtain(data.length);
    for (int i = 0; i < data.length; i++) {
      buffer[i] = data[i];
    }
    return buffer;
  }

  @Nonnull
  public static float[] toFloats(@Nonnull final double[] data) {
    return copy(data, new float[data.length]);
  }

  public static float[] copy(double[] src, float[] buffer) {
    for (int i = 0; i < src.length; i++) {
      buffer[i] = (float) src[i];
    }
    return buffer;
  }

  @Nonnull
  public static JsonArray toJsonArray(@Nonnull int[] ints) {
    @Nonnull JsonArray dim = new JsonArray();
    for (int i = 0; i < ints.length; i++) {
      dim.add(new JsonPrimitive(ints[i]));
    }
    return dim;
  }

  @Nonnull
  public static int[] fromJsonArray(@Nonnull JsonArray ints) {
    @Nonnull int[] array = new int[ints.size()];
    for (int i = 0; i < ints.size(); i++) {
      array[i] = ints.get(i).getAsInt();
    }
    return array;
  }

  @Nonnull
  public static Tensor invertDimensions(@Nonnull Tensor tensor) {
    return tensor.rearrange(Tensor::reverse);
  }

  @Nonnull
  public static int[] permute(@Nonnull int[] key, int[] data, final int[] dimensions) {
    @Nonnull int[] copy = new int[key.length];
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

  @Nonnull
  public static int[] reverse(@Nonnull int[] dimensions) {
    return reverseInPlace(Arrays.copyOf(dimensions, dimensions.length));
  }

  @Nonnull
  public static int[] reverse(@Nonnull long[] dimensions) {
    return reverseInPlace(Arrays.stream(dimensions).mapToInt(x -> (int) x).toArray());
  }

  public static int[] reverseInPlace(final int[] array) {
    if (array == null) {
      return array;
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

  public static CharSequence prettyPrint(double[] doubles) {
    @Nonnull Tensor t = new Tensor(doubles);
    String prettyPrint = t.prettyPrint();
    t.freeRef();
    return prettyPrint;
  }

  public static double[] getPixel(final Tensor tensor, final int x, final int y, final int bands) {
    return IntStream.range(0, bands).mapToDouble(band -> tensor.get(x, y, band)).toArray();
  }

  @NotNull
  public static SerializableFunction<Tensor, Tensor> select(Coordinate... reducedCoords) {
    return tensor -> {
      Tensor reduced = new Tensor(reducedCoords.length);
      reduced.setByCoord(c2 -> tensor.get(reducedCoords[c2.getIndex()]), false);
      return reduced;
    };
  }

  @Override
  public Tensor addRef() {
    return (Tensor) super.addRef();
  }

  @Nonnull
  public Tensor sumChannels() {
    int[] dimensions = getDimensions();
    Tensor self = this;
    return new Tensor(dimensions[0], dimensions[1], 1).setByCoord(c -> {
      int[] coords = c.getCoords();
      return IntStream.range(0, dimensions[2]).mapToDouble(j -> self.get(coords[0], coords[1], j)).sum();
    });
  }

  @Nonnull
  public Stream<double[]> getPixelStream() {
    int[] dimensions = getDimensions();
    int width = dimensions[0];
    int height = dimensions[1];
    int bands = dimensions[2];
    return IntStream.range(0, width).mapToObj(x -> x).parallel().flatMap(x -> {
      return IntStream.range(0, height).mapToObj(y -> y).map(y -> {
        return getPixel(this, x, y, bands);
      });
    });
  }

  public Tensor rescaleRms(final double rms) {
    return scale(rms / rms());
  }

  public Tensor normalizeDistribution() {
    double[] sortedValues = Arrays.stream(getData()).sorted().toArray();
    Tensor result = map(v -> Math.abs(((double) Arrays.binarySearch(sortedValues, v)) / ((double) sortedValues.length)));
    return result;
  }

  @Nonnull
  public Tensor rearrange(@Nonnull UnaryOperator<int[]> fn) {
    return rearrange(fn, fn.apply(getDimensions()));
  }

  @Nonnull
  public Tensor rearrange(@Nonnull UnaryOperator<int[]> fn, int[] outputDims) {
    @Nonnull Tensor result = new Tensor(outputDims);
    coordStream(false).forEach(c -> {
      int[] inCoords = c.getCoords();
      int[] outCoords = fn.apply(inCoords);
      result.set(outCoords, get(c));
    });
    return result;
  }

  public boolean isValid() {
    return !isFinalized() && (null == this.data || this.data.length == Tensor.length(dimensions));
  }

  public void addInPlace(@Nonnull final Tensor tensor) {
    assert Arrays.equals(getDimensions(), tensor.getDimensions()) : Arrays.toString(getDimensions()) + " != " + Arrays.toString(tensor.getDimensions());
    double[] toAdd = tensor.getData();
    double[] data = getData();
    int length = length();
    int shards = Math.max(1, Math.min(8, length / 64));
    double shardSize = (double) length / shards;
    DoubleStream.iterate(0, x -> x + shardSize).limit(shards).parallel().forEach(start -> {
      int end = (int) Math.min(length, Math.floor(start + shardSize));
      for (int i = (int) Math.floor(start); i < end; i++) {
        data[i] += toAdd[i];
      }
    });
  }

  public void add(@Nonnull final Coordinate coords, final double value) {
    add(coords.getIndex(), value);
  }

  @Nonnull
  public final Tensor add(final int index, final double value) {
    getData()[index] += value;
    return this;
  }

  public void add(@Nonnull final int[] coords, final double value) {
    add(index(coords), value);
  }

  @Nullable
  public Tensor add(@Nonnull final Tensor right) {
    assert Arrays.equals(getDimensions(), right.getDimensions());
    return mapCoords((c) -> get(c) + right.get(c));
  }

  @Nullable
  public Tensor addAndFree(@Nonnull final Tensor right) {
    assertAlive();
    right.assertAlive();
    if (1 == currentRefCount()) {
      addInPlace(right);
      return this;
    } else {
      assert Arrays.equals(getDimensions(), right.getDimensions());
      return mapCoordsAndFree((c) -> get(c) + right.get(c));
    }
  }

  @Nonnull
  public Stream<Coordinate> coordStream(boolean parallel) {
    //ConcurrentHashSet<Object> distinctBuffer = new ConcurrentHashSet<>();
    //assert distinctBuffer.add(coordinate.copy()) : String.format("Duplicate: %s in %s", coordinate, distinctBuffer);
    return StreamSupport.stream(Spliterators.spliterator(new Iterator<Coordinate>() {

      int cnt = 0;
      @Nonnull
      Coordinate coordinate = new Coordinate();
      @Nonnull
      int[] val = new int[dimensions.length];
      @Nonnull
      int[] safeCopy = new int[dimensions.length];

      @Override
      public boolean hasNext() {
        return cnt < length();
      }

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
        System.arraycopy(val, 0, safeCopy, 0, val.length);
        coordinate.setIndex(cnt++);
        coordinate.setCoords(safeCopy);
        return parallel ? coordinate.copy() : coordinate;
      }
    }, length(), Spliterator.ORDERED), parallel);
  }

  public int length() {
    assertAlive();
    if (null != data) {
      return data.length;
    } else {
      return Tensor.length(dimensions);
    }
  }

  @Nonnull
  public Tensor copy() {
    assertAlive();
    return new Tensor(RecycleBin.DOUBLES.copyOf(getData(), getData().length), Arrays.copyOf(dimensions, dimensions.length));
  }

  @Override
  protected void _free() {
    if (null != data) {
      if (RecycleBin.DOUBLES.want(data.length)) {
        RecycleBin.DOUBLES.recycle(data, data.length);
      }
      data = null;
    }
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
    @Nullable final Tensor other = (Tensor) obj;
    if (0 == currentRefCount()) return false;
    if (0 == other.currentRefCount()) return false;
    if (!Arrays.equals(dimensions, other.dimensions)) {
      return false;
    }
    return Arrays.equals(getData(), other.getData());
  }

  public double get(@Nonnull final Coordinate coords) {
    final double v = getData()[coords.getIndex()];
    return v;
  }

  public double get(final int index) {
    return getData()[index];
  }

  public double get(final int c1, final int c2) {
    return getData()[index(c1, c2)];
  }

  public double get(final int c1, final int c2, final int c3) {
    return getData()[index(c1, c2, c3)];
  }

  public double get(final int c1, final int c2, final int c3, final int c4, final int... coords) {
    return getData()[index(c1, c2, c3, c4, coords)];
  }

  public void get(@Nonnull final double[] bufferArray) {
    System.arraycopy(getData(), 0, bufferArray, 0, length());
  }

  public double get(@Nonnull final int[] coords) {
    return getData()[index(coords)];
  }

  @Nonnull
  public double[] getData() {
    assertAlive();
    if (null == data) {
      synchronized (this) {
        if (null == data) {
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

  @Nonnull
  public final int[] getDimensions() {
    return Arrays.copyOf(dimensions, dimensions.length);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(getData());
    result = prime * result + Arrays.hashCode(dimensions);
    return result;
  }

  @Nonnull
  public float[] getDataAsFloats() {
    return Tensor.toFloats(getData());
  }

  public int index(final int c1) {
    int v = 0;
    v += strides[0] * c1;
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  public int index(final int c1, final int c2) {
    int v = 0;
    v += strides[0] * c1;
    v += strides[1] * c2;
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  public int index(final int c1, final int c2, final int c3) {
    int v = 0;
    v += strides[0] * c1;
    v += strides[1] * c2;
    v += strides[2] * c3;
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  public int index(@Nonnull final Coordinate coords) {
    return coords.getIndex();
  }

  public int index(final int c1, final int c2, final int c3, final int c4, @Nullable final int... coords) {
    int v = 0;
    v += strides[0] * c1;
    v += strides[1] * c2;
    v += strides[2] * c3;
    v += strides[3] * c4;
    if (null != coords && 0 < coords.length) {
      for (int i = 0; 4 + i < strides.length && i < coords.length; i++) {
        v += strides[4 + i] * coords[4 + i];
      }
    }
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  public double l1() {
    return Arrays.stream(getData()).sum();
  }

  public double l2() {
    return Math.sqrt(Arrays.stream(getData()).map(x -> x * x).sum());
  }

  public int index(@Nonnull final int[] coords) {
    int v = 0;
    for (int i = 0; i < strides.length && i < coords.length; i++) {
      v += strides[i] * coords[i];
    }
    return v;
    // return IntStream.range(0, strides.length).mapCoords(i->strides[i]*coords[i]).sum();
  }

  @Nullable
  public Tensor map(@Nonnull final DoubleUnaryOperator f) {
    return map(f, true);
  }

  @Nullable
  public Tensor map(@Nonnull final DoubleUnaryOperator f, boolean parallel) {
    @Nullable final double[] data = getData();
    Tensor tensor = new Tensor(dimensions);
    @Nonnull final double[] cpy = tensor.getData();
    IntStream stream = IntStream.range(0, data.length);
    if (parallel) stream = stream.parallel();
    stream.forEach(i -> cpy[i] = f.applyAsDouble(data[i]));
    return tensor;
  }

  @Nullable
  public Tensor mapAndFree(@Nonnull final DoubleUnaryOperator f) {
    @Nullable final double[] data = getData();
    @Nonnull final double[] cpy = new double[data.length];
    for (int i = 0; i < data.length; i++) {
      final double x = data[i];
      // assert Double.isFinite(x);
      final double v = f.applyAsDouble(x);
      // assert Double.isFinite(v);
      cpy[i] = v;
    }
    Tensor tensor = new Tensor(cpy, dimensions);
    this.freeRef();
    return tensor;
  }

  @Nullable
  public Tensor mapCoords(@Nonnull final ToDoubleFunction<Coordinate> f) {
    return mapCoords(f, false);
  }

  @Nullable
  public Tensor mapCoordsAndFree(@Nonnull final ToDoubleFunction<Coordinate> f) {
    return mapCoordsAndFree(f, false);
  }

  @Nullable
  public Tensor mapCoords(@Nonnull final ToDoubleFunction<Coordinate> f, boolean parallel) {
    return new Tensor(Tensor.getDoubles(coordStream(parallel).mapToDouble(i -> f.applyAsDouble(i)), length()), dimensions);
  }

  @Nullable
  public Tensor mapCoordsAndFree(@Nonnull final ToDoubleFunction<Coordinate> f, boolean parallel) {
    Tensor tensor = new Tensor(Tensor.getDoubles(coordStream(parallel).mapToDouble(i -> f.applyAsDouble(i)), length()), dimensions);
    freeRef();
    return tensor;
  }

  @Nullable
  public Tensor mapIndex(@Nonnull final TupleOperator f) {
    return new Tensor(Tensor.getDoubles(IntStream.range(0, length()).mapToDouble(i -> f.eval(get(i), i)), length()), dimensions);
  }

  public double mean() {
    return sum() / length();
  }

  @Nullable
  public Tensor mapParallel(@Nonnull final DoubleUnaryOperator f) {
    @Nullable final double[] data = getData();
    return new Tensor(Tensor.getDoubles(IntStream.range(0, length()).mapToDouble(i -> f.applyAsDouble(data[i])), length()), dimensions);
  }

  @Nonnull
  public Tensor minus(@Nonnull final Tensor right) {
    if (!Arrays.equals(getDimensions(), right.getDimensions())) {
      throw new IllegalArgumentException(Arrays.toString(getDimensions()) + " != " + Arrays.toString(right.getDimensions()));
    }
    @Nonnull final Tensor copy = new Tensor(getDimensions());
    @Nullable final double[] thisData = getData();
    @Nullable final double[] rightData = right.getData();
    Arrays.parallelSetAll(copy.getData(), i -> (thisData[i] == rightData[i]) ? 0 : (thisData[i] - rightData[i]));
    return copy;
  }

  public String prettyPrint() {
    return toString(true);
  }

  public String prettyPrintAndFree() {
    String prettyPrint = prettyPrint();
    freeRef();
    return prettyPrint;
  }

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

  public double rmsAndFree() {
    double rms = rms();
    freeRef();
    return rms;
  }

  public double rms() {
    return Math.sqrt(sumSq() / length());
  }

  @Nullable
  public Tensor reduceParallel(@Nonnull final Tensor right, @Nonnull final DoubleBinaryOperator f) {
    if (!Arrays.equals(right.getDimensions(), getDimensions())) {
      throw new IllegalArgumentException(Arrays.toString(right.getDimensions()) + " != " + Arrays.toString(getDimensions()));
    }
    @Nullable final double[] dataL = getData();
    @Nullable final double[] dataR = right.getData();
    return new Tensor(Tensor.getDoubles(IntStream.range(0, length()).mapToDouble(i -> f.applyAsDouble(dataL[i], dataR[i])), length()), dimensions);
  }

  @Nullable
  public Tensor round(final int precision) {
    if (precision > 8) return this;
    if (precision < 1) throw new IllegalArgumentException();
    return round(precision, 10);
  }

  @Nullable
  public Tensor round(final int precision, final int base) {
    return map(v -> {
      final double units = Math.pow(base, Math.ceil(Math.log(v) / Math.log(base)) - precision);
      return Math.round(v / units) * units;
    });
  }

  @Nullable
  public Tensor scale(final double d) {
    return map(v -> v * d);
  }

  @Nonnull
  public Tensor scaleInPlace(final double d) {
    @Nullable final double[] data = getData();
    for (int i = 0; i < data.length; i++) {
      data[i] *= d;
    }
    return this;
  }

  public Tensor set(@Nonnull final Coordinate coords, final double value) {
    if (Double.isFinite(value)) set(coords.getIndex(), value);
    return this;
  }

  @Nonnull
  public Tensor set(final double[] data) {
    for (int i = 0; i < getData().length; i++) {
      getData()[i] = data[i];
    }
    return this;
  }

  @Nonnull
  public Tensor set(@Nonnull final DoubleSupplier f) {
    Arrays.setAll(getData(), i -> f.getAsDouble());
    return this;
  }

  public void set(final int coord1, final int coord2, final double value) {
    assert Double.isFinite(value);
    set(index(coord1, coord2), value);
  }

  public void set(final int coord1, final int coord2, final int coord3, final double value) {
    assert Double.isFinite(value);
    set(index(coord1, coord2, coord3), value);
  }

  public void set(final int coord1, final int coord2, final int coord3, final int coord4, final double value) {
    assert Double.isFinite(value);

    set(index(coord1, coord2, coord3, coord4), value);
  }

  @Nonnull
  public Tensor set(final int index, final double value) {
    // assert Double.isFinite(value);
    assert index >= 0 : index;
    assert index < length() : String.format("%d>%d (%s)", index, length(), Arrays.toString(dimensions));
    getData()[index] = value;
    return this;
  }

  public void set(@Nonnull final int[] coords, final double value) {
    //assert Double.isFinite(value);
    set(index(coords), value);
  }

  @Nonnull
  public Tensor set(@Nonnull final IntToDoubleFunction f) {
    Arrays.parallelSetAll(getData(), f);
    return this;
  }

  public Tensor set(@Nonnull final Tensor right) {
    assertAlive();
    @Nullable final double[] src = right.getData();
    double[] dst = getData();
    if (dst.length != src.length) {
      throw new IllegalArgumentException(dst.length + " != " + src.length);
    }
    System.arraycopy(src, 0, dst, 0, src.length);
    return this;
  }

  public Tensor setAll(final double v) {
    @Nullable final double[] data = getData();
    for (int i = 0; i < data.length; i++) {
      data[i] = v;
    }
    return this;
  }

  @Nonnull
  public Tensor setByCoord(@Nonnull final ToDoubleFunction<Coordinate> f) {
    return setByCoord(f, true);
  }

  @Nonnull
  public Tensor setByCoord(@Nonnull final ToDoubleFunction<Coordinate> f, boolean parallel) {
    coordStream(parallel).forEach(c -> set(c, f.applyAsDouble(c)));
    return this;
  }

  public double sum() {
    double v = 0;
    for (final double element : getData()) {
      v += element;
    }
    // assert Double.isFinite(v);
    return v;
  }

  public double sumSq() {
    double v = 0;
    for (final double element : getData()) {
      v += element * element;
    }
    // assert Double.isFinite(v);
    return v;
  }

  public void setParallelByIndex(@Nonnull final IntToDoubleFunction f) {
    IntStream.range(0, length()).parallel().forEach(c -> set(c, f.applyAsDouble(c)));
  }

  @Nonnull
  public BufferedImage toGrayImage() {
    return toGrayImage(0);
  }

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

  @Nonnull
  public BufferedImage toImage() {
    @Nonnull final int[] dims = getDimensions();
    if (3 == dims.length) {
      if (3 == dims[2]) {
        return toRgbImage();
      } else {
        assert 1 == dims[2];
        return toGrayImage();
      }
    } else {
      assert 2 == dims.length;
      return toGrayImage();
    }
  }

  @Nonnull
  public List<BufferedImage> toImages() {
    @Nonnull final int[] dims = getDimensions();
    if (3 == dims.length) {
      if (3 == dims[2]) {
        return Arrays.asList(toRgbImage());
      } else if (0 == dims[2] % 3) {
        @Nonnull final ArrayList<BufferedImage> list = new ArrayList<>();
        for (int i = 0; i < dims[2]; i += 3) {
          list.add(toRgbImage(i, i + 1, i + 2));
        }
        return list;
      } else if (1 == dims[2]) {
        return Arrays.asList(toGrayImage());
      } else {
        @Nonnull final ArrayList<BufferedImage> list = new ArrayList<>();
        for (int i = 0; i < dims[2]; i++) {
          list.add(toGrayImage(i));
        }
        return list;
      }
    } else {
      assert 2 == dims.length : "order: " + dims.length;
      return Arrays.asList(toGrayImage());
    }
  }

  @Nonnull
  public JsonElement getJson(@Nullable Map<CharSequence, byte[]> resources, @Nonnull DataSerializer dataSerializer) {
    if (length() > 1024) {
      @Nonnull JsonObject obj = new JsonObject();
      @Nonnull int[] dimensions = getDimensions();
      obj.add("length", toJsonArray(dimensions));
      if (null != id) obj.addProperty("id", id.toString());
      @Nonnull byte[] bytes = getBytes(dataSerializer);
      obj.addProperty("precision", ((SerialPrecision) dataSerializer).name());
      if (null != resources) {
        @Nonnull String id = UUID.randomUUID().toString();
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

  @Nonnull
  public Tensor setBytes(byte[] bytes) {
    return setBytes(bytes, json_precision);
  }

  @Nonnull
  public byte[] getBytes(@Nonnull DataSerializer precision) {
    return precision.toBytes(getData());
  }

  @Nonnull
  public Tensor setBytes(byte[] bytes, @Nonnull DataSerializer precision) {
    precision.copy(bytes, getData());
    return this;
  }

  @Nonnull
  private JsonElement getJson(@Nonnull final int[] coords) {
    if (coords.length == dimensions.length) {
      final double d = get(coords);
      return new JsonPrimitive(d);
    } else {
      @Nonnull final JsonArray jsonArray = new JsonArray();
      IntStream.range(0, dimensions[dimensions.length - (coords.length + 1)]).mapToObj(i -> {
        @Nonnull final int[] newCoord = new int[coords.length + 1];
        System.arraycopy(coords, 0, newCoord, 1, coords.length);
        newCoord[0] = i;
        return getJson(newCoord);
      }).forEach(l -> jsonArray.add(l));
      return jsonArray;
    }
  }

  @Nonnull
  public BufferedImage toRgbImage() {
    return toRgbImage(0, 1, 2);
  }

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

  @Nonnull
  public BufferedImage toRgbImageAlphaMask(final int redBand, final int greenBand, final int blueBand, Tensor alphaMask) {
    assert alphaMask.getDimensions()[0] == getDimensions()[0];
    assert alphaMask.getDimensions()[1] == getDimensions()[1];
    @Nonnull final int[] dims = getDimensions();
    @Nonnull final BufferedImage img = new BufferedImage(dims[0], dims[1], BufferedImage.TYPE_INT_ARGB);
    for (int x = 0; x < img.getWidth(); x++) {
      for (int y = 0; y < img.getHeight(); y++) {
        final double red = Tensor.bound8bit(this.get(x, y, redBand));
        final double green = Tensor.bound8bit(this.get(x, y, greenBand));
        final double blue = Tensor.bound8bit(this.get(x, y, blueBand));
        final double alpha = Tensor.bound8bit(alphaMask.get(x, y, 0));
        img.setRGB(x, y, (int) (red + ((int) green << 8) + ((int) blue << 16) + ((int) alpha << 24)));
      }
    }
    return img;
  }

  @Nonnull
  @Override
  public String toString() {
    return (null == data ? "0" : Integer.toHexString(System.identityHashCode(data))) + "@" + toString(false);
  }

  private String toString(final boolean prettyPrint, @Nonnull final int... coords) {
    if (coords.length == dimensions.length) {
      return Double.toString(get(coords));
    } else {
      List<CharSequence> list = IntStream.range(0, dimensions[coords.length]).mapToObj(i -> {
        @Nonnull final int[] newCoord = Arrays.copyOf(coords, coords.length + 1);
        newCoord[coords.length] = i;
        return toString(prettyPrint, newCoord);
      }).limit(15).collect(Collectors.toList());
      if (list.size() > 10) {
        list = list.subList(0, 8);
        list.add("...");
      }
      if (prettyPrint) {
        if (coords.length < dimensions.length - 2) {
          final CharSequence str = list.stream().limit(10)
              .map(s -> "\t" + s.toString().replaceAll("\n", "\n\t"))
              .reduce((a, b) -> a + ",\n" + b).orElse("");
          return "[\n" + str + "\n]";
        } else {
          final CharSequence str = list.stream().reduce((a, b) -> a + ", " + b).orElse("");
          return "[ " + str + " ]";
        }
      } else {
        final CharSequence str = list.stream().reduce((a, b) -> a + "," + b).orElse("");
        return "[ " + str + " ]";
      }
    }
  }

  @Nonnull
  public Tensor invertDimensions() {
    return invertDimensions(this);
  }

  @Nonnull
  public Tensor invertDimensionsAndFree() {
    Tensor tensor = invertDimensions(this);
    freeRef();
    return tensor;
  }

  @Nonnull
  public Tensor permuteDimensions(int... key) {
    assertAlive();
    int[] inputDims = getDimensions();
    int[] absKey = Arrays.stream(key).map(a -> a == Integer.MAX_VALUE ? 0 : Math.abs(a)).toArray();
    int[] outputDims = permute(absKey, inputDims, inputDims);
    return rearrange(in -> permute(key, in, inputDims), outputDims);
  }

  public Tensor permuteDimensionsAndFree(int... key) {
    Tensor result = permuteDimensions(key);
    this.freeRef();
    return result;
  }

  @Nullable
  public Tensor reshapeCast(@Nonnull int... dims) {
    if (0 == dims.length) throw new IllegalArgumentException();
    if (length(dims) != length()) throw new IllegalArgumentException(Arrays.toString(dims) + " != " + length());
    double[] data = getData();
    return new Tensor(dims, null == data ? null : RecycleBin.DOUBLES.copyOf(data, data.length));
  }

  @Nullable
  public Tensor reshapeCastAndFree(@Nonnull int... dims) {
    Tensor tensor = reshapeCast(dims);
    freeRef();
    return tensor;
  }

  public void forEach(@Nonnull CoordOperator fn, boolean parallel) {
    coordStream(parallel).forEach(c -> {
      fn.eval(get(c), c);
    });
  }

  public double dot(final Tensor right) {
    double[] l = getData();
    double[] r = right.getData();
    double v = 0;
    for (int i = 0; i < l.length; i++) {
      v += l[i] * r[i];
    }
    return v;
  }

  public Tensor unit() {
    return scale(1.0 / Math.sqrt(sumSq()));
  }

  public Tensor selectBand(final int band) {
    assert band >= 0;
    int[] dimensions = getDimensions();
    assert 3 == dimensions.length;
    assert band < dimensions[2];
    return new Tensor(dimensions[0], dimensions[1], 1).setByCoord(c -> {
      int[] coords = c.getCoords();
      return get(coords[0], coords[1], band);
    });
  }

  public Tensor copyAndFree() {
    if (currentRefCount() == 1) return this;
    Tensor copy = copy();
    freeRef();
    return copy;
  }

//  /**
//   * Resize as img tensor.
//   *
//   * @param width  the width
//   * @param height the height
//   * @return the tensor
//   */
//  public Tensor resizeAsImg(final int width, final int height) {
//    if (getDimensions()[0] == width && getDimensions()[1] == height) {
//      addRef();
//      return this;
//    }
//    return Tensor.fromRGB(TestUtil.resize(toImage(), width, height));
//  }

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

  public Tensor setId(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  public Tensor randomize(double amplitude) {
    double[] data = getData();
    for (int i = 0; i < data.length; i++) {
      data[i] = (FastRandom.INSTANCE.random() - 0.5) * 2 * amplitude;
    }
    return this;
  }

  public double mag() {
    return Math.sqrt(sumSq());
  }

  public double magAndFree() {
    double mag = mag();
    freeRef();
    return mag;
  }

  public interface CoordOperator {
    void eval(double value, Coordinate index);
  }


  public interface TupleOperator {
    double eval(double value, int index);
  }
}
