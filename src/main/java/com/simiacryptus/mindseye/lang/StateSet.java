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
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class StateSet<K> extends DoubleBufferSet<K, State<K>> {

  public StateSet() {
  }

  public StateSet(@Nonnull final DeltaSet<K> toCopy) {
    assert toCopy.stream().allMatch(x -> {
      boolean temp_41_0001 = RefArrays.stream(x.getDelta()).allMatch(Double::isFinite);
      x.freeRef();
      return temp_41_0001;
    });
    RefMap<K, Delta<K>> temp_41_0018 = toCopy.getMap();
    temp_41_0018.forEach((layer, layerDelta) -> {
      State<K> temp_41_0019 = this.get(layer, layerDelta.target);
      assert temp_41_0019 != null;
      RefUtil.freeRef(temp_41_0019.backup());
      temp_41_0019.freeRef();
      layerDelta.freeRef();
    });
    temp_41_0018.freeRef();
    toCopy.freeRef();
    assert stream().allMatch(x -> {
      boolean temp_41_0002 = RefArrays.stream(x.getDelta()).allMatch(Double::isFinite);
      x.freeRef();
      return temp_41_0002;
    });
    assert stream().allMatch(x -> {
      boolean temp_41_0003 = x instanceof State;
      if (null != x)
        x.freeRef();
      return temp_41_0003;
    });
  }

  public StateSet(@Nonnull final DoubleBufferSet<K, State<K>> toCopy) {
    super(toCopy);
    assert stream().allMatch(x -> {
      boolean temp_41_0004 = x instanceof State;
      if (null != x)
        x.freeRef();
      return temp_41_0004;
    });
  }

  public StateSet(@Nonnull final RefMap<K, State<K>> collect) {
    super(collect);
  }

  public boolean isDifferent() {
    return stream().parallel().anyMatch(x -> {
      boolean temp_41_0005 = !x.areEqual();
      x.freeRef();
      return temp_41_0005;
    });
  }

  @Nonnull
  public static <K> StateSet<K> union(@Nonnull final DoubleBufferSet<K, State<K>> left,
                                      @Nonnull final DoubleBufferSet<K, State<K>> right) {
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0020 = left.map.entrySet();
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0021 = right.map.entrySet();
    final RefMap<K, State<K>> collect = RefStream.concat(temp_41_0020.stream(), temp_41_0021.stream())
        .collect(RefCollectors.groupingBy((@Nonnull final Map.Entry<K, State<K>> e1) -> {
          K temp_41_0015 = e1.getKey();
          RefUtil.freeRef(e1);
          return temp_41_0015;
        }, RefCollectors.mapping((@Nonnull final Map.Entry<K, State<K>> x) -> {
          State<K> temp_41_0016 = x.getValue();
          RefUtil.freeRef(x);
          return temp_41_0016;
        }, RefCollectors
            .collectingAndThen(RefCollectors.reducing((@Nonnull final State<K> a, @Nonnull final State<K> b) -> {
              assert a.target == b.target;
              assert a.key.equals(b.key);
              b.freeRef();
              return a;
            }), x -> {
              State<K> temp_41_0006 = RefUtil.get(x);
              RefUtil.freeRef(x);
              return temp_41_0006;
            }))));
    temp_41_0021.freeRef();
    temp_41_0020.freeRef();
    right.freeRef();
    left.freeRef();
    StateSet<K> temp_41_0007 = new StateSet<K>(collect == null ? null : collect.addRef());
    if (null != collect)
      collect.freeRef();
    return temp_41_0007;
  }

  @Nullable
  public static @SuppressWarnings("unused")
  StateSet[] addRefs(@Nullable StateSet[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(StateSet::addRef).toArray((x) -> new StateSet[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  StateSet[][] addRefs(@Nullable StateSet[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(StateSet::addRefs).toArray((x) -> new StateSet[x][]);
  }

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
          RefUtil.freeRef(temp_41_0023.addInPlace(buffer.getDelta()));
          temp_41_0023.freeRef();
          buffer.freeRef();
        }, deltas.addRef()));
    right.freeRef();
    StateSet<K> temp_41_0008 = deltas.asState();
    deltas.freeRef();
    return temp_41_0008;
  }

  @Nonnull
  public DeltaSet<K> asVector() {
    @Nonnull final RefHashMap<K, Delta<K>> newMap = new RefHashMap<>();
    map.forEach(RefUtil.wrapInterface((BiConsumer<? super K, ? super State<K>>) (layer, state) -> {
      RefUtil.freeRef(newMap.put(layer,
          new Delta<K>(layer, state.target, RecycleBin.DOUBLES.copyOf(state.delta, state.delta.length))));
      state.freeRef();
    }, RefUtil.addRef(newMap)));
    DeltaSet<K> temp_41_0009 = new DeltaSet<>(RefUtil.addRef(RefUtil.addRef(newMap)));
    newMap.freeRef();
    return temp_41_0009;
  }

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

  @Nonnull
  public void restore() {
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0024 = map.entrySet();
    RefStream<Map.Entry<K, State<K>>> stream = temp_41_0024.stream();
    temp_41_0024.freeRef();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    stream.forEach(e -> {
      State<K> temp_41_0025 = e.getValue();
      RefUtil.freeRef(temp_41_0025.restore());
      temp_41_0025.freeRef();
      RefUtil.freeRef(e);
    });
  }

  @Nonnull
  @Override
  public StateSet<K> map(@Nonnull final Function<State<K>, State<K>> mapper) {
    RefHashSet<Map.Entry<K, State<K>>> temp_41_0026 = map.entrySet();
    RefStream<Map.Entry<K, State<K>>> stream = temp_41_0026.stream();
    temp_41_0026.freeRef();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    final RefMap<K, State<K>> newMap = stream.collect(RefCollectors.toMap(e -> {
      K temp_41_0011 = e.getKey();
      RefUtil.freeRef(e);
      return temp_41_0011;
    }, e -> {
      State<K> temp_41_0027 = e.getValue();
      State<K> temp_41_0012 = mapper.apply(temp_41_0027);
      if (null != temp_41_0027)
        temp_41_0027.freeRef();
      RefUtil.freeRef(e);
      return temp_41_0012;
    }));
    StateSet<K> temp_41_0013 = new StateSet<>(newMap == null ? null : newMap.addRef());
    if (null != newMap)
      newMap.freeRef();
    return temp_41_0013;
  }

  @Nonnull
  public StateSet<K> subtract(@Nonnull final DeltaSet<K> right) {
    StateSet<K> temp_41_0017 = this.add(right.scale(-1));
    right.freeRef();
    return temp_41_0017;
  }

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

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  StateSet<K> addRef() {
    return (StateSet<K>) super.addRef();
  }

  @Nonnull
  @Override
  protected State<K> factory(@Nonnull final K layer, final double[] target) {
    return new State<K>(layer, target);
  }
}
