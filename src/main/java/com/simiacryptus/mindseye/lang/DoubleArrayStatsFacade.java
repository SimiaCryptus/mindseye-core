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

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.stream.DoubleStream;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefDoubleStream;

public @com.simiacryptus.ref.lang.RefAware class DoubleArrayStatsFacade {
  private final double[] data;

  public DoubleArrayStatsFacade(final double[] data) {
    this.data = data;
  }

  public int length() {
    return data.length;
  }

  public double rms() {
    return Math.sqrt(sumSq() / length());
  }

  public double sum() {
    final DoubleSummaryStatistics statistics = com.simiacryptus.ref.wrappers.RefArrays.stream(data).summaryStatistics();
    return statistics.getSum();
  }

  public double sumSq() {
    final com.simiacryptus.ref.wrappers.RefDoubleStream doubleStream = com.simiacryptus.ref.wrappers.RefArrays
        .stream(data).map((final double x) -> x * x);
    final DoubleSummaryStatistics statistics = doubleStream.summaryStatistics();
    return statistics.getSum();
  }
}
