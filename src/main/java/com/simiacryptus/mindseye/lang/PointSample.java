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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefSet;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Predicate;

public final class PointSample extends ReferenceCountingBase {
  public final int count;
  @Nonnull
  public final DeltaSet<UUID> delta;
  public final double sum;
  @Nonnull
  public final StateSet<UUID> weights;
  public double rate;

  public PointSample(@Nonnull final DeltaSet<UUID> delta, @Nonnull final StateSet<UUID> weights, final double sum,
      final double rate, final int count) {
    RefMap<UUID, Delta<UUID>> deltaMap = delta.getMap();
    RefMap<UUID, State<UUID>> weightsMap = weights.getMap();
    try {
      assert deltaMap.size() == weightsMap.size();
      DeltaSet<UUID> temp_08_0001 = new DeltaSet<>(delta == null ? null : delta.addRef());
      this.delta = temp_08_0001 == null ? null : temp_08_0001.addRef();
      if (null != temp_08_0001)
        temp_08_0001.freeRef();
      StateSet<UUID> temp_08_0002 = new StateSet<>(weights == null ? null : weights.addRef());
      this.weights = temp_08_0002 == null ? null : temp_08_0002.addRef();
      if (null != temp_08_0002)
        temp_08_0002.freeRef();
      RefMap<UUID, Delta<UUID>> temp_08_0010 = delta.getMap();
      RefSet<UUID> temp_08_0011 = temp_08_0010.keySet();
      assert temp_08_0011.stream().allMatch(RefUtil.wrapInterface((Predicate<? super UUID>) x -> {
        return weightsMap.containsKey(x);
      }, weights == null ? null : weights.addRef()));
      if (null != temp_08_0011)
        temp_08_0011.freeRef();
      if (null != temp_08_0010)
        temp_08_0010.freeRef();
      this.sum = sum;
      this.count = count;
      RefUtil.freeRef(setRate(rate));
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      if (null != weightsMap)
        weightsMap.freeRef();
      if (null != deltaMap)
        deltaMap.freeRef();
    }
    weights.freeRef();
    delta.freeRef();
  }

  public double getMean() {
    return sum / count;
  }

  public double getRate() {
    return rate;
  }

  @Nonnull
  public PointSample setRate(final double rate) {
    this.rate = rate;
    return this.addRef();
  }

  public static PointSample add(@Nonnull final PointSample left, @Nonnull final PointSample right) {
    RefMap<UUID, Delta<UUID>> temp_08_0012 = left.delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0013 = left.weights.getMap();
    assert temp_08_0012.size() == temp_08_0013.size();
    if (null != temp_08_0013)
      temp_08_0013.freeRef();
    if (null != temp_08_0012)
      temp_08_0012.freeRef();
    RefMap<UUID, Delta<UUID>> temp_08_0014 = right.delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0015 = right.weights.getMap();
    assert temp_08_0014.size() == temp_08_0015.size();
    if (null != temp_08_0015)
      temp_08_0015.freeRef();
    if (null != temp_08_0014)
      temp_08_0014.freeRef();
    assert left.rate == right.rate;
    DeltaSet<UUID> delta = left.delta.add(right.delta.addRef());
    StateSet<UUID> stateSet = StateSet.union(left.weights.addRef(), right.weights.addRef());
    PointSample temp_08_0003 = new PointSample(delta == null ? null : delta.addRef(),
        stateSet == null ? null : stateSet.addRef(), left.sum + right.sum, left.rate, left.count + right.count);
    right.freeRef();
    left.freeRef();
    if (null != stateSet)
      stateSet.freeRef();
    if (null != delta)
      delta.freeRef();
    return temp_08_0003;
  }

  public static @SuppressWarnings("unused") PointSample[] addRefs(PointSample[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(PointSample::addRef).toArray((x) -> new PointSample[x]);
  }

  public static @SuppressWarnings("unused") PointSample[][] addRefs(PointSample[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(PointSample::addRefs).toArray((x) -> new PointSample[x][]);
  }

  public PointSample add(@Nonnull final PointSample right) {
    PointSample temp_08_0006 = PointSample.add(this.addRef(), right == null ? null : right);
    return temp_08_0006;
  }

  public PointSample addInPlace(@Nonnull final PointSample right) {
    RefMap<UUID, Delta<UUID>> temp_08_0016 = delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0017 = weights.getMap();
    assert temp_08_0016.size() == temp_08_0017.size();
    if (null != temp_08_0017)
      temp_08_0017.freeRef();
    if (null != temp_08_0016)
      temp_08_0016.freeRef();
    RefMap<UUID, Delta<UUID>> temp_08_0018 = right.delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0019 = right.weights.getMap();
    assert temp_08_0018.size() == temp_08_0019.size();
    if (null != temp_08_0019)
      temp_08_0019.freeRef();
    if (null != temp_08_0018)
      temp_08_0018.freeRef();
    assert rate == right.rate;
    PointSample temp_08_0007 = new PointSample(delta.addInPlace(right.delta.addRef()),
        StateSet.union(weights == null ? null : weights.addRef(), right.weights.addRef()), sum + right.sum, rate,
        count + right.count);
    right.freeRef();
    return temp_08_0007;
  }

  @Nonnull
  public PointSample copyFull() {
    @Nonnull
    DeltaSet<UUID> deltaCopy = delta.copy();
    @Nonnull
    StateSet<UUID> weightsCopy = weights.copy();
    PointSample temp_08_0004 = new PointSample(deltaCopy == null ? null : deltaCopy,
        weightsCopy == null ? null : weightsCopy, sum, rate, count);
    return temp_08_0004;
  }

  @Nonnull
  public PointSample normalize() {
    if (count == 1) {
      return this.addRef();
    } else {
      @Nonnull
      DeltaSet<UUID> scale = delta.scale(1.0 / count);
      PointSample temp_08_0005 = new PointSample(scale == null ? null : scale,
          weights == null ? null : weights.addRef(), sum / count, rate, 1);
      return temp_08_0005;
    }
  }

  @Nonnull
  public PointSample restore() {
    weights.stream().forEach(d -> {
      RefUtil.freeRef(d.restore());
      if (null != d)
        d.freeRef();
    });
    return this.addRef();
  }

  @Nonnull
  public PointSample backup() {
    weights.stream().forEach(d -> {
      RefUtil.freeRef(d.backup());
      if (null != d)
        d.freeRef();
    });
    return this.addRef();
  }

  @Override
  public String toString() {
    @Nonnull
    final com.simiacryptus.ref.wrappers.RefStringBuilder sb = new com.simiacryptus.ref.wrappers.RefStringBuilder(
        "PointSample{");
    sb.append("avg=").append(getMean());
    sb.append('}');
    return sb.toString();
  }

  public void _free() {
    weights.freeRef();
    delta.freeRef();
  }

  public @Override @SuppressWarnings("unused") PointSample addRef() {
    return (PointSample) super.addRef();
  }
}
