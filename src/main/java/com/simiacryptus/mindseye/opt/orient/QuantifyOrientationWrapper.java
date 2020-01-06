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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefSet;
import com.simiacryptus.ref.wrappers.RefString;
import com.simiacryptus.util.data.DoubleStatistics;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public @RefAware
class QuantifyOrientationWrapper extends OrientationStrategyBase<LineSearchCursor> {

  private final OrientationStrategy<? extends LineSearchCursor> inner;

  public QuantifyOrientationWrapper(final OrientationStrategy<? extends LineSearchCursor> inner) {
    {
      OrientationStrategy<? extends LineSearchCursor> temp_02_0001 = inner == null
          ? null
          : inner.addRef();
      this.inner = temp_02_0001 == null ? null : temp_02_0001.addRef();
      if (null != temp_02_0001)
        temp_02_0001.freeRef();
    }
    if (null != inner)
      inner.freeRef();
  }

  public static @SuppressWarnings("unused")
  QuantifyOrientationWrapper[] addRefs(QuantifyOrientationWrapper[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(QuantifyOrientationWrapper::addRef)
        .toArray((x) -> new QuantifyOrientationWrapper[x]);
  }

  public static @SuppressWarnings("unused")
  QuantifyOrientationWrapper[][] addRefs(
      QuantifyOrientationWrapper[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(QuantifyOrientationWrapper::addRefs)
        .toArray((x) -> new QuantifyOrientationWrapper[x][]);
  }

  @Nonnull
  public CharSequence getId(@Nonnull final DoubleBuffer<UUID> x) {
    String temp_02_0006 = x.toString();
    x.freeRef();
    return temp_02_0006;
  }

  @Override
  public LineSearchCursor orient(final Trainable subject, final PointSample measurement,
                                 @Nonnull final TrainingMonitor monitor) {
    final LineSearchCursor cursor = inner.orient(subject == null ? null : subject.addRef(),
        measurement == null ? null : measurement.addRef(), monitor);
    if (null != measurement)
      measurement.freeRef();
    if (null != subject)
      subject.freeRef();
    if (cursor instanceof SimpleLineSearchCursor) {
      final DeltaSet<UUID> direction = ((SimpleLineSearchCursor) cursor).direction.addRef();
      @Nonnull final StateSet<UUID> weights = ((SimpleLineSearchCursor) cursor).origin.weights.addRef();
      RefMap<CharSequence, RefList<State<UUID>>> temp_02_0007 = weights
          .stream().collect(RefCollectors.groupingBy(x -> {
            CharSequence temp_02_0002 = getId(x == null ? null : x.addRef());
            if (null != x)
              x.freeRef();
            return temp_02_0002;
          }, RefCollectors.toList()));
      RefSet<Map.Entry<CharSequence, RefList<State<UUID>>>> temp_02_0008 = temp_02_0007
          .entrySet();
      final RefMap<CharSequence, CharSequence> dataMap = temp_02_0008.stream().collect(RefCollectors.toMap(x -> {
        CharSequence temp_02_0003 = x.getKey();
        if (null != x)
          RefUtil.freeRef(x);
        return temp_02_0003;
      }, RefUtil.wrapInterface(
          (Function<? super Map.Entry<CharSequence, RefList<State<UUID>>>, ? extends CharSequence>) list -> {
            RefList<State<UUID>> temp_02_0009 = list
                .getValue();
            final RefList<Double> doubleList = temp_02_0009.stream()
                .map(RefUtil.wrapInterface(
                    (Function<? super State<UUID>, ? extends Double>) weightDelta -> {
                      RefMap<UUID, Delta<UUID>> temp_02_0010 = direction
                          .getMap();
                      final DoubleBuffer<UUID> dirDelta = temp_02_0010.get(weightDelta.key);
                      if (null != temp_02_0010)
                        temp_02_0010.freeRef();
                      final double denominator = weightDelta.deltaStatistics().rms();
                      if (null != weightDelta)
                        weightDelta.freeRef();
                      final double numerator = null == dirDelta ? 0 : dirDelta.deltaStatistics().rms();
                      if (null != dirDelta)
                        dirDelta.freeRef();
                      return numerator / (0 == denominator ? 1 : denominator);
                    }, direction == null ? null : direction.addRef()))
                .collect(RefCollectors.toList());
            if (null != temp_02_0009)
              temp_02_0009.freeRef();
            if (null != list)
              RefUtil.freeRef(list);
            if (1 == doubleList.size()) {
              String temp_02_0005 = Double.toString(doubleList.get(0));
              if (null != doubleList)
                doubleList.freeRef();
              return temp_02_0005;
            }
            String temp_02_0004 = new DoubleStatistics()
                .accept(doubleList.stream().mapToDouble(x -> x).toArray()).toString();
            if (null != doubleList)
              doubleList.freeRef();
            return temp_02_0004;
          }, direction == null ? null : direction.addRef())));
      if (null != temp_02_0008)
        temp_02_0008.freeRef();
      if (null != temp_02_0007)
        temp_02_0007.freeRef();
      weights.freeRef();
      if (null != direction)
        direction.freeRef();
      monitor.log(RefString.format("Line search stats: %s", dataMap));
      if (null != dataMap)
        dataMap.freeRef();
    } else {
      monitor.log(RefString.format("Non-simple cursor: %s", cursor));
    }
    return cursor;
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  QuantifyOrientationWrapper addRef() {
    return (QuantifyOrientationWrapper) super.addRef();
  }

}
