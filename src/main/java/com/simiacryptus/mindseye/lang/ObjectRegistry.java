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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for maintaining a registry of all objects that inherit from
 * ReferenceCountingBase. This registry is used to keep track of all objects that need to
 * have their reference counts maintained.
 * <p>
 * The registry is implemented as a cache, using a RefMap. A ScheduledExecutorService is used
 * to periodically clean up the cache.
 *
 * @docgenVersion 9
 */
public final class ObjectRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ObjectRegistry.class);
  private static final RefMap<Class<? extends ReferenceCountingBase>, ObjectRecords<ReferenceCountingBase>> cache = new RefConcurrentHashMap<>();
  private static final ScheduledExecutorService maintenanceThread = Executors.newScheduledThreadPool(1,
      new ThreadFactoryBuilder().setDaemon(true).build());

  {
    maintenanceThread.scheduleAtFixedRate(() -> {
      RefCollection<ObjectRecords<ReferenceCountingBase>> values = cache.values();
      try {
        values.forEach(v -> {
          try {
            v.maintain();
          } finally {
            RefUtil.freeRef(v);
          }
        });
      } finally {
        values.freeRef();
      }
    }, 10000, 10000, TimeUnit.MILLISECONDS);
  }

  //  public RegisteredObjectBase() {
  //    register();
  //  }

  /**
   * Get all living instances of the given class.
   *
   * @param k   the class to get instances of
   * @param <T> the type of the class
   * @return a stream of living instances of the given class
   * @docgenVersion 9
   */
  @Nonnull
  public static <T extends ReferenceCountingBase> RefStream<T> getLivingInstances(@Nonnull final Class<T> k) {
    return getInstances(k).filter(x -> {
      boolean temp_32_0001 = !x.isFreed();
      x.freeRef();
      return temp_32_0001;
    });
  }

  /**
   * Returns a stream of instances of the given class.
   *
   * @param k   the class to get instances of
   * @param <T> the type of the class
   * @return a stream of instances of the given class
   * @docgenVersion 9
   */
  @Nonnull
  public static <T extends ReferenceCountingBase> RefStream<T> getInstances(@Nonnull final Class<T> k) {
    return stream(cache.entrySet()).filter(e -> {
      Class<? extends ReferenceCountingBase> aClass = e.getKey();
      RefUtil.freeRef(e);
      return k.isAssignableFrom(aClass);
    }).flatMap(recordsEntry -> {
      ObjectRecords<ReferenceCountingBase> value = recordsEntry.getValue();
      RefUtil.freeRef(recordsEntry);
      RefStream<RefWeakReference<ReferenceCountingBase>> stream = value.stream();
      value.freeRef();
      return stream;
    }).map((RefWeakReference<ReferenceCountingBase> x) -> {
      return (T) x.get();
    }).filter(x -> {
      if (x != null) {
        x.freeRef();
        return true;
      } else {
        return false;
      }
    });
  }

  /**
   * Registers an object for reference counting.
   *
   * @param registeredObjectBase the object to register
   * @docgenVersion 9
   */
  public static void register(ReferenceCountingBase registeredObjectBase) {
    ObjectRegistry.ObjectRecords<ReferenceCountingBase> objectRecords = cache.computeIfAbsent(registeredObjectBase.getClass(),
        k -> new ObjectRecords<>());
    objectRecords.add(RefWeakReference.wrap(registeredObjectBase));
    objectRecords.freeRef();
  }

  /**
   * Returns a stream of the given entries.
   *
   * @param entries the entries to stream
   * @param <T>     the type of the entries
   * @return a stream of the given entries
   * @docgenVersion 9
   */
  private static <T> RefStream<T> stream(RefSet<T> entries) {
    RefStream<T> refStream = entries.stream();
    entries.freeRef();
    return refStream;
  }

  /**
   * This class represents a collection of objects.
   * The "dirty" flag indicates whether any of the objects in the collection have been modified.
   *
   * @docgenVersion 9
   */
  private static class ObjectRecords<T extends ReferenceCountingBase>
      extends RefConcurrentLinkedDeque<RefWeakReference<T>> {
    private volatile boolean dirty = false;

    /**
     * Overrides the add method to set the dirty flag to true
     *
     * @param tWeakReference the object to add
     * @return true if the object was added, false otherwise
     * @docgenVersion 9
     */
    @Override
    public boolean add(final @RefAware RefWeakReference<T> tWeakReference) {
      dirty = true;
      return super.add(tWeakReference);
    }

    /**
     * Overrides the stream method to set the dirty flag to true
     * before returning the stream from the superclass.
     *
     * @docgenVersion 9
     */
    @Override
    public RefStream<RefWeakReference<T>> stream() {
      dirty = true;
      return super.stream();
    }

    /**
     * This method is unused.
     *
     * @docgenVersion 9
     */
    public @SuppressWarnings("unused")
    void _free() {
      super._free();
    }

    /**
     * @return an ObjectRecords<T> after adding a reference
     * @docgenVersion 9
     */
    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    ObjectRecords<T> addRef() {
      return (ObjectRecords<T>) super.addRef();
    }

    /**
     * This function is responsible for maintaining the state of the object.
     *
     * @docgenVersion 9
     */
    private void maintain() {
      if (dirty) {
        this.removeIf(ref -> {
          T t = ref.get();
          RefUtil.freeRef(ref);
          boolean notNull = null != t;
          RefUtil.freeRef(t);
          return notNull;
        });
        dirty = false;
      }
    }
  }

}
