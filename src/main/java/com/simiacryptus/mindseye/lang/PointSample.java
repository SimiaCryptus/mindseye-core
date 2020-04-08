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

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefStringBuilder;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import java.util.UUID;

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
    try {
      assert delta.size() == weights.size();
      this.delta = new DeltaSet<>(delta.addRef());
      this.weights = new StateSet<>(weights.addRef());
      if (!weights.containsAll(delta.getMap())) throw new IllegalStateException();
      this.sum = sum;
      this.count = count;
      setRate(rate);
    } catch (Throwable e) {
      throw Util.throwException(e);
    } finally {
      weights.freeRef();
      delta.freeRef();
    }
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
    assert left.delta.size() == left.weights.size();
    assert right.delta.size() == right.weights.size();
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
    return PointSample.add(this.addRef(), right);
  }

  @Nonnull
  public PointSample addInPlace(@Nonnull final PointSample right) {
    try {
      assert delta.size() == weights.size();
      assert right.delta.size() == right.weights.size();
      assert rate == right.rate;
      delta.addInPlace(right.delta.addRef());
      return new PointSample(
          delta.addRef(),
          StateSet.union(weights.addRef(), right.weights.addRef()),
          sum + right.sum,
          rate,
          count + right.count);
    } finally {
      right.freeRef();
    }
  }

  @Nonnull
  public PointSample copyFull() {
    return new PointSample(
        delta.copy(),
        weights.copy(),
        sum, rate, count);
  }

  @Nonnull
  public PointSample normalize() {
    if (count == 1) {
      return this.addRef();
    } else {
      return new PointSample(
          delta.scale(1.0 / count),
          weights.addRef(),
          sum / count,
          rate,
          1);
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
    super._free();
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
