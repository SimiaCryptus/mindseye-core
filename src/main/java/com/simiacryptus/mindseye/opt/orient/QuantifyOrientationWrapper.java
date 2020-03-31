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
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.data.DoubleStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class QuantifyOrientationWrapper extends OrientationStrategyBase<LineSearchCursor> {

  @Nullable
  private final OrientationStrategy<? extends LineSearchCursor> inner;

  public QuantifyOrientationWrapper(@Nullable final OrientationStrategy<? extends LineSearchCursor> inner) {
    OrientationStrategy<? extends LineSearchCursor> temp_02_0001 = inner == null ? null : inner.addRef();
    this.inner = temp_02_0001 == null ? null : temp_02_0001.addRef();
    if (null != temp_02_0001)
      temp_02_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  @Nonnull
  public CharSequence getId(@Nonnull final DoubleBuffer<UUID> x) {
    String temp_02_0006 = x.toString();
    x.freeRef();
    return temp_02_0006;
  }

  @Override
  public LineSearchCursor orient(@Nullable final Trainable subject, @Nullable final PointSample measurement,
                                 @Nonnull final TrainingMonitor monitor) {
    assert inner != null;
    final LineSearchCursor cursor = inner.orient(subject, measurement, monitor);
    if (cursor instanceof SimpleLineSearchCursor) {
      assert ((SimpleLineSearchCursor) cursor).direction != null;
      final DeltaSet<UUID> direction = ((SimpleLineSearchCursor) cursor).direction.addRef();
      @Nonnull final StateSet<UUID> weights = ((SimpleLineSearchCursor) cursor).origin.weights.addRef();
      RefMap<CharSequence, RefList<State<UUID>>> temp_02_0007 = weights.stream().collect(RefCollectors.groupingBy(x -> {
        return getId(x);
      }, RefCollectors.toList()));
      RefSet<Map.Entry<CharSequence, RefList<State<UUID>>>> temp_02_0008 = temp_02_0007.entrySet();
      final RefMap<CharSequence, CharSequence> dataMap = temp_02_0008.stream().collect(RefCollectors.toMap(x -> {
        CharSequence temp_02_0003 = x.getKey();
        RefUtil.freeRef(x);
        return temp_02_0003;
      }, RefUtil.wrapInterface(
          (Function<? super Map.Entry<CharSequence, RefList<State<UUID>>>, ? extends CharSequence>) list -> {
            RefList<State<UUID>> temp_02_0009 = list.getValue();
            final RefList<Double> doubleList = temp_02_0009.stream()
                .map(RefUtil.wrapInterface((Function<? super State<UUID>, ? extends Double>) weightDelta -> {
                  final DoubleBuffer<UUID> dirDelta = direction.get(weightDelta.key);
                  final double denominator = weightDelta.deltaStatistics().rms();
                  weightDelta.freeRef();
                  final double numerator = null == dirDelta ? 0 : dirDelta.deltaStatistics().rms();
                  if (null != dirDelta)
                    dirDelta.freeRef();
                  return numerator / (0 == denominator ? 1 : denominator);
                }, direction.addRef())).collect(RefCollectors.toList());
            temp_02_0009.freeRef();
            RefUtil.freeRef(list);
            if (1 == doubleList.size()) {
              double d = doubleList.get(0);
              String temp_02_0005 = Double.toString(d);
              doubleList.freeRef();
              return temp_02_0005;
            }
            String temp_02_0004 = new DoubleStatistics().accept(doubleList.stream().mapToDouble(x -> x).toArray())
                .toString();
            doubleList.freeRef();
            return temp_02_0004;
          }, direction)));
      temp_02_0008.freeRef();
      temp_02_0007.freeRef();
      weights.freeRef();
      monitor.log(RefString.format("Line search stats: %s", dataMap));
    } else {
      monitor.log(RefString.format("Non-simple cursor: %s", cursor.addRef()));
    }
    return cursor;
  }

  @Override
  public void reset() {
    assert inner != null;
    inner.reset();
  }

  public void _free() {
    super._free();
    if (null != inner)
      inner.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  QuantifyOrientationWrapper addRef() {
    return (QuantifyOrientationWrapper) super.addRef();
  }

}
