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

/**
 * This class defines a DoubleBufferSet.
 * <p>
 * A DoubleBufferSet is a map that uses a reference-based hash table.
 *
 * @docgenVersion 9
 */
public abstract class DoubleBufferSet<K, V extends DoubleBuffer<K>> extends ReferenceCountingBase {
  /**
   * The Log.
   */
  static final Logger log = LoggerFactory.getLogger(DoubleBufferSet.class);

  /**
   * The Map.
   */
  @Nonnull
  protected final RefHashMap<K, V> map = new RefHashMap<>();

  /**
   * Instantiates a new Double buffer set.
   */
  public DoubleBufferSet() {
  }

  /**
   * Instantiates a new Double buffer set.
   *
   * @param toCopy the to copy
   */
  public DoubleBufferSet(@Nonnull final DoubleBufferSet<K, V> toCopy) {
    this(toCopy.getMap());
    toCopy.freeRef();
  }

  /**
   * Instantiates a new Double buffer set.
   *
   * @param collect the collect
   */
  public DoubleBufferSet(@Nonnull final RefMap<K, ? extends V> collect) {
    map.putAll(collect);
  }

  /**
   * Returns an unmodifiable view of the underlying map.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public RefMap<K, V> getMap() {
    return RefCollections.unmodifiableMap(map.addRef());
  }

  /**
   * @param x The input parameter.
   * @return The output DoubleBufferSet.
   * @docgenVersion 9
   */
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

  /**
   * Gets the value for the given layer and pointer.
   *
   * @param layer the layer to get the value for
   * @param ptr   the pointer to get the value for
   * @return the value for the given layer and pointer
   * @docgenVersion 9
   */
  @javax.annotation.Nullable
  public V get(final K layer, final double[] ptr) {
    final V delta = get(layer, () -> factory(layer, ptr));
    assert delta.key.equals(layer);
    assert delta.target == ptr;
    return delta;
  }

  /**
   * @return the value associated with the given layer, or null if there is no such value
   * @throws AssertionError if the value is non-null but its key does not equal the given layer
   * @docgenVersion 9
   */
  @javax.annotation.Nullable
  public V get(final K layer) {
    final V delta = map.get(layer);
    if (null == delta) return delta;
    assert delta.key.equals(layer);
    return delta;
  }

  /**
   * @param layer  The layer to get from
   * @param tensor The tensor to get from
   * @return The value from the layer and tensor
   * @docgenVersion 9
   */
  @javax.annotation.Nullable
  public V get(final K layer, @Nonnull final Tensor tensor) {
    V delta = get(layer, tensor.getData());
    tensor.freeRef();
    return delta;
  }

  /**
   * Maps the values in this set using the provided mapping function.
   *
   * @param mapper the mapping function to apply to each value
   * @return a new set containing the mapped values
   * @throws NullPointerException if the mapper is null
   * @docgenVersion 9
   */
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

  /**
   * Returns a stream of references to the values in this RefSet.
   *
   * @return a stream of references to the values in this RefSet
   * @docgenVersion 9
   */
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

  /**
   * Frees this object and its underlying resources.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    map.freeRef();
  }

  /**
   * @return a new reference to this DoubleBufferSet
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  DoubleBufferSet<K, V> addRef() {
    return (DoubleBufferSet<K, V>) super.addRef();
  }

  /**
   * Returns the number of elements in this map.
   *
   * @docgenVersion 9
   */
  public int size() {
    return map.size();
  }

  /**
   * Returns a new {@link DoubleBufferSet} where all non-finite values have been replaced with the provided default value.
   *
   * @param defaultValue the value to use for replacement
   * @return a new {@link DoubleBufferSet} with the provided default value replacing all non-finite values
   * @docgenVersion 9
   */
  public DoubleBufferSet<K, V> allFinite(double defaultValue) {
    return map((V doubleBuffer) -> {
      V v = (V) doubleBuffer.map(d -> Double.isFinite(d) ? d : defaultValue);
      doubleBuffer.freeRef();
      return v;
    });
  }

  /**
   * Returns a RefSet view of the keys contained in this map.
   *
   * @docgenVersion 9
   */
  public RefSet<K> keySet() {
    return map.keySet();
  }

  /**
   * This is an abstract factory method that creates a new value V for a given key K and target double array.
   *
   * @docgenVersion 9
   */
  protected abstract V factory(final K layer, final double[] target);

  /**
   * @param layer   the layer to get
   * @param factory the factory to use to create the value if it does not exist
   * @return the value for the layer
   * @docgenVersion 9
   */
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

  /**
   * This class represents a delegate.
   *
   * @param parent the parent DoubleBufferSet
   * @docgenVersion 9
   */
  protected static class Delegate<K, T extends DoubleBuffer<K>> extends DoubleBufferSet<K, T> {
    @Nullable
    private final DoubleBufferSet<K, T> parent;

    /**
     * Instantiates a new Delegate.
     *
     * @param parent the parent
     */
    public Delegate(final DoubleBufferSet<K, T> parent) {
      this(parent, new RefHashMap<>());
    }

    /**
     * Instantiates a new Delegate.
     *
     * @param parent the parent
     * @param newMap the new map
     */
    public Delegate(@Nullable final DoubleBufferSet<K, T> parent, @Nonnull final RefMap<K, T> newMap) {
      super(newMap);
      this.parent = parent;
    }

    /**
     * This method suppresses warnings for unused variables.
     * If the parent variable is not null, the parent's freeRef
     * method is called. Otherwise, the superclass' _free
     * method is called.
     *
     * @docgenVersion 9
     */
    public @SuppressWarnings("unused")
    void _free() {
      if (null != parent)
        parent.freeRef();
      super._free();
    }

    /**
     * @return a new Delegate object with a reference added
     * @docgenVersion 9
     */
    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    Delegate<K, T> addRef() {
      return (Delegate<K, T>) super.addRef();
    }

    /**
     * @Override protected T factory(final K layer, final double[] target) {
     * assert parent != null;
     * return parent.factory(layer, target);
     * }
     * @docgenVersion 9
     */
    @Override
    protected T factory(final K layer, final double[] target) {
      assert parent != null;
      return parent.factory(layer, target);
    }
  }
}
