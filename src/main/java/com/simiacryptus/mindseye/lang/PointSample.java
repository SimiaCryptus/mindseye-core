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
      assert delta.getMap().size() == weights.getMap().size();
      this.delta = new DeltaSet<>(delta);
      this.weights = new StateSet<>(weights);
      assert delta.getMap().keySet().stream().allMatch(x -> weights.getMap().containsKey(x));
      this.sum = sum;
      this.count = count;
      setRate(rate);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
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
    return this;
  }

  public static PointSample add(@Nonnull final PointSample left, @Nonnull final PointSample right) {
    assert left.delta.getMap().size() == left.weights.getMap().size();
    assert right.delta.getMap().size() == right.weights.getMap().size();
    assert left.rate == right.rate;
    DeltaSet<UUID> delta = left.delta.add(right.delta);
    StateSet<UUID> stateSet = StateSet.union(left.weights, right.weights);
    return new PointSample(delta, stateSet, left.sum + right.sum, left.rate, left.count + right.count);
  }

  public PointSample add(@Nonnull final PointSample right) {
    return PointSample.add(this, right);
  }

  public PointSample addInPlace(@Nonnull final PointSample right) {
    assert delta.getMap().size() == weights.getMap().size();
    assert right.delta.getMap().size() == right.weights.getMap().size();
    assert rate == right.rate;
    return new PointSample(delta.addInPlace(right.delta), StateSet.union(weights, right.weights), sum + right.sum, rate,
        count + right.count);
  }

  @Nonnull
  public PointSample copyFull() {
    @Nonnull
    DeltaSet<UUID> deltaCopy = delta.copy();
    @Nonnull
    StateSet<UUID> weightsCopy = weights.copy();
    return new PointSample(deltaCopy, weightsCopy, sum, rate, count);
  }

  @Nonnull
  public PointSample normalize() {
    if (count == 1) {
      return this;
    } else {
      @Nonnull
      DeltaSet<UUID> scale = delta.scale(1.0 / count);
      return new PointSample(scale, weights, sum / count, rate, 1);
    }
  }

  @Nonnull
  public PointSample restore() {
    weights.stream().forEach(d -> d.restore());
    return this;
  }

  @Nonnull
  public PointSample backup() {
    weights.stream().forEach(d -> d.backup());
    return this;
  }

  @Override
  public String toString() {
    @Nonnull final StringBuffer sb = new StringBuffer("PointSample{");
    sb.append("avg=").append(getMean());
    sb.append('}');
    return sb.toString();
  }

  @Override
  public PointSample addRef() {
    return (PointSample) super.addRef();
  }

  @Override
  protected void _free() {
  }
}
