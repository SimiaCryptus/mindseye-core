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
import com.simiacryptus.ref.wrappers.RefDoubleStream;

import java.util.DoubleSummaryStatistics;

/**
 * This class provides a facade for statistical operations on an array of doubles.
 * The data is stored in a private array field.
 *
 * @docgenVersion 9
 */
public class DoubleArrayStatsFacade {
  private final double[] data;

  /**
   * Instantiates a new Double array stats facade.
   *
   * @param data the data
   */
  public DoubleArrayStatsFacade(final double[] data) {
    this.data = data;
  }

  /**
   * Returns the length of the string.
   *
   * @docgenVersion 9
   */
  public int length() {
    return data.length;
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
   * Returns the sum of the elements in the array.
   *
   * @docgenVersion 9
   */
  public double sum() {
    final DoubleSummaryStatistics statistics = RefArrays.stream(data).summaryStatistics();
    return statistics.getSum();
  }

  /**
   * Calculates the sum of the squares of all the numbers in the list.
   *
   * @docgenVersion 9
   */
  public double sumSq() {
    final RefDoubleStream doubleStream = RefArrays.stream(data).map((final double x) -> x * x);
    final DoubleSummaryStatistics statistics = doubleStream.summaryStatistics();
    return statistics.getSum();
  }
}
