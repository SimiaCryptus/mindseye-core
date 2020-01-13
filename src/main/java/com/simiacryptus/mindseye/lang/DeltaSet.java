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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefStream;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class DeltaSet<K> extends DoubleBufferSet<K, Delta<K>> {

  public DeltaSet() {
  }

  public DeltaSet(@Nonnull final DoubleBufferSet<K, Delta<K>> toCopy) {
    super(toCopy);
    assert stream().allMatch(x -> {
      boolean temp_37_0001 = x instanceof Delta;
      if (null != x)
        x.freeRef();
      return temp_37_0001;
    });
  }

  public DeltaSet(@Nonnull final RefMap<K, ? extends Delta<K>> collect) {
    super(collect);
    assert stream().allMatch(x -> {
      boolean temp_37_0002 = x instanceof Delta;
      if (null != x)
        x.freeRef();
      return temp_37_0002;
    });
  }

  public double getMagnitude() {
    RefHashSet<Map.Entry<K, Delta<K>>> temp_37_0011 = map.entrySet();
    RefStream<Map.Entry<K, Delta<K>>> stream = temp_37_0011.stream();
    if (null != temp_37_0011)
      temp_37_0011.freeRef();
    if (100 < map.size()) {
      stream = stream.parallel();
    }
    final double[] elementArray = stream.mapToDouble(entry -> {
      final DoubleBuffer<K> value = entry.getValue();
      if (null != entry)
        RefUtil.freeRef(entry);
      double temp_37_0003 = value.deltaStatistics().sumSq();
      if (null != value)
        value.freeRef();
      return temp_37_0003;
    }).toArray();
    return Math.sqrt(RefArrays.stream(elementArray).sum());
  }

  public static @SuppressWarnings("unused") DeltaSet[] addRefs(DeltaSet[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DeltaSet::addRef).toArray((x) -> new DeltaSet[x]);
  }

  public static @SuppressWarnings("unused") DeltaSet[][] addRefs(DeltaSet[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DeltaSet::addRefs).toArray((x) -> new DeltaSet[x][]);
  }

  @Nonnull
  public void accumulate(final double alpha) {
    stream().forEach(d -> {
      d.accumulate(alpha);
      if (null != d)
        d.freeRef();
    });
  }

  @Nonnull
  public DeltaSet<K> add(@Nonnull final DeltaSet<K> right) {
    DeltaSet<K> temp_37_0012 = this.copy();
    DeltaSet<K> temp_37_0009 = temp_37_0012.addInPlace(right == null ? null : right);
    if (null != temp_37_0012)
      temp_37_0012.freeRef();
    return temp_37_0009;
  }

  @Nonnull
  public DeltaSet<K> addInPlace(@Nonnull final DeltaSet<K> right) {
    right.map.forEach((layer, buffer) -> {
      Delta<K> temp_37_0013 = get(layer, buffer.target);
      temp_37_0013.addInPlace(buffer == null ? null : buffer.addRef());
      if (null != temp_37_0013)
        temp_37_0013.freeRef();
      if (null != buffer)
        buffer.freeRef();
    });
    right.freeRef();
    return this.addRef();
  }

  @Nonnull
  public StateSet<K> asState() {
    @Nonnull
    final StateSet<K> returnValue = new StateSet<>();
    map.forEach(RefUtil.wrapInterface((BiConsumer<? super K, ? super Delta<K>>) (layer, delta) -> {
      delta.assertAlive();
      State<K> kState = returnValue.get(layer, delta.target);
      kState.set(delta.delta);
      if (null != kState)
        kState.freeRef();
      if (null != delta)
        delta.freeRef();
    }, returnValue == null ? null : returnValue.addRef()));
    return returnValue;
  }

  @Nonnull
  @Override
  public DeltaSet<K> copy() {
    return this.map(x -> {
      Delta<K> temp_37_0004 = x.copy();
      if (null != x)
        x.freeRef();
      return temp_37_0004;
    });
  }

  public double dot(@Nonnull final DoubleBufferSet<K, Delta<K>> right) {
    RefHashSet<Map.Entry<K, Delta<K>>> temp_37_0014 = map.entrySet();
    RefStream<Map.Entry<K, Delta<K>>> stream = temp_37_0014.stream();
    if (null != temp_37_0014)
      temp_37_0014.freeRef();
    if (100 < map.size()) {
      stream = stream.parallel();
    }
    double temp_37_0010 = stream
        .mapToDouble(RefUtil.wrapInterface((ToDoubleFunction<? super Map.Entry<K, Delta<K>>>) entry -> {
          final K key = entry.getKey();
          final Delta<K> value = entry.getValue();
          if (null != entry)
            RefUtil.freeRef(entry);
          final Delta<K> rValue = right.map.get(key);
          if (null != rValue) {
            double temp_37_0005 = value.dot(rValue == null ? null : rValue.addRef());
            if (null != value)
              value.freeRef();
            if (null != rValue)
              rValue.freeRef();
            return temp_37_0005;
          } else {
            if (null != value)
              value.freeRef();
            if (null != rValue)
              rValue.freeRef();
            return 0;
          }
        }, right == null ? null : right)).sum();
    return temp_37_0010;
  }

  @Nonnull
  @Override
  public DeltaSet<K> map(@NotNull final Function<Delta<K>, Delta<K>> mapper) {
    @Nonnull
    DoubleBufferSet<K, Delta<K>> map = super.map(mapper).addRef();
    DeltaSet<K> temp_37_0006 = new DeltaSet<>(map == null ? null : map);
    return temp_37_0006;
  }

  @Nonnull
  public DeltaSet<K> scale(final double f) {
    return map(x -> {
      Delta<K> temp_37_0007 = x.scale(f);
      if (null != x)
        x.freeRef();
      return temp_37_0007;
    });
  }

  @Nonnull
  public DeltaSet<K> subtract(@Nonnull final DeltaSet<K> right) {
    DeltaSet<K> scale = right.scale(-1);
    right.freeRef();
    DeltaSet<K> temp_37_0008 = this.add(scale == null ? null : scale.addRef());
    if (null != scale)
      scale.freeRef();
    return temp_37_0008;
  }

  @Nonnull
  public DeltaSet<K> unit() {
    return scale(1.0 / getMagnitude());
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") DeltaSet<K> addRef() {
    return (DeltaSet<K>) super.addRef();
  }

  @Nonnull
  @Override
  protected Delta<K> factory(@Nonnull final K layer, final double[] target) {
    return new Delta<K>(layer, target);
  }

}
