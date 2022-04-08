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

package com.simiacryptus.ref.lang;

import com.simiacryptus.mindseye.lang.DataSerializer;
import com.simiacryptus.mindseye.lang.SerialPrecision;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefString;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.DoubleSupplier;

/**
 * This is the DataSerializerTest class.
 * It contains a logger for the DataSerializerTest class.
 *
 * @docgenVersion 9
 */
public class DataSerializerTest {
  private static final Logger log = LoggerFactory.getLogger(DataSerializerTest.class);

  /**
   * Test the double precision serialization
   *
   * @docgenVersion 9
   */
  @Test
  @Tag("UnitTest")
  public void testDouble() {
    test(SerialPrecision.Double);
  }

  /**
   * Test the Float precision.
   *
   * @docgenVersion 9
   */
  @Test
  @Tag("UnitTest")
  public void testFloat() {
    test(SerialPrecision.Float);
  }

  /**
   * Test the uniform32 precision.
   *
   * @docgenVersion 9
   */
  @Test
  @Tag("UnitTest")
  public void testUniform32() {
    test(SerialPrecision.Uniform32);
  }

  /**
   * Test the uniform16 precision.
   *
   * @docgenVersion 9
   */
  @Test
  @Tag("UnitTest")
  public void testUniform16() {
    test(SerialPrecision.Uniform16);
  }

  /**
   * Test the uniform8 serialization precision.
   *
   * @docgenVersion 9
   */
  @Test
  @Tag("UnitTest")
  public void testUniform8() {
    test(SerialPrecision.Uniform8);
  }

  /**
   * Test the given DataSerializer with random1 and random2, using "Uniform" and "Exponential" as labels.
   *
   * @docgenVersion 9
   */
  public void test(@Nonnull DataSerializer target) {
    test(target, this::random1, "Uniform");
    test(target, this::random2, "Exponential");
  }

  /**
   * @param target the DataSerializer to test
   * @param f      the function to use for testing
   * @param name   the name of the test
   * @docgenVersion 9
   */
  public void test(@Nonnull DataSerializer target, @Nonnull DoubleSupplier f, CharSequence name) {
    @Nonnull
    double[] source = random(1024, f);
    @Nonnull
    double[] result = target.fromBytes(target.toBytes(source));
    double rms = RefIntStream.range(0, source.length)
        .mapToDouble(i -> (source[i] - result[i]) / (source[i] + result[i])).map(x -> x * x).average().getAsDouble();
    log.info(RefString.format("%s RMS: %s", name, rms));
    //assert rms < 1e-4;
  }

  /**
   * @param i the number of elements of the array
   * @param f the function used to generate the random numbers
   * @return an array of random numbers
   * @docgenVersion 9
   */
  @Nonnull
  private double[] random(int i, @Nonnull DoubleSupplier f) {
    @Nonnull
    double[] doubles = new double[i];
    RefArrays.parallelSetAll(doubles, j -> f.getAsDouble());
    return doubles;
  }

  /**
   * Returns a random double between 0 and 1.
   *
   * @docgenVersion 9
   */
  private double random1() {
    return Math.random();
  }

  /**
   * Returns a random number between 0 and 1.
   *
   * @docgenVersion 9
   */
  private double random2() {
    return Math.exp(Math.random());
  }

}
