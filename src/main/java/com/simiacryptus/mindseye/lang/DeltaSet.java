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

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefStream;

public @com.simiacryptus.ref.lang.RefAware class DeltaSet<K> extends DoubleBufferSet<K, Delta<K>> {

  public DeltaSet() {
  }

  public DeltaSet(@Nonnull final DoubleBufferSet<K, Delta<K>> toCopy) {
    super(toCopy);
    assert stream().allMatch(x -> x instanceof Delta);
  }

  public DeltaSet(@Nonnull final com.simiacryptus.ref.wrappers.RefMap<K, ? extends Delta<K>> collect) {
    super(collect);
    assert stream().allMatch(x -> x instanceof Delta);
  }

  public double getMagnitude() {
    com.simiacryptus.ref.wrappers.RefStream<Map.Entry<K, Delta<K>>> stream = map.entrySet().stream();
    if (100 < map.size()) {
      stream = stream.parallel();
    }
    final double[] elementArray = stream.mapToDouble(entry -> {
      final DoubleBuffer<K> value = entry.getValue();
      return value.deltaStatistics().sumSq();
    }).toArray();
    return Math.sqrt(com.simiacryptus.ref.wrappers.RefArrays.stream(elementArray).sum());
  }

  @Nonnull
  public void accumulate(final double alpha) {
    stream().forEach(d -> d.accumulate(alpha));
  }

  @Nonnull
  public DeltaSet<K> add(@Nonnull final DeltaSet<K> right) {
    return this.copy().addInPlace(right);
  }

  @Nonnull
  public DeltaSet<K> addInPlace(@Nonnull final DeltaSet<K> right) {
    right.map.forEach((layer, buffer) -> {
      get(layer, buffer.target).addInPlace(buffer);
    });
    return this;
  }

  @Nonnull
  public StateSet<K> asState() {
    @Nonnull
    final StateSet<K> returnValue = new StateSet<>();
    map.forEach((layer, delta) -> {
      delta.assertAlive();
      State<K> kState = returnValue.get(layer, delta.target);
      kState.set(delta.delta);
    });
    return returnValue;
  }

  @Nonnull
  @Override
  public DeltaSet<K> copy() {
    return this.map(x -> x.copy());
  }

  public double dot(@Nonnull final DoubleBufferSet<K, Delta<K>> right) {
    com.simiacryptus.ref.wrappers.RefStream<Map.Entry<K, Delta<K>>> stream = map.entrySet().stream();
    if (100 < map.size()) {
      stream = stream.parallel();
    }
    return stream.mapToDouble(entry -> {
      final K key = entry.getKey();
      final Delta<K> value = entry.getValue();
      final Delta<K> rValue = right.map.get(key);
      if (null != rValue) {
        return value.dot(rValue);
      } else {
        return 0;
      }
    }).sum();
  }

  @Nonnull
  @Override
  public DeltaSet<K> map(@NotNull final Function<Delta<K>, Delta<K>> mapper) {
    @Nonnull
    DoubleBufferSet<K, Delta<K>> map = super.map(mapper);
    return new DeltaSet<>(map);
  }

  @Nonnull
  public DeltaSet<K> scale(final double f) {
    return map(x -> x.scale(f));
  }

  @Nonnull
  public DeltaSet<K> subtract(@Nonnull final DeltaSet<K> right) {
    DeltaSet<K> scale = right.scale(-1);
    return this.add(scale);
  }

  @Nonnull
  public DeltaSet<K> unit() {
    return scale(1.0 / getMagnitude());
  }

  @Nonnull
  @Override
  protected Delta<K> factory(@Nonnull final K layer, final double[] target) {
    return new Delta<K>(layer, target);
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") DeltaSet<K> addRef() {
    return (DeltaSet<K>) super.addRef();
  }

  public static @SuppressWarnings("unused") DeltaSet[] addRefs(DeltaSet[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeltaSet::addRef)
        .toArray((x) -> new DeltaSet[x]);
  }

  public static @SuppressWarnings("unused") DeltaSet[][] addRefs(DeltaSet[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeltaSet::addRefs)
        .toArray((x) -> new DeltaSet[x][]);
  }

}
