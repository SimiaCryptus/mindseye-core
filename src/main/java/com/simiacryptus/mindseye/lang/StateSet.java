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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Union two StateSets.
 *
 * @param right the right StateSet
 * @return the resulting StateSet
 * @docgenVersion 9
 */
public class StateSet<K> extends DoubleBufferSet<K, State<K>> {

  /**
   * Instantiates a new State set.
   */
  public StateSet() {
  }

  /**
   * Instantiates a new State set.
   *
   * @param toCopy the to copy
   */
  public StateSet(@Nonnull final DeltaSet<K> toCopy) {
//    assert toCopy.stream().allMatch(x -> {
//      boolean temp_41_0001 = RefArrays.stream(x.getDelta()).allMatch(Double::isFinite);
//      x.freeRef();
//      return temp_41_0001;
//    });
    RefMap<K, Delta<K>> temp_41_0018 = toCopy.getMap();
    temp_41_0018.forEach((layer, layerDelta) -> {
      State<K> state = get(layer, layerDelta.target);
      assert state != null;
      state.backup();
      RefUtil.freeRef(state);
      layerDelta.freeRef();
    });
    temp_41_0018.freeRef();
    toCopy.freeRef();
//    assert stream().allMatch(x -> {
//      boolean temp_41_0002 = Arrays.stream(x.getDelta()).allMatch(Double::isFinite);
//      x.freeRef();
//      return temp_41_0002;
//    });
//    assert stream().allMatch(x -> {
//      boolean temp_41_0003 = x instanceof State;
//      if (null != x)
//        x.freeRef();
//      return temp_41_0003;
//    });
  }

  /**
   * Instantiates a new State set.
   *
   * @param toCopy the to copy
   */
  public StateSet(@Nonnull final DoubleBufferSet<K, State<K>> toCopy) {
    super(toCopy);
//    assert stream().allMatch(x -> {
//      boolean temp_41_0004 = x instanceof State;
//      if (null != x)
//        x.freeRef();
//      return temp_41_0004;
//    });
  }

  /**
   * Instantiates a new State set.
   *
   * @param collect the collect
   */
  public StateSet(@Nonnull final RefMap<K, State<K>> collect) {
    super(collect);
  }

  /**
   * Returns true if the two objects are different, false otherwise.
   *
   * @docgenVersion 9
   */
  public boolean isDifferent() {
    return stream().parallel().anyMatch(x -> {
      boolean temp_41_0005 = !x.areEqual();
      x.freeRef();
      return temp_41_0005;
    });
  }

  /**
   * Returns a new StateSet that is the union of this StateSet and the other StateSet.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static <K> StateSet<K> union(@Nonnull final DoubleBufferSet<K, State<K>> left,
                                      @Nonnull final DoubleBufferSet<K, State<K>> right) {
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0020 = left.map.entrySet();
    left.freeRef();
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0021 = right.map.entrySet();
    right.freeRef();
    final RefMap<K, State<K>> collect = RefStream.concat(temp_41_0020.stream(), temp_41_0021.stream())
        .collect(RefCollectors.groupingBy((final Map.Entry<K, State<K>> entry) -> {
          K key = entry.getKey();
          RefUtil.freeRef(entry);
          return key;
        }, RefCollectors.mapping(
            (final Map.Entry<K, State<K>> entry) -> {
              State<K> value = entry.getValue();
              RefUtil.freeRef(entry);
              return value;
            },
            RefCollectors.collectingAndThen(
                RefCollectors.reducing((@Nonnull final State<K> a, @Nonnull final State<K> b) -> {
                  assert a.target == b.target;
                  assert a.key.equals(b.key);
                  b.freeRef();
                  return a;
                }),
                optional -> RefUtil.get(optional)))));
    temp_41_0021.freeRef();
    temp_41_0020.freeRef();
    return new StateSet<K>(collect);
  }

  /**
   * Checks if this set contains all of the elements in the specified collection.
   *
   * @docgenVersion 9
   */
  public boolean containsAll(RefMap<K, Delta<K>> deltaMap) {
    RefSet<K> keySet = deltaMap.keySet();
    RefMap<K, State<K>> weightsMap = getMap();
    try {
      return keySet.stream().allMatch(x -> weightsMap.containsKey(x));
    } finally {
      weightsMap.freeRef();
      keySet.freeRef();
      deltaMap.freeRef();
    }
  }


  /**
   * Adds a new state to the set.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public StateSet<K> add(@Nonnull final DeltaSet<K> right) {
    @Nonnull final DeltaSet<K> deltas = new DeltaSet<K>();
    map.forEach(
        RefUtil.wrapInterface((BiConsumer<K, State<K>>) (@Nonnull final K layer, @Nonnull final State<K> buffer) -> {
          Delta<K> temp_41_0022 = deltas.get(layer, buffer.target);
          assert temp_41_0022 != null;
          temp_41_0022.set(buffer.getDelta());
          temp_41_0022.freeRef();
          buffer.freeRef();
        }, deltas.addRef()));
    right.map.forEach(
        RefUtil.wrapInterface((BiConsumer<K, Delta<K>>) (@Nonnull final K layer, @Nonnull final Delta<K> buffer) -> {
          Delta<K> temp_41_0023 = deltas.get(layer, buffer.target);
          assert temp_41_0023 != null;
          temp_41_0023.addInPlace(buffer.getDelta());
          temp_41_0023.freeRef();
          buffer.freeRef();
        }, deltas.addRef()));
    right.freeRef();
    StateSet<K> temp_41_0008 = deltas.asState();
    deltas.freeRef();
    return temp_41_0008;
  }

  /**
   * Returns a DeltaSet as a Vector.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public DeltaSet<K> asVector() {
    @Nonnull final RefHashMap<K, Delta<K>> newMap = new RefHashMap<>();
    map.forEach(RefUtil.wrapInterface((BiConsumer<? super K, ? super State<K>>) (layer, state) -> {
      RefUtil.freeRef(newMap.put(RefUtil.addRef(layer),
          new Delta<K>(layer, state.target, RecycleBin.DOUBLES.copyOf(state.delta, state.delta.length))));
      state.freeRef();
    }, RefUtil.addRef(newMap)));
    return new DeltaSet<>(newMap);
  }

  /**
   * Returns a copy of this StateSet.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public StateSet<K> copy() {
    return map(x -> {
      State<K> temp_41_0010 = x.copy();
      x.freeRef();
      return temp_41_0010;
    });
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

  /**
   * Restores the state of the object.
   *
   * @docgenVersion 9
   */
  public void restore() {
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0024 = map.entrySet();
    RefStream<Map.Entry<K, State<K>>> stream = temp_41_0024.stream();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    stream.forEach(e -> {
      State<K> temp_41_0025 = e.getValue();
      temp_41_0025.restore();
      temp_41_0025.freeRef();
      RefUtil.freeRef(e);
    });
    temp_41_0024.freeRef();
  }

  /**
   * Returns a new StateSet[K] with the results of mapping the given function
   * over the elements of this StateSet[K].
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public StateSet<K> map(@Nonnull final RefFunction<State<K>, State<K>> mapper) {
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0026 = map.entrySet();
    RefStream<Map.Entry<K, State<K>>> stream = temp_41_0026.stream();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    final RefMap<K, State<K>> newMap = stream.collect(RefCollectors.toMap(e -> {
      K temp_41_0011 = e.getKey();
      RefUtil.freeRef(e);
      return temp_41_0011;
    }, e -> {
      State<K> temp_41_0012 = mapper.apply(e.getValue());
      RefUtil.freeRef(e);
      return temp_41_0012;
    }));
    temp_41_0026.freeRef();
    return new StateSet<>(newMap);
  }

  /**
   * Removes all elements from this set.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public StateSet<K> subtract(@Nonnull final DeltaSet<K> right) {
    StateSet<K> temp_41_0017 = this.add(right.scale(-1));
    right.freeRef();
    return temp_41_0017;
  }

  /**
   * Subtracts the delta set from the original set.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public DeltaSet<K> subtract(@Nonnull final StateSet<K> right) {
    @Nonnull
    DeltaSet<K> rvec = right.asVector();
    right.freeRef();
    @Nonnull
    DeltaSet<K> scale = rvec.scale(-1);
    rvec.freeRef();
    @Nonnull
    StateSet<K> add = this.add(scale);
    DeltaSet<K> temp_41_0014 = add.asVector();
    add.freeRef();
    return temp_41_0014;
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  /**
   * Adds a reference to the StateSet.
   *
   * @return the StateSet
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  StateSet<K> addRef() {
    return (StateSet<K>) super.addRef();
  }

  /**
   * Returns a new State object.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  protected State<K> factory(@Nonnull final K layer, final double[] target) {
    return new State<K>(layer, target);
  }
}
