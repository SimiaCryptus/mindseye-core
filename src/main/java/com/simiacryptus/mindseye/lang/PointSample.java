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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefSet;
import com.simiacryptus.ref.wrappers.RefStringBuilder;

import javax.annotation.Nonnull;
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
      DeltaSet<UUID> temp_08_0001 = new DeltaSet<>(delta.addRef());
      this.delta = temp_08_0001.addRef();
      temp_08_0001.freeRef();
      StateSet<UUID> temp_08_0002 = new StateSet<>(weights.addRef());
      this.weights = temp_08_0002.addRef();
      temp_08_0002.freeRef();
      RefMap<UUID, Delta<UUID>> temp_08_0010 = delta.getMap();
      RefSet<UUID> temp_08_0011 = temp_08_0010.keySet();
      assert temp_08_0011.stream().allMatch(RefUtil.wrapInterface((Predicate<? super UUID>) x -> {
        return weightsMap.containsKey(x);
      }, weights.addRef()));
      temp_08_0011.freeRef();
      temp_08_0010.freeRef();
      this.sum = sum;
      this.count = count;
      setRate(rate);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      weightsMap.freeRef();
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

  public void setRate(double rate) {
    this.rate = rate;
  }

  @Nonnull
  public static PointSample add(@Nonnull final PointSample left, @Nonnull final PointSample right) {
    RefMap<UUID, Delta<UUID>> temp_08_0012 = left.delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0013 = left.weights.getMap();
    assert temp_08_0012.size() == temp_08_0013.size();
    temp_08_0013.freeRef();
    temp_08_0012.freeRef();
    RefMap<UUID, Delta<UUID>> temp_08_0014 = right.delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0015 = right.weights.getMap();
    assert temp_08_0014.size() == temp_08_0015.size();
    temp_08_0015.freeRef();
    temp_08_0014.freeRef();
    assert left.rate == right.rate;
    DeltaSet<UUID> delta = left.delta.add(right.delta.addRef());
    StateSet<UUID> stateSet = StateSet.union(left.weights.addRef(), right.weights.addRef());
    PointSample temp_08_0003 = new PointSample(delta.addRef(),
        stateSet.addRef(), left.sum + right.sum, left.rate, left.count + right.count);
    right.freeRef();
    left.freeRef();
    stateSet.freeRef();
    delta.freeRef();
    return temp_08_0003;
  }


  @Nonnull
  public PointSample add(@Nonnull final PointSample right) {
    PointSample temp_08_0006 = PointSample.add(this.addRef(), right);
    return temp_08_0006;
  }

  @Nonnull
  public PointSample addInPlace(@Nonnull final PointSample right) {
    RefMap<UUID, Delta<UUID>> temp_08_0016 = delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0017 = weights.getMap();
    assert temp_08_0016.size() == temp_08_0017.size();
    temp_08_0017.freeRef();
    temp_08_0016.freeRef();
    RefMap<UUID, Delta<UUID>> temp_08_0018 = right.delta.getMap();
    RefMap<UUID, State<UUID>> temp_08_0019 = right.weights.getMap();
    assert temp_08_0018.size() == temp_08_0019.size();
    temp_08_0019.freeRef();
    temp_08_0018.freeRef();
    assert rate == right.rate;
    delta.addInPlace(right.delta.addRef());
    PointSample temp_08_0007 = new PointSample(delta.addRef(),
        StateSet.union(weights.addRef(), right.weights.addRef()), sum + right.sum, rate,
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
    PointSample temp_08_0004 = new PointSample(deltaCopy,
        weightsCopy, sum, rate, count);
    return temp_08_0004;
  }

  @Nonnull
  public PointSample normalize() {
    if (count == 1) {
      return this.addRef();
    } else {
      @Nonnull
      DeltaSet<UUID> scale = delta.scale(1.0 / count);
      PointSample temp_08_0005 = new PointSample(scale,
          weights.addRef(), sum / count, rate, 1);
      return temp_08_0005;
    }
  }

  public void restore() {
    weights.stream().forEach(d -> {
      d.restore();
      d.freeRef();
    });
  }

  public void backup() {
    weights.stream().forEach(d -> {
      d.backup();
      d.freeRef();
    });
  }

  @Override
  public String toString() {
    @Nonnull final RefStringBuilder sb = new RefStringBuilder(
        "PointSample{");
    sb.append("avg=").append(getMean());
    sb.append('}');
    return sb.toString();
  }

  public void _free() {
    weights.freeRef();
    delta.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  PointSample addRef() {
    return (PointSample) super.addRef();
  }
}
