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
import java.nio.DoubleBuffer;
import java.nio.*;
import java.util.Base64;
import java.util.DoubleSummaryStatistics;

/**
 * The enum Serial precision.
 */
public enum SerialPrecision implements DataSerializer {
  /**
   * The Double.
   */
  Double(8) {
    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull double[] from, @Nonnull byte[] to) {
      @Nonnull
      DoubleBuffer inBuffer = DoubleBuffer.wrap(from);
      @Nonnull
      DoubleBuffer outBuffer = ByteBuffer.wrap(to).asDoubleBuffer();
      while (inBuffer.hasRemaining()) {
        outBuffer.put(inBuffer.get());
      }
    }

    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull byte[] from, @Nonnull double[] to) {
      @Nonnull
      DoubleBuffer inBuffer = ByteBuffer.wrap(from).asDoubleBuffer();
      @Nonnull
      DoubleBuffer outBuffer = DoubleBuffer.wrap(to);
      while (inBuffer.hasRemaining()) {
        outBuffer.put(inBuffer.get());
      }
    }
  },
  /**
   * The Float.
   */
  Float(4) {
    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull double[] from, @Nonnull byte[] to) {
      @Nonnull
      DoubleBuffer inBuffer = DoubleBuffer.wrap(from);
      @Nonnull
      FloatBuffer outBuffer = ByteBuffer.wrap(to).asFloatBuffer();
      while (inBuffer.hasRemaining()) {
        outBuffer.put((float) inBuffer.get());
      }
    }

    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull byte[] from, @Nonnull double[] to) {
      @Nonnull
      FloatBuffer inBuffer = ByteBuffer.wrap(from).asFloatBuffer();
      @Nonnull
      DoubleBuffer outBuffer = DoubleBuffer.wrap(to);
      while (inBuffer.hasRemaining()) {
        outBuffer.put(inBuffer.get());
      }
    }
  },
  /**
   * The Uniform 32.
   */
  Uniform32(4) {
    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull double[] from, @Nonnull byte[] to) {
      DoubleSummaryStatistics statistics = RefArrays.stream(from).summaryStatistics();
      @Nonnull
      DoubleBuffer inBuffer = DoubleBuffer.wrap(from);
      @Nonnull
      FloatBuffer floatBuffer = ByteBuffer.wrap(to).asFloatBuffer();
      double min = statistics.getMin();
      double max = statistics.getMax();
      floatBuffer.put((float) min);
      floatBuffer.put((float) max);
      double center = (max + min) / 2;
      double radius = (max - min) / 2;
      @Nonnull
      IntBuffer byteBuffer = ByteBuffer.wrap(to).asIntBuffer();
      byteBuffer.position(2);
      while (inBuffer.hasRemaining()) {
        byteBuffer.put((int) (Integer.MAX_VALUE * (inBuffer.get() - center) / radius));
      }
    }

    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull byte[] from, @Nonnull double[] to) {
      @Nonnull
      DoubleBuffer outBuffer = DoubleBuffer.wrap(to);
      @Nonnull
      FloatBuffer floatBuffer = ByteBuffer.wrap(from).asFloatBuffer();
      double min = floatBuffer.get();
      double max = floatBuffer.get();
      double center = (max + min) / 2;
      double radius = (max - min) / 2;
      @Nonnull
      IntBuffer intBuffer = ByteBuffer.wrap(from).asIntBuffer();
      intBuffer.position(2);
      while (intBuffer.hasRemaining()) {
        int v = intBuffer.get();
        outBuffer.put(v * radius / Integer.MAX_VALUE + center);
      }
    }

    /**
     * Returns the size of the header.
     *
     *   @docgenVersion 9
     */
    @Override
    public int getHeaderSize() {
      return 8;
    }
  },
  /**
   * The Uniform 16.
   */
  Uniform16(2) {
    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull double[] from, @Nonnull byte[] to) {
      DoubleSummaryStatistics statistics = RefArrays.stream(from).summaryStatistics();
      @Nonnull
      DoubleBuffer inBuffer = DoubleBuffer.wrap(from);
      @Nonnull
      FloatBuffer floatBuffer = ByteBuffer.wrap(to).asFloatBuffer();
      double min = statistics.getMin();
      double max = statistics.getMax();
      floatBuffer.put((float) min);
      floatBuffer.put((float) max);
      double center = (max + min) / 2;
      double radius = (max - min) / 2;
      @Nonnull
      ShortBuffer shortBuffer = ByteBuffer.wrap(to).asShortBuffer();
      shortBuffer.position(4);
      while (inBuffer.hasRemaining()) {
        shortBuffer.put((short) (Short.MAX_VALUE * (inBuffer.get() - center) / radius));
      }
    }

    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull byte[] from, @Nonnull double[] to) {
      @Nonnull
      DoubleBuffer outBuffer = DoubleBuffer.wrap(to);
      @Nonnull
      FloatBuffer floatBuffer = ByteBuffer.wrap(from).asFloatBuffer();
      double min = floatBuffer.get();
      double max = floatBuffer.get();
      double center = (max + min) / 2;
      double radius = (max - min) / 2;
      @Nonnull
      ShortBuffer shortBuffer = ByteBuffer.wrap(from).asShortBuffer();
      shortBuffer.position(4);
      while (shortBuffer.hasRemaining()) {
        short v = shortBuffer.get();
        outBuffer.put(v * radius / Short.MAX_VALUE + center);
      }
    }

    /**
     * Returns the size of the header.
     *
     *   @docgenVersion 9
     */
    @Override
    public int getHeaderSize() {
      return 8;
    }
  },
  /**
   * The Uniform 8.
   */
  Uniform8(1) {
    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull double[] from, @Nonnull byte[] to) {
      DoubleSummaryStatistics statistics = RefArrays.stream(from).summaryStatistics();
      @Nonnull
      DoubleBuffer inBuffer = DoubleBuffer.wrap(from);
      @Nonnull
      FloatBuffer floatBuffer = ByteBuffer.wrap(to).asFloatBuffer();
      double min = statistics.getMin();
      double max = statistics.getMax();
      floatBuffer.put((float) min);
      floatBuffer.put((float) max);
      double center = (max + min) / 2;
      double radius = (max - min) / 2;
      @Nonnull
      ByteBuffer byteBuffer = ByteBuffer.wrap(to);
      byteBuffer.position(8);
      while (inBuffer.hasRemaining()) {
        byteBuffer.put((byte) (Byte.MAX_VALUE * (inBuffer.get() - center) / radius));
      }
    }

    /**
     * Copies the value of this object to another object.
     *
     *   @docgenVersion 9
     */
    @Override
    public void copy(@Nonnull byte[] from, @Nonnull double[] to) {
      @Nonnull
      DoubleBuffer outBuffer = DoubleBuffer.wrap(to);
      @Nonnull
      FloatBuffer floatBuffer = ByteBuffer.wrap(from).asFloatBuffer();
      double min = floatBuffer.get();
      double max = floatBuffer.get();
      double center = (max + min) / 2;
      double radius = (max - min) / 2;
      @Nonnull
      ByteBuffer byteBuffer = ByteBuffer.wrap(from);
      byteBuffer.position(8);
      while (byteBuffer.hasRemaining()) {
        byte v = byteBuffer.get();
        outBuffer.put(v * radius / Byte.MAX_VALUE + center);
      }
    }

    /**
     * Returns the size of the header.
     *
     *   @docgenVersion 9
     */
    @Override
    public int getHeaderSize() {
      return 8;
    }
  };

  private final int size;

  SerialPrecision(final int size) {
    this.size = size;
  }

  /**
   * Returns the size of the element.
   *
   * @docgenVersion 9
   */
  @Override
  public int getElementSize() {
    return size;
  }

  /**
   * Parse the input and return an array of doubles.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public double[] parse(@Nonnull String trim) {
    return fromBytes(Base64.getDecoder().decode(trim));
  }

  /**
   * Returns the Base64-encoded version of this String.
   *
   * @docgenVersion 9
   */
  public String base64(@Nonnull Tensor value) {
    String string = Base64.getEncoder().encodeToString(toBytes(value.getData()));
    value.freeRef();
    return string;
  }

  /**
   * Converts this value to a {@link Rational}.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public Rational toRational(double value, int maxScalar) {
    @Nonnull
    Rational current = rationalRecursion(value, 0);
    for (int i = 0; i < 10; i++) {
      @Nonnull
      Rational next = rationalRecursion(value, i);
      if (next.numerator < maxScalar && next.denominator < maxScalar) {
        current = next;
      } else {
        break;
      }
    }
    return current;
  }

  /**
   * This method returns a Rational number
   * using recursion.
   *
   * @docgenVersion 9
   */
  @Nonnull
  private Rational rationalRecursion(double value, int recursions) {
    if (value < 0) {
      @Nonnull
      Rational rational = rationalRecursion(-value, recursions);
      return new Rational(-rational.numerator, rational.denominator);
    } else if (0 == value) {
      return new Rational(0, 1);
    } else if (value >= 1) {
      int scalar = (int) value;
      @Nonnull
      Rational rational = rationalRecursion(value - scalar, recursions);
      return new Rational(rational.numerator + scalar * rational.denominator, rational.denominator);
    } else if (recursions <= 0) {
      return new Rational((int) Math.round(value), 1);
    } else {
      @Nonnull
      Rational rational = rationalRecursion(1.0 / value, recursions - 1);
      return new Rational(rational.denominator, rational.numerator);
    }
  }

  /**
   * This class represents a rational number.
   * It is immutable, meaning that once created,
   * the numerator and denominator cannot be changed.
   *
   * @docgenVersion 9
   */
  public static class Rational {
    /**
     * The Numerator.
     */
    public final int numerator;
    /**
     * The Denominator.
     */
    public final int denominator;

    /**
     * Instantiates a new Rational.
     *
     * @param numerator   the numerator
     * @param denominator the denominator
     */
    public Rational(int numerator, int denominator) {
      this.numerator = numerator;
      this.denominator = denominator;
    }
  }
}
