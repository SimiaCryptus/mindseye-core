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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract @RefAware
class DoubleBufferSet<K, T extends DoubleBuffer<K>>
    extends ReferenceCountingBase {
  static final Logger log = LoggerFactory.getLogger(DoubleBufferSet.class);

  @Nonnull
  protected final RefHashMap<K, T> map = new RefHashMap<>();

  public DoubleBufferSet() {
  }

  public DoubleBufferSet(@Nonnull final DoubleBufferSet<K, T> toCopy) {
    this(toCopy.map);
  }

  public DoubleBufferSet(@Nonnull final RefMap<K, ? extends T> collect) {
    collect.forEach((k, v) -> {
      assert null != k;
      assert null != v;
    });
    synchronized (collect) {
      map.putAll(collect);
      map.forEach((k, v) -> {
      });
    }
  }

  @Nonnull
  public RefMap<K, T> getMap() {
    return RefCollections.unmodifiableMap(map);
  }

  public static @SuppressWarnings("unused")
  DoubleBufferSet[] addRefs(DoubleBufferSet[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DoubleBufferSet::addRef)
        .toArray((x) -> new DoubleBufferSet[x]);
  }

  public static @SuppressWarnings("unused")
  DoubleBufferSet[][] addRefs(DoubleBufferSet[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DoubleBufferSet::addRefs)
        .toArray((x) -> new DoubleBufferSet[x][]);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public DoubleBufferSet<K, T> copy() {
    return map(x -> (T) x.copy());
  }

  public T get(final K layer, final double[] ptr) {
    final T delta = get(layer, () -> factory(layer, ptr));
    assert delta.key.equals(layer);
    assert delta.target == ptr;
    return delta;
  }

  public T get(final K layer, @Nonnull final Tensor ptr) {
    return get(layer, ptr.getData());
  }

  @Nonnull
  public DoubleBufferSet<K, T> map(@Nonnull final Function<T, T> mapper) {
    @Nonnull final DoubleBufferSet<K, T> parent = this;
    RefStream<Map.Entry<K, T>> stream = map.entrySet().stream();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    final RefMap<K, T> newMap = stream
        .collect(RefCollectors.toMap(e -> e.getKey(), e -> mapper.apply(e.getValue())));
    return new Delegate(parent, newMap);
  }

  public RefStream<T> stream() {
    return map.values().stream().filter(n -> null != n).distinct()
        .sorted(RefComparator.comparing(y -> System.identityHashCode(y.target)));
  }

  public void _free() {
    map.forEach((k, v) -> {
    });
    //    map.clear();
  }

  public @Override
  @SuppressWarnings("unused")
  DoubleBufferSet<K, T> addRef() {
    return (DoubleBufferSet<K, T>) super.addRef();
  }

  protected abstract T factory(final K layer, final double[] target);

  private T get(@Nullable final K layer, @Nullable final Supplier<T> factory) {
    if (null == map)
      throw new IllegalArgumentException();
    if (null == factory)
      throw new IllegalArgumentException();
    if (null == layer)
      throw new IllegalArgumentException();
    synchronized (map) {
      return map.computeIfAbsent(layer, l -> {
        T delta = factory.get();
        assert null != delta;
        if (log.isDebugEnabled())
          log.debug(String.format("Init key buffer for %s - %s params", l.getClass(), delta.target.length));
        return delta;
      });
    }
  }

  protected static @RefAware
  class Delegate<K, T extends DoubleBuffer<K>>
      extends DoubleBufferSet<K, T> {
    private final DoubleBufferSet<K, T> parent;

    public Delegate(final DoubleBufferSet<K, T> parent) {
      this(parent, new RefHashMap<>());
    }

    public Delegate(final DoubleBufferSet<K, T> parent,
                    @Nonnull final RefMap<K, T> newMap) {
      super(newMap);
      this.parent = parent;
    }

    public static @SuppressWarnings("unused")
    Delegate[] addRefs(Delegate[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(Delegate::addRef)
          .toArray((x) -> new Delegate[x]);
    }

    public @SuppressWarnings("unused")
    void _free() {
    }

    public @Override
    @SuppressWarnings("unused")
    Delegate<K, T> addRef() {
      return (Delegate<K, T>) super.addRef();
    }

    @Override
    protected T factory(final K layer, final double[] target) {
      return parent.factory(layer, target);
    }
  }
}
