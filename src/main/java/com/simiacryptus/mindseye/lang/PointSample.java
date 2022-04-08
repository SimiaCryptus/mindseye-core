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

/**
 * This class represents a PointSample.
 *
 * @param count   the number of points
 * @param delta   the set of deltas
 * @param sum     the sum of the points
 * @param weights the set of weights
 * @param rate    the rate
 * @docgenVersion 9
 */
public final class PointSample extends ReferenceCountingBase {
  /**
   * The Count.
   */
  public final int count;
  /**
   * The Delta.
   */
  @Nonnull
  public final DeltaSet<UUID> delta;
  /**
   * The Sum.
   */
  public final double sum;
  /**
   * The Weights.
   */
  @Nonnull
  public final StateSet<UUID> weights;
  /**
   * The Rate.
   */
  public double rate;

  /**
   * Instantiates a new Point sample.
   *
   * @param delta   the delta
   * @param weights the weights
   * @param sum     the sum
   * @param rate    the rate
   * @param count   the count
   */
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

  /**
   * Returns the mean of the values in the stream.
   *
   * @docgenVersion 9
   */
  public double getMean() {
    return sum / count;
  }

  /**
   * Returns the rate.
   *
   * @docgenVersion 9
   */
  public double getRate() {
    return rate;
  }

  /**
   * Sets the rate.
   *
   * @param rate the new rate
   * @docgenVersion 9
   */
  public void setRate(double rate) {
    this.rate = rate;
  }

  /**
   * Adds two PointSamples together.
   *
   * @param left  the first PointSample to add
   * @param right the second PointSample to add
   * @return the sum of the two PointSamples
   * @docgenVersion 9
   */
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


  /**
   * Adds the given [right] PointSample to this one, returning the result as a new PointSample.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public PointSample add(@Nonnull final PointSample right) {
    return PointSample.add(this.addRef(), right);
  }

  /**
   * Adds the given PointSample to this one, modifying this instance in-place and returning it.
   *
   * @param right the PointSample to add to this one
   * @return this instance, for chaining
   * @docgenVersion 9
   */
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

  /**
   * Returns a new PointSample with a deep copy of the delta, weights, sum, rate, and count fields.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public PointSample copyFull() {
    return new PointSample(
        delta.copy(),
        weights.copy(),
        sum, rate, count);
  }

  /**
   * Normalizes this PointSample.
   *
   * @return a normalized PointSample
   * @docgenVersion 9
   */
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

  /**
   * Restores the weights.
   *
   * @docgenVersion 9
   */
  public void restore() {
    weights.stream().forEach(d -> {
      d.restore();
      d.freeRef();
    });
  }

  /**
   * This method backs up the weights of the neural network.
   *
   * @docgenVersion 9
   */
  public void backup() {
    weights.stream().forEach(d -> {
      d.backup();
      d.freeRef();
    });
  }

  /**
   * Returns a string representation of this PointSample.
   *
   * @return a string representation of this PointSample.
   * @docgenVersion 9
   */
  @Override
  public String toString() {
    @Nonnull final RefStringBuilder sb = new RefStringBuilder(
        "PointSample{");
    sb.append("avg=").append(getMean());
    sb.append('}');
    return sb.toString();
  }

  /**
   * Frees this object's resources.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    weights.freeRef();
    delta.freeRef();
  }

  /**
   * Adds a reference to this PointSample.
   *
   * @return a reference to this PointSample
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  PointSample addRef() {
    return (PointSample) super.addRef();
  }
}
