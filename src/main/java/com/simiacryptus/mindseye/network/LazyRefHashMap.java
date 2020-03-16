/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.mindseye.network;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class LazyRefHashMap<K, V> extends ReferenceCountingBase implements Map<K, V> {
  private final RefHashMap<K, RefAtomicReference<V>> inner = new RefHashMap<>();

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @Override
  public int size() {
    return inner.size();
  }

  @Override
  public boolean containsKey(Object key) {
    return inner.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return inner.containsValue(value);
  }

  @Override
  public V get(Object key) {
    final RefAtomicReference<V> atomicReference;
    synchronized (inner) {
      atomicReference = inner.computeIfAbsent((K) key, k -> new RefAtomicReference<>());
    }
    return getOrInit((K) key, atomicReference);
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    return getOrInit(key, inner.put(key, new RefAtomicReference<>(value)));
  }

  @Override
  public V remove(Object key) {
    RefAtomicReference<V> reference = inner.remove(key);
    return getOrInit((K) key, (RefAtomicReference<V>) reference);
  }

  public V getOrInit(@RefAware K key, RefAtomicReference<V> reference) {
    V get = reference.updateAndGet(prev -> {
      if (null != prev) return prev;
      else RefUtil.freeRef(prev);
      return init(RefUtil.addRef(key));
    });
    RefUtil.freeRef(key);
    reference.freeRef();
    return get;
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    if (m instanceof ReferenceCounting) {
      m.forEach((k, v) -> RefUtil.freeRef(inner.put(k, new RefAtomicReference<>(v))));
    } else {
      m.forEach((k, v) -> RefUtil.freeRef(inner.put(RefUtil.addRef(k), new RefAtomicReference<>(RefUtil.addRef(v)))));
    }
  }

  @Override
  public void clear() {
    inner.clear();
  }

  @NotNull
  @Override
  public RefSet<K> keySet() {
    return inner.keySet();
  }

  @NotNull
  @Override
  public RefCollection<V> values() {
    RefArrayList<V> vs = new RefArrayList<>();
    inner.forEach((k, v) -> vs.add(getOrInit(k, v)));
    return vs;
  }

  @NotNull
  @Override
  public RefSet<Entry<K, V>> entrySet() {
    RefHashSet<Entry<K, V>> entries = new RefHashSet<>();
    inner.forEach((k, v) -> entries.add(new RefEntry<K, V>(k, getOrInit(k, v)) {
      @javax.annotation.Nullable
      @Override
      public V setValue(@RefAware V value) {
        return v.getAndSet(value);
      }

      @Override
      protected void _free() {
        v.freeRef();
        super._free();
      }
    }));
    return entries;
  }

  @Override
  public boolean equals(Object o) {
    return inner.equals(o);
  }

  @Override
  public int hashCode() {
    return inner.hashCode();
  }

  @Override
  public LazyRefHashMap<K, V> addRef() {
    return (LazyRefHashMap<K, V>) super.addRef();
  }

  protected abstract @RefAware
  V init(@RefAware K key);

  @Override
  protected void _free() {
    super._free();
    inner.freeRef();
  }
}
