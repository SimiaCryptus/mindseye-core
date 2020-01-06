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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefArrays;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

@SuppressWarnings({"rawtypes", "unchecked"})
public @RefAware
class State<K> extends DoubleBuffer<K> {

  public State(@Nonnull final K layer, final double[] target) {
    super(layer, target);
  }

  public State(@Nonnull final K layer, final double[] target, final double[] delta) {
    super(layer, target, delta);
  }

  public static @SuppressWarnings("unused")
  State[] addRefs(State[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(State::addRef).toArray((x) -> new State[x]);
  }

  public static @SuppressWarnings("unused")
  State[][] addRefs(State[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(State::addRefs).toArray((x) -> new State[x][]);
  }

  public boolean areEqual() {
    return areEqual(getDelta(), target);
  }

  @Nonnull
  public final synchronized State<K> backup() {
    com.simiacryptus.ref.wrappers.RefSystem.arraycopy(target, 0, getDelta(), 0, target.length);
    return this.addRef();
  }

  @Nonnull
  @Override
  public State<K> copy() {
    assertAlive();
    return new State(key, target, RecycleBin.DOUBLES.copyOf(delta, length()));
  }

  @Nonnull
  @Override
  public State<K> map(@Nonnull final DoubleUnaryOperator mapper) {
    return new State(key, target, RefArrays.stream(getDelta()).map(x -> mapper.applyAsDouble(x)).toArray());
  }

  @Nonnull
  public final synchronized State<K> restore() {
    com.simiacryptus.ref.wrappers.RefSystem.arraycopy(getDelta(), 0, target, 0, target.length);
    return this.addRef();
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  State<K> addRef() {
    return (State<K>) super.addRef();
  }

}
