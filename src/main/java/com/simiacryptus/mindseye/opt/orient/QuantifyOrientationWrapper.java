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

package com.simiacryptus.mindseye.opt.orient;

import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.DoubleBuffer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.lang.StateSet;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.util.data.DoubleStatistics;

import javax.annotation.Nonnull;
import java.util.UUID;

public @com.simiacryptus.ref.lang.RefAware
class QuantifyOrientationWrapper
    extends OrientationStrategyBase<LineSearchCursor> {

  private final OrientationStrategy<? extends LineSearchCursor> inner;

  public QuantifyOrientationWrapper(final OrientationStrategy<? extends LineSearchCursor> inner) {
    this.inner = inner;
  }

  public static @SuppressWarnings("unused")
  QuantifyOrientationWrapper[] addRefs(QuantifyOrientationWrapper[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(QuantifyOrientationWrapper::addRef)
        .toArray((x) -> new QuantifyOrientationWrapper[x]);
  }

  public static @SuppressWarnings("unused")
  QuantifyOrientationWrapper[][] addRefs(
      QuantifyOrientationWrapper[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(QuantifyOrientationWrapper::addRefs)
        .toArray((x) -> new QuantifyOrientationWrapper[x][]);
  }

  @Nonnull
  public CharSequence getId(@Nonnull final DoubleBuffer<UUID> x) {
    return x.toString();
  }

  @Override
  public LineSearchCursor orient(final Trainable subject, final PointSample measurement,
                                 @Nonnull final TrainingMonitor monitor) {
    final LineSearchCursor cursor = inner.orient(subject, measurement, monitor);
    if (cursor instanceof SimpleLineSearchCursor) {
      final DeltaSet<UUID> direction = ((SimpleLineSearchCursor) cursor).direction;
      @Nonnull final StateSet<UUID> weights = ((SimpleLineSearchCursor) cursor).origin.weights;
      final com.simiacryptus.ref.wrappers.RefMap<CharSequence, CharSequence> dataMap = weights.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.groupingBy(x -> getId(x),
              com.simiacryptus.ref.wrappers.RefCollectors.toList()))
          .entrySet().stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toMap(x -> x.getKey(), list -> {
            final com.simiacryptus.ref.wrappers.RefList<Double> doubleList = list.getValue().stream()
                .map(weightDelta -> {
                  final DoubleBuffer<UUID> dirDelta = direction.getMap().get(weightDelta.key);
                  final double denominator = weightDelta.deltaStatistics().rms();
                  final double numerator = null == dirDelta ? 0 : dirDelta.deltaStatistics().rms();
                  return numerator / (0 == denominator ? 1 : denominator);
                }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
            if (1 == doubleList.size())
              return Double.toString(doubleList.get(0));
            return new DoubleStatistics().accept(doubleList.stream().mapToDouble(x -> x).toArray()).toString();
          }));
      monitor.log(String.format("Line search stats: %s", dataMap));
    } else {
      monitor.log(String.format("Non-simple cursor: %s", cursor));
    }
    return cursor;
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  QuantifyOrientationWrapper addRef() {
    return (QuantifyOrientationWrapper) super.addRef();
  }

}
