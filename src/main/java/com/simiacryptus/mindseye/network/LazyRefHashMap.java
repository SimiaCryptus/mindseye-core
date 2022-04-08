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

/**
 * This class provides a lazy reference hash map implementation.
 * It uses a RefHashMap to store key-value pairs, and each value
 * is wrapped in a RefAtomicReference to provide lazy initialization.
 *
 * @docgenVersion 9
 */
public abstract class LazyRefHashMap<K, V> extends ReferenceCountingBase implements Map<K, V> {
  private final RefHashMap<K, RefAtomicReference<V>> inner = new RefHashMap<>();

  /**
   * Returns true if the stack is empty, false otherwise.
   *
   * @docgenVersion 9
   */
  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  /**
   * Returns the size of the collection.
   *
   * @docgenVersion 9
   */
  @Override
  public int size() {
    return inner.size();
  }

  /**
   * Checks if a key exists in the map
   *
   * @return true if the key exists, false otherwise
   * @docgenVersion 9
   */
  @Override
  public boolean containsKey(Object key) {
    return inner.containsKey(key);
  }

  /**
   * Returns true if this map contains a mapping for the specified value.
   *
   * @docgenVersion 9
   */
  @Override
  public boolean containsValue(Object value) {
    return inner.containsValue(value);
  }

  /**
   * Returns the value of this Option.
   *
   * @return the value of this Option
   * @throws NoSuchElementException if this Option is empty
   * @docgenVersion 9
   */
  @Override
  public V get(Object key) {
    final RefAtomicReference<V> atomicReference;
    synchronized (inner) {
      atomicReference = inner.computeIfAbsent((K) key, k -> new RefAtomicReference<>());
    }
    return getOrInit((K) key, atomicReference);
  }

  /**
   * V put();
   *
   * @return
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public V put(K key, V value) {
    return getOrInit(key, inner.put(key, new RefAtomicReference<>(value)));
  }

  /**
   * Removes an element from the collection and returns it.
   *
   * @docgenVersion 9
   */
  @Override
  public V remove(Object key) {
    RefAtomicReference<V> reference = inner.remove(key);
    return getOrInit((K) key, reference);
  }

  /**
   * Returns the value stored in V, or initializes V with a default value if it is empty.
   *
   * @docgenVersion 9
   */
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

  /**
   * Adds all of the elements in the specified map to this map.
   *
   * @docgenVersion 9
   */
  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    if (m instanceof ReferenceCounting) {
      m.forEach((k, v) -> RefUtil.freeRef(inner.put(k, new RefAtomicReference<>(v))));
    } else {
      m.forEach((k, v) -> RefUtil.freeRef(inner.put(RefUtil.addRef(k), new RefAtomicReference<>(RefUtil.addRef(v)))));
    }
  }

  /**
   * Clears the contents of this buffer.
   *
   * @docgenVersion 9
   */
  @Override
  public void clear() {
    inner.clear();
  }

  /**
   * Returns a RefSet view of the keys contained in this map.
   *
   * @docgenVersion 9
   */
  @NotNull
  @Override
  public RefSet<K> keySet() {
    return inner.keySet();
  }

  /**
   * Returns a RefCollection of values.
   *
   * @docgenVersion 9
   */
  @NotNull
  @Override
  public RefCollection<V> values() {
    RefArrayList<V> vs = new RefArrayList<>();
    inner.forEach((k, v) -> vs.add(getOrInit(k, v)));
    return vs;
  }

  /**
   * Returns a Set of Entry objects, each representing a key-value pair in the RefMap.
   *
   * @docgenVersion 9
   */
  @NotNull
  @Override
  public RefSet<Entry<K, V>> entrySet() {
    RefHashSet<Entry<K, V>> entries = new RefHashSet<>();
    inner.forEach((k, v) -> entries.add(new RefEntry<K, V>(k, getOrInit(k, v)) {
      /**
       * Sets the value of V.
       *
       *   @docgenVersion 9
       */
      @javax.annotation.Nullable
      @Override
      public V setValue(@RefAware V value) {
        return v.getAndSet(value);
      }

      /**
       * Frees the memory associated with this object.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        v.freeRef();
        super._free();
      }
    }));
    return entries;
  }

  /**
   * Returns true if the object is the same as the one given, false otherwise.
   *
   * @docgenVersion 9
   */
  @Override
  public boolean equals(Object o) {
    return inner.equals(o);
  }

  /**
   * Returns a hash code value for the object. This method is
   * supported for the benefit of hash tables such as those provided by
   * HashMap.
   *
   * @docgenVersion 9
   */
  @Override
  public int hashCode() {
    return inner.hashCode();
  }

  /**
   * Add a reference to the LazyRefHashMap.
   *
   * @docgenVersion 9
   */
  @Override
  public LazyRefHashMap<K, V> addRef() {
    return (LazyRefHashMap<K, V>) super.addRef();
  }

  /**
   * V init();
   * <p>
   * This function initializes a value of type V.
   *
   * @docgenVersion 9
   */
  protected abstract @RefAware
  V init(@RefAware K key);

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    super._free();
    inner.freeRef();
  }
}
