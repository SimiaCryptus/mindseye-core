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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class DescribeOrientationWrapper extends OrientationStrategyBase<LineSearchCursor> {

  @Nullable
  private final OrientationStrategy<? extends LineSearchCursor> inner;

  public DescribeOrientationWrapper(@Nullable final OrientationStrategy<? extends LineSearchCursor> inner) {
    OrientationStrategy<? extends LineSearchCursor> temp_27_0001 = inner == null ? null : inner.addRef();
    this.inner = temp_27_0001 == null ? null : temp_27_0001.addRef();
    if (null != temp_27_0001)
      temp_27_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  @Nonnull
  public static CharSequence getId(@Nonnull final DoubleBuffer<UUID> x) {
    String temp_27_0009 = x.key.toString();
    x.freeRef();
    return temp_27_0009;
  }

  @Nonnull
  public static CharSequence render(@Nonnull final DoubleBuffer<UUID> weightDelta,
                                    @Nonnull final DoubleBuffer<UUID> dirDelta) {
    @Nonnull final CharSequence weightString = RefArrays.toString(weightDelta.getDelta());
    weightDelta.freeRef();
    @Nonnull final CharSequence deltaString = RefArrays.toString(dirDelta.getDelta());
    dirDelta.freeRef();
    return RefString.format("pos: %s\nvec: %s", weightString, deltaString);
  }

  public static CharSequence render(@Nonnull final StateSet<UUID> weights, @Nonnull final DeltaSet<UUID> direction) {
    RefMap<CharSequence, RefList<State<UUID>>> temp_27_0010 = weights.stream().collect(RefCollectors.groupingBy(x -> {
      CharSequence temp_27_0002 = DescribeOrientationWrapper.getId(x == null ? null : x.addRef());
      if (null != x)
        x.freeRef();
      return temp_27_0002;
    }, RefCollectors.toList()));
    RefSet<Map.Entry<CharSequence, RefList<State<UUID>>>> temp_27_0011 = temp_27_0010.entrySet();
    final RefMap<CharSequence, CharSequence> data = temp_27_0011.stream().collect(RefCollectors.toMap(x -> {
      CharSequence temp_27_0003 = x.getKey();
      RefUtil.freeRef(x);
      return temp_27_0003;
    }, RefUtil.wrapInterface((Function<Map.Entry<CharSequence, RefList<State<UUID>>>, ? extends CharSequence>) (
        @Nonnull final Map.Entry<CharSequence, RefList<State<UUID>>> list) -> {
      final RefList<State<UUID>> deltaList = list.getValue();
      RefUtil.freeRef(list);
      if (1 == deltaList.size()) {
        final State<UUID> weightDelta = deltaList.get(0);
        deltaList.freeRef();
        RefMap<UUID, Delta<UUID>> temp_27_0012 = direction.getMap();
        assert weightDelta != null;
        CharSequence temp_27_0005 = DescribeOrientationWrapper.render(weightDelta.addRef(),
            temp_27_0012.get(weightDelta.key));
        temp_27_0012.freeRef();
        weightDelta.freeRef();
        return temp_27_0005;
      } else {
        CharSequence temp_27_0004 = deltaList.stream()
            .map(RefUtil.wrapInterface((Function<State<UUID>, CharSequence>) weightDelta -> {
              RefMap<UUID, Delta<UUID>> temp_27_0013 = direction.getMap();
              assert weightDelta != null;
              CharSequence temp_27_0006 = DescribeOrientationWrapper
                  .render(weightDelta.addRef(), temp_27_0013.get(weightDelta.key));
              temp_27_0013.freeRef();
              weightDelta.freeRef();
              return temp_27_0006;
            }, direction.addRef())).limit(10).reduce((a, b) -> a + "\n" + b).orElse("");
        deltaList.freeRef();
        return temp_27_0004;
      }
    }, direction)));
    temp_27_0011.freeRef();
    temp_27_0010.freeRef();
    weights.freeRef();
    RefSet<Map.Entry<CharSequence, CharSequence>> temp_27_0014 = data.entrySet();
    String temp_27_0007 = temp_27_0014.stream().map(e -> {
      String temp_27_0008 = RefString.format("%s = %s", e.getKey(), e.getValue());
      RefUtil.freeRef(e);
      return temp_27_0008;
    }).map(str -> str.replaceAll("\n", "\n\t")).reduce((a, b) -> a + "\n" + b).orElse("");
    temp_27_0014.freeRef();
    data.freeRef();
    return temp_27_0007;
  }

  @Override
  public LineSearchCursor orient(@Nullable final Trainable subject, @Nullable final PointSample measurement,
                                 @Nonnull final TrainingMonitor monitor) {
    assert inner != null;
    final LineSearchCursor cursor = inner.orient(subject, measurement, monitor);
    if (cursor instanceof SimpleLineSearchCursor) {
      SimpleLineSearchCursor simpleLineSearchCursor = (SimpleLineSearchCursor) cursor.addRef();
      assert simpleLineSearchCursor.direction != null;
      final DeltaSet<UUID> direction = simpleLineSearchCursor.direction.addRef();
      @Nonnull final StateSet<UUID> weights = simpleLineSearchCursor.origin.weights.addRef();
      simpleLineSearchCursor.freeRef();
      final CharSequence asString = DescribeOrientationWrapper.render(weights, direction);
      monitor.log(RefString.format("Orientation Details: %s", asString));
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
  DescribeOrientationWrapper addRef() {
    return (DescribeOrientationWrapper) super.addRef();
  }
}
