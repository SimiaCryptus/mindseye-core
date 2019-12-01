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
import com.simiacryptus.util.test.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.DoubleSupplier;
import java.util.stream.IntStream;

public class DataSerializerTest {
  private static final Logger log = LoggerFactory.getLogger(DataSerializerTest.class);

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testDouble() {
    test(SerialPrecision.Double);
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testFloat() {
    test(SerialPrecision.Float);
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testUniform32() {
    test(SerialPrecision.Uniform32);
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testUniform16() {
    test(SerialPrecision.Uniform16);
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testUniform8() {
    test(SerialPrecision.Uniform8);
  }

  public void test(@Nonnull DataSerializer target) {
    test(target, this::random1, "Uniform");
    test(target, this::random2, "Exponential");
  }

  public void test(@Nonnull DataSerializer target, @Nonnull DoubleSupplier f, CharSequence name) {
    @Nonnull double[] source = random(1024, f);
    @Nonnull double[] result = target.fromBytes(target.toBytes(source));
    double rms = IntStream.range(0, source.length).mapToDouble(i -> (source[i] - result[i]) / (source[i] + result[i])).map(x -> x * x).average().getAsDouble();
    log.info(String.format("%s RMS: %s", name, rms));
    //assert rms < 1e-4;
  }

  @Nonnull
  private double[] random(int i, @Nonnull DoubleSupplier f) {
    @Nonnull double[] doubles = new double[i];
    Arrays.parallelSetAll(doubles, j -> f.getAsDouble());
    return doubles;
  }

  private double random1() {
    return Math.random();
  }

  private double random2() {
    return Math.exp(Math.random());
  }

}
