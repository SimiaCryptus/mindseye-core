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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/**
 * The type Delta set.
 *
 * @param <K> the type parameter
 */
public class DeltaSet<K> extends DoubleBufferSet<K, Delta<K>> {

  /**
   * Instantiates a new Delta set.
   */
  public DeltaSet() {
  }

  /**
   * Instantiates a new Delta set.
   *
   * @param toCopy the to copy
   */
  public DeltaSet(@Nonnull final DoubleBufferSet<K, Delta<K>> toCopy) {
    super(toCopy);
//    assert stream().allMatch(x -> {
//      try {
//        return x instanceof Delta;
//      } finally {
//        if (null != x) x.freeRef();
//      }
//    });
  }

  /**
   * Instantiates a new Delta set.
   *
   * @param collect the collect
   */
  public DeltaSet(@Nonnull final RefMap<K, ? extends Delta<K>> collect) {
    super(collect);
//    assert stream().allMatch(x -> {
//      boolean temp_37_0002 = x instanceof Delta;
//      if (null != x)
//        x.freeRef();
//      return temp_37_0002;
//    });
  }

  /**
   * Gets magnitude.
   *
   * @return the magnitude
   */
  public double getMagnitude() {
    RefHashSet<Map.Entry<K, Delta<K>>> temp_37_0011 = map.entrySet();
    RefStream<Map.Entry<K, Delta<K>>> stream = temp_37_0011.stream();
    if (100 < map.size()) {
      stream = stream.parallel();
    }
    final double[] elementArray = stream.mapToDouble(entry -> {
      final DoubleBuffer<K> value = entry.getValue();
      RefUtil.freeRef(entry);
      double temp_37_0003 = value.deltaStatistics().sumSq();
      value.freeRef();
      return temp_37_0003;
    }).toArray();
    temp_37_0011.freeRef();
    return Math.sqrt(RefArrays.stream(elementArray).sum());
  }


  /**
   * Accumulate.
   *
   * @param alpha the alpha
   */
  public void accumulate(final double alpha) {
    stream().forEach((Delta<K> d) -> {
      d.accumulate(alpha);
      d.freeRef();
    });
  }

  /**
   * Add delta set.
   *
   * @param right the right
   * @return the delta set
   */
  public DeltaSet<K> add(@Nonnull final DeltaSet<K> right) {
    DeltaSet<K> copy = this.copy();
    copy.addInPlace(right);
    return copy;
  }

  /**
   * Add in place.
   *
   * @param right the right
   */
  public void addInPlace(@Nonnull DeltaSet<K> right) {
    right.map.forEach((layer, buffer) -> {
      Delta<K> temp_37_0013 = get(layer, buffer.target);
      assert temp_37_0013 != null;
      temp_37_0013.addInPlace(buffer);
      temp_37_0013.freeRef();
    });
    right.freeRef();
  }

  /**
   * As state state set.
   *
   * @return the state set
   */
  @Nonnull
  public StateSet<K> asState() {
    @Nonnull final StateSet<K> returnValue = new StateSet<>();
    map.forEach(RefUtil.wrapInterface((BiConsumer<? super K, ? super Delta<K>>) (layer, delta) -> {
      delta.assertAlive();
      State<K> kState = returnValue.get(layer, delta.target);
      assert kState != null;
      kState.set(delta.delta);
      kState.freeRef();
      delta.freeRef();
    }, returnValue.addRef()));
    return returnValue;
  }

  @Nonnull
  @Override
  public DeltaSet<K> copy() {
    return new DeltaSet(this.map(x -> {
      try {
        return x.copy();
      } finally {
        x.freeRef();
      }
    }));
  }

  /**
   * Dot double.
   *
   * @param right the right
   * @return the double
   */
  public double dot(@Nonnull final DoubleBufferSet<K, Delta<K>> right) {
    RefHashSet<Map.Entry<K, Delta<K>>> entries = map.entrySet();
    RefStream<Map.Entry<K, Delta<K>>> stream = entries.stream();
    if (100 < map.size()) {
      stream = stream.parallel();
    }
    double temp_37_0010 = stream
        .mapToDouble(RefUtil.wrapInterface((ToDoubleFunction<? super Map.Entry<K, Delta<K>>>) entry -> {
          final K key = entry.getKey();
          final Delta<K> value = entry.getValue();
          RefUtil.freeRef(entry);
          final Delta<K> rValue = right.map.get(key);
          if (null != rValue) {
            double temp_37_0005 = value.dot(rValue);
            if (null != value) value.freeRef();
            return temp_37_0005;
          } else {
            if (null != value) value.freeRef();
            return 0;
          }
        }, right)).sum();
    entries.freeRef();
    return temp_37_0010;
  }

  @Nonnull
  @Override
  public DeltaSet<K> map(@Nonnull final RefFunction<Delta<K>, Delta<K>> mapper) {
    return new DeltaSet<>(super.map(mapper));
  }

  /**
   * Scale delta set.
   *
   * @param f the f
   * @return the delta set
   */
  @Nonnull
  public DeltaSet<K> scale(final double f) {
    return map(x -> {
      Delta<K> temp_37_0007 = x.scale(f);
      x.freeRef();
      return temp_37_0007;
    });
  }

  /**
   * Subtract delta set.
   *
   * @param right the right
   * @return the delta set
   */
  @Nonnull
  public DeltaSet<K> subtract(@Nonnull final DeltaSet<K> right) {
    DeltaSet<K> scale = right.scale(-1);
    right.freeRef();
    return this.add(scale);
  }

  /**
   * Unit delta set.
   *
   * @return the delta set
   */
  @Nonnull
  public DeltaSet<K> unit() {
    return scale(1.0 / getMagnitude());
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  DeltaSet<K> addRef() {
    return (DeltaSet<K>) super.addRef();
  }

  public DeltaSet<K> allFinite(double defaultValue) {
    return map((Delta<K> delta) -> {
      Delta<K> map = delta.map(d -> Double.isFinite(d) ? d : defaultValue);
      delta.freeRef();
      return map;
    });
  }

  @Nonnull
  @Override
  protected Delta<K> factory(@Nonnull final K layer, final double[] target) {
    return new Delta<K>(layer, target);
  }

}
