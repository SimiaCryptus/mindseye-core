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
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;
import com.simiacryptus.ref.wrappers.RefWeakReference;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefStream;

public abstract @com.simiacryptus.ref.lang.RefAware class RegisteredObjectBase extends ReferenceCountingBase {
  private static final Logger logger = LoggerFactory.getLogger(RegisteredObjectBase.class);
  private static final com.simiacryptus.ref.wrappers.RefMap<Class<? extends RegisteredObjectBase>, ObjectRecords<RegisteredObjectBase>> cache = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
  private static final ScheduledExecutorService maintenanceThread = Executors.newScheduledThreadPool(1,
      new ThreadFactoryBuilder().setDaemon(true).build());

  //  public RegisteredObjectBase() {
  //    register();
  //  }

  public static <T extends RegisteredObjectBase> com.simiacryptus.ref.wrappers.RefStream<T> getLivingInstances(
      final Class<T> k) {
    return getInstances(k).filter(x -> !x.isFinalized());
  }

  public static <T extends RegisteredObjectBase> com.simiacryptus.ref.wrappers.RefStream<T> getInstances(
      final Class<T> k) {
    return cache.entrySet().stream().filter(e -> k.isAssignableFrom(e.getKey())).map(x -> x.getValue())
        .flatMap(ObjectRecords::stream).map(x -> (T) x.get()).filter(x -> x != null);
  }

  protected void register() {
    cache.computeIfAbsent(getClass(), k -> new ObjectRecords<>())
        .add(new com.simiacryptus.ref.wrappers.RefWeakReference<>(this));
  }

  private static @com.simiacryptus.ref.lang.RefAware class ObjectRecords<T extends RegisteredObjectBase> extends
      com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<com.simiacryptus.ref.wrappers.RefWeakReference<T>> {
    private volatile boolean dirty = false;
    private final ScheduledFuture<?> maintenanceFuture = maintenanceThread.scheduleAtFixedRate(this::maintain, 1, 1,
        TimeUnit.SECONDS);

    @Override
    public boolean add(final com.simiacryptus.ref.wrappers.RefWeakReference<T> tWeakReference) {
      dirty = true;
      return super.add(tWeakReference);
    }

    @Override
    public com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.ref.wrappers.RefWeakReference<T>> stream() {
      dirty = true;
      return super.stream();
    }

    private void maintain() {
      if (dirty) {
        this.removeIf(ref -> null == ref.get());
        dirty = false;
      }
    }

    public @SuppressWarnings("unused") void _free() {
    }

    public @Override @SuppressWarnings("unused") ObjectRecords<T> addRef() {
      return (ObjectRecords<T>) super.addRef();
    }

    public static @SuppressWarnings("unused") ObjectRecords[] addRefs(ObjectRecords[] array) {
      if (array == null)
        return null;
      return java.util.Arrays.stream(array).filter((x) -> x != null).map(ObjectRecords::addRef)
          .toArray((x) -> new ObjectRecords[x]);
    }
  }

  public @SuppressWarnings("unused") void _free() {
  }

  public @Override @SuppressWarnings("unused") RegisteredObjectBase addRef() {
    return (RegisteredObjectBase) super.addRef();
  }

  public static @SuppressWarnings("unused") RegisteredObjectBase[] addRefs(RegisteredObjectBase[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RegisteredObjectBase::addRef)
        .toArray((x) -> new RegisteredObjectBase[x]);
  }

  public static @SuppressWarnings("unused") RegisteredObjectBase[][] addRefs(RegisteredObjectBase[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RegisteredObjectBase::addRefs)
        .toArray((x) -> new RegisteredObjectBase[x][]);
  }

}
