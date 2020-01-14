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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class DoubleBufferSet<K, T extends DoubleBuffer<K>> extends ReferenceCountingBase {
  static final Logger log = LoggerFactory.getLogger(DoubleBufferSet.class);

  @Nonnull
  protected final RefHashMap<K, T> map = new RefHashMap<>();

  public DoubleBufferSet() {
  }

  public DoubleBufferSet(@Nonnull final DoubleBufferSet<K, T> toCopy) {
    this(toCopy.map);
    toCopy.freeRef();
  }

  public DoubleBufferSet(@Nonnull final RefMap<K, ? extends T> collect) {
    collect.forEach((k, v) -> {
      assert null != k;
      assert null != v;
      v.freeRef();
    });
    synchronized (collect) {
      map.putAll(collect.addRef());
      map.forEach((k, v) -> {
        if (null != v)
          v.freeRef();
      });
    }
    collect.freeRef();
  }

  @Nonnull
  public RefMap<K, T> getMap() {
    return RefCollections.unmodifiableMap(RefUtil.addRef(map));
  }

  @Nullable
  public static @SuppressWarnings("unused")
  DoubleBufferSet[] addRefs(@Nullable DoubleBufferSet[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DoubleBufferSet::addRef)
        .toArray((x) -> new DoubleBufferSet[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  DoubleBufferSet[][] addRefs(@Nullable DoubleBufferSet[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DoubleBufferSet::addRefs)
        .toArray((x) -> new DoubleBufferSet[x][]);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public DoubleBufferSet<K, T> copy() {
    return map(x -> {
      T temp_15_0002 = (T) x.copy();
      x.freeRef();
      return temp_15_0002;
    });
  }

  @javax.annotation.Nullable
  public T get(final K layer, final double[] ptr) {
    final T delta = get(layer, () -> factory(layer, ptr));
    assert delta.key.equals(layer);
    assert delta.target == ptr;
    return delta;
  }

  @javax.annotation.Nullable
  public T get(final K layer, @Nonnull final Tensor ptr) {
    T temp_15_0008 = get(layer, ptr.getData());
    ptr.freeRef();
    return temp_15_0008;
  }

  @Nonnull
  public DoubleBufferSet<K, T> map(@Nonnull final Function<T, T> mapper) {
    @Nonnull final DoubleBufferSet<K, T> parent = this.addRef();
    RefHashSet<Map.Entry<K, T>> temp_15_0009 = map.entrySet();
    RefStream<Map.Entry<K, T>> stream = temp_15_0009.stream();
    temp_15_0009.freeRef();
    if (map.size() > 100) {
      stream = stream.parallel();
    }
    final RefMap<K, T> newMap = stream.collect(RefCollectors.toMap(e -> {
      K temp_15_0004 = e.getKey();
      RefUtil.freeRef(e);
      return temp_15_0004;
    }, e -> {
      T temp_15_0010 = e.getValue();
      T temp_15_0005 = mapper.apply(temp_15_0010);
      if (null != temp_15_0010)
        temp_15_0010.freeRef();
      RefUtil.freeRef(e);
      return temp_15_0005;
    }));
    DoubleBufferSet.Delegate temp_15_0003 = new Delegate(parent,
        newMap == null ? null : newMap.addRef());
    if (null != newMap)
      newMap.freeRef();
    return temp_15_0003;
  }

  @Nonnull
  public RefStream<T> stream() {
    RefHashSet<T> temp_15_0012 = map.values();
    RefStream<T> temp_15_0011 = temp_15_0012.stream().filter(n -> {
      boolean temp_15_0006 = null != n;
      if (null != n)
        n.freeRef();
      return temp_15_0006;
    }).distinct().sorted(RefComparator.comparing(y -> {
      int temp_15_0007 = RefSystem.identityHashCode(y.target);
      y.freeRef();
      return temp_15_0007;
    }));
    temp_15_0012.freeRef();
    return temp_15_0011;
  }

  public void _free() {
    map.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  DoubleBufferSet<K, T> addRef() {
    return (DoubleBufferSet<K, T>) super.addRef();
  }

  protected abstract T factory(final K layer, final double[] target);

  @NotNull
  private T get(@Nullable final K layer, @Nullable final Supplier<T> factory) {
    if (null == factory)
      throw new IllegalArgumentException();
    if (null == layer)
      throw new IllegalArgumentException();
    synchronized (map) {
      return map.computeIfAbsent(layer, l -> {
        T delta = factory.get();
        assert null != delta;
        if (log.isDebugEnabled())
          log.debug(RefString.format("Init key buffer for %s - %s params", l.getClass(), delta.target.length));
        return delta;
      });
    }
  }

  protected static class Delegate<K, T extends DoubleBuffer<K>> extends DoubleBufferSet<K, T> {
    @Nullable
    private final DoubleBufferSet<K, T> parent;

    public Delegate(final DoubleBufferSet<K, T> parent) {
      this(parent, new RefHashMap<>());
    }

    public Delegate(@Nullable final DoubleBufferSet<K, T> parent, @Nonnull final RefMap<K, T> newMap) {
      super(newMap);
      DoubleBufferSet<K, T> temp_15_0001 = parent == null ? null : parent.addRef();
      this.parent = temp_15_0001 == null ? null : temp_15_0001.addRef();
      if (null != temp_15_0001)
        temp_15_0001.freeRef();
      if (null != parent)
        parent.freeRef();
    }

    @Nullable
    public static @SuppressWarnings("unused")
    Delegate[] addRefs(@Nullable Delegate[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(Delegate::addRef).toArray((x) -> new Delegate[x]);
    }

    public @SuppressWarnings("unused")
    void _free() {
      if (null != parent)
        parent.freeRef();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    Delegate<K, T> addRef() {
      return (Delegate<K, T>) super.addRef();
    }

    @Override
    protected T factory(final K layer, final double[] target) {
      assert parent != null;
      return parent.factory(layer, target);
    }
  }
}
