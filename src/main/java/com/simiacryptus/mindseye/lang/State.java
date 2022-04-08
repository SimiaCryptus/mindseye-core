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

import com.simiacryptus.ref.lang.RecycleBin;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefSystem;

import javax.annotation.Nonnull;
import java.util.function.DoubleUnaryOperator;

/**
 * This class represents a state.
 *
 * @docgenVersion 9
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class State<K> extends DoubleBuffer<K> {

  /**
   * Instantiates a new State.
   *
   * @param layer  the layer
   * @param target the target
   */
  public State(@Nonnull final K layer, final double[] target) {
    super(layer, target);
  }

  /**
   * Instantiates a new State.
   *
   * @param layer  the layer
   * @param target the target
   * @param delta  the delta
   */
  public State(@Nonnull final K layer, final double[] target, final double[] delta) {
    super(layer, target, delta);
  }


  /**
   * Returns true if the current object is equal to the specified object,
   * taking into account a delta value.
   *
   * @docgenVersion 9
   */
  public boolean areEqual() {
    return areEqual(getDelta(), target);
  }

  /**
   * This method backs up the target array by copying it into the delta array.
   * It is synchronized to ensure that the backup operation is atomic.
   *
   * @docgenVersion 9
   */
  public synchronized void backup() {
    RefSystem.arraycopy(target, 0, getDelta(), 0, target.length);
  }

  /**
   * @Nonnull
   * @Override public State<K> copy() {
   * assertAlive();
   * return new State(key, target, RecycleBin.DOUBLES.copyOf(delta, length()));
   * }
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public State<K> copy() {
    assertAlive();
    return new State(key, target, RecycleBin.DOUBLES.copyOf(delta, length()));
  }

  /**
   * Maps the delta of this State using the given mapper function.
   *
   * @param mapper   the function to use to map the delta
   * @param parallel whether or not to run the mapping in parallel
   * @return a new State with the mapped delta
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public State<K> map(@Nonnull final DoubleUnaryOperator mapper, boolean parallel) {
    return new State(key, target, RefArrays.stream(getDelta()).map(mapper::applyAsDouble).toArray());
  }

  /**
   * Restores the target array by copying the delta array into it.
   * This method is synchronized to prevent concurrent modifications.
   *
   * @docgenVersion 9
   */
  public synchronized void restore() {
    RefSystem.arraycopy(getDelta(), 0, target, 0, target.length);
  }

  /**
   * This method is unused.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  /**
   * @return the state after adding a reference
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  State<K> addRef() {
    return (State<K>) super.addRef();
  }

}
