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
import java.util.Map;
import java.util.function.Supplier;

public abstract class DoubleBufferSet<K, V extends DoubleBuffer<K>> extends ReferenceCountingBase {
  static final Logger log = LoggerFactory.getLogger(DoubleBufferSet.class);

  @Nonnull
  protected final RefHashMap<K, V> map = new RefHashMap<>();

  public DoubleBufferSet() {
  }

  public DoubleBufferSet(@Nonnull final DoubleBufferSet<K, V> toCopy) {
    this(toCopy.getMap());
    toCopy.freeRef();
  }

  public DoubleBufferSet(@Nonnull final RefMap<K, ? extends V> collect) {
    map.putAll(collect);
  }

  @Nonnull
  public RefMap<K, V> getMap() {
    return RefCollections.unmodifiableMap(map.addRef());
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public DoubleBufferSet<K, V> copy() {
    return map(x -> {
      try {
        return (V) x.copy();
      } finally {
        x.freeRef();
      }
    });
  }

  @javax.annotation.Nullable
  public V get(final K layer, final double[] ptr) {
    final V delta = get(layer, () -> factory(layer, ptr));
    assert delta.key.equals(layer);
    assert delta.target == ptr;
    return delta;
  }

  @javax.annotation.Nullable
  public V get(final K layer, @Nonnull final Tensor ptr) {
    V temp_15_0008 = get(layer, ptr.getData());
    ptr.freeRef();
    return temp_15_0008;
  }

  @Nonnull
  public DoubleBufferSet<K, V> map(@Nonnull final RefFunction<V, V> mapper) {
    RefHashSet<Map.Entry<K, V>> entries = map.entrySet();
    try {
      RefStream<Map.Entry<K, V>> stream = entries.stream();
      if (map.size() > 100) {
        stream = stream.parallel();
      }
      final RefMap<K, V> newMap = stream.collect(RefCollectors.toMap(e -> {
        try {
          return e.getKey();
        } finally {
          RefUtil.freeRef(e);
        }
      }, e -> {
        try {
          return mapper.apply(e.getValue());
        } finally {
          RefUtil.freeRef(e);
        }
      }));
      return new Delegate(this.addRef(), newMap);
    } finally {
      entries.freeRef();
    }
  }

  @Nonnull
  public RefStream<V> stream() {
    RefHashSet<V> values = map.values();
    RefStream<V> stream = values.stream().filter(v -> {
      if (null != v) {
        v.freeRef();
        return true;
      } else {
        return false;
      }
    })
        .distinct()
//        .sorted(RefComparator.comparingInt(v -> {
//          int hashCode = RefSystem.identityHashCode(v.target);
//          v.freeRef();
//          return hashCode;
//        }))
        ;
    values.freeRef();
    return stream;
  }

  public void _free() {
    super._free();
    map.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  DoubleBufferSet<K, V> addRef() {
    return (DoubleBufferSet<K, V>) super.addRef();
  }

  public int size() {
    return map.size();
  }

  public DoubleBufferSet<K, V> allFinite(double defaultValue) {
    return map((V doubleBuffer) -> {
      V v = (V) doubleBuffer.map(d -> Double.isFinite(d) ? d : defaultValue);
      doubleBuffer.freeRef();
      return v;
    });
  }

  protected abstract V factory(final K layer, final double[] target);

  @NotNull
  private V get(@Nullable final K layer, @Nullable final Supplier<V> factory) {
    if (null == factory)
      throw new IllegalArgumentException();
    if (null == layer)
      throw new IllegalArgumentException();
    try {
      synchronized (map) {
        return map.computeIfAbsent(layer, l -> {
          RefUtil.freeRef(l);
          V delta = factory.get();
          assert null != delta;
          if (log.isDebugEnabled())
            log.debug(RefString.format("Init key buffer for %s - %s params", l.getClass(), delta.target.length));
          return delta;
        });
      }
    } finally {
      RefUtil.freeRef(factory);
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
      this.parent = parent;
    }

    public @SuppressWarnings("unused")
    void _free() {
      if (null != parent)
        parent.freeRef();
      super._free();
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
