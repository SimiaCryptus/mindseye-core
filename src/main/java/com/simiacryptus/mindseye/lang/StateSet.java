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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefHashMap;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefStream;

public @com.simiacryptus.ref.lang.RefAware class StateSet<K> extends DoubleBufferSet<K, State<K>> {

  public StateSet() {
  }

  public StateSet(@Nonnull final DeltaSet<K> toCopy) {
    assert toCopy.stream()
        .allMatch(x -> com.simiacryptus.ref.wrappers.RefArrays.stream(x.getDelta()).allMatch(Double::isFinite));
    toCopy.getMap().forEach((layer, layerDelta) -> {
      this.get(layer, layerDelta.target).backup();
    });
    assert stream()
        .allMatch(x -> com.simiacryptus.ref.wrappers.RefArrays.stream(x.getDelta()).allMatch(Double::isFinite));
    assert stream().allMatch(x -> x instanceof State);
  }

  public StateSet(@Nonnull final DoubleBufferSet<K, State<K>> toCopy) {
    super(toCopy);
    assert stream().allMatch(x -> x instanceof State);
  }

  public StateSet(@Nonnull final com.simiacryptus.ref.wrappers.RefMap<K, State<K>> collect) {
    super(collect);
  }

  public boolean isDifferent() {
    return stream().parallel().anyMatch(x -> !x.areEqual());
  }

  public static <K> StateSet<K> union(@Nonnull final DoubleBufferSet<K, State<K>> left,
      @Nonnull final DoubleBufferSet<K, State<K>> right) {
    final com.simiacryptus.ref.wrappers.RefMap<K, State<K>> collect = com.simiacryptus.ref.wrappers.RefStream
        .concat(left.map.entrySet().stream(), right.map.entrySet().stream())
        .collect(com.simiacryptus.ref.wrappers.RefCollectors.groupingBy(
            (@Nonnull final Map.Entry<K, State<K>> e1) -> e1.getKey(),
            com.simiacryptus.ref.wrappers.RefCollectors.mapping(
                (@Nonnull final Map.Entry<K, State<K>> x) -> x.getValue(),
                com.simiacryptus.ref.wrappers.RefCollectors
                    .collectingAndThen(com.simiacryptus.ref.wrappers.RefCollectors
                        .reducing((@Nonnull final State<K> a, @Nonnull final State<K> b) -> {
                          assert a.target == b.target;
                          assert a.key.equals(b.key);
                          return a;
                        }), x -> x.get()))));
    return new StateSet<K>(collect);
  }

  @Nonnull
  public StateSet<K> add(@Nonnull final DeltaSet<K> right) {
    @Nonnull
    final DeltaSet<K> deltas = new DeltaSet<K>();
    map.forEach((@Nonnull final K layer, @Nonnull final State<K> buffer) -> {
      deltas.get(layer, buffer.target).set(buffer.getDelta());
    });
    right.map.forEach((@Nonnull final K layer, @Nonnull final Delta<K> buffer) -> {
      deltas.get(layer, buffer.target).addInPlace(buffer.getDelta());
    });
    return deltas.asState();
  }

  @Nonnull
  public DeltaSet<K> asVector() {
    @Nonnull
    final com.simiacryptus.ref.wrappers.RefHashMap<K, Delta<K>> newMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    map.forEach((layer, state) -> newMap.put(layer,
        new Delta<K>(layer, state.target, RecycleBin.DOUBLES.copyOf(state.delta, state.delta.length))));
    return new DeltaSet<>(newMap);
  }

  @Nonnull
  @Override
  public StateSet<K> copy() {
    return map(x -> x.copy());
  }

  @Nonnull
  public void restore() {
    com.simiacryptus.ref.wrappers.RefStream<Map.Entry<K, State<K>>> stream = map.entrySet().stream();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    stream.forEach(e -> e.getValue().restore());
  }

  @Nonnull
  @Override
  public StateSet<K> map(@NotNull @Nonnull final Function<State<K>, State<K>> mapper) {
    com.simiacryptus.ref.wrappers.RefStream<Map.Entry<K, State<K>>> stream = map.entrySet().stream();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    final com.simiacryptus.ref.wrappers.RefMap<K, State<K>> newMap = stream
        .collect(com.simiacryptus.ref.wrappers.RefCollectors.toMap(e -> e.getKey(), e -> mapper.apply(e.getValue())));
    return new StateSet<>(newMap);
  }

  @Nonnull
  public StateSet<K> subtract(@Nonnull final DeltaSet<K> right) {
    return this.add(right.scale(-1));
  }

  @Nonnull
  public DeltaSet<K> subtract(@Nonnull final StateSet<K> right) {
    @Nonnull
    DeltaSet<K> rvec = right.asVector();
    @Nonnull
    DeltaSet<K> scale = rvec.scale(-1);
    @Nonnull
    StateSet<K> add = this.add(scale);
    return add.asVector();
  }

  //  /**
  //   * Union evalInputDelta setByCoord.
  //   *
  //   * @param right the right
  //   * @return the evalInputDelta setByCoord
  //   */
  //  @Nonnull
  //  public DoubleBufferSet<K, State<K>> union(@Nonnull final DoubleBufferSet<K, State<K>> right) {
  //    return StateSet.union(this, right);
  //  }

  @Nonnull
  @Override
  protected State<K> factory(@Nonnull final K layer, final double[] target) {
    return new State<K>(layer, target);
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") StateSet<K> addRef() {
    return (StateSet<K>) super.addRef();
  }

  public static @SuppressWarnings("unused") StateSet[] addRefs(StateSet[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(StateSet::addRef)
        .toArray((x) -> new StateSet[x]);
  }

  public static @SuppressWarnings("unused") StateSet[][] addRefs(StateSet[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(StateSet::addRefs)
        .toArray((x) -> new StateSet[x][]);
  }
}
