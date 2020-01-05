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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract @RefAware
class RegisteredObjectBase extends ReferenceCountingBase {
  private static final Logger logger = LoggerFactory.getLogger(RegisteredObjectBase.class);
  private static final RefMap<Class<? extends RegisteredObjectBase>, ObjectRecords<RegisteredObjectBase>> cache = new RefConcurrentHashMap<>();
  private static final ScheduledExecutorService maintenanceThread = Executors.newScheduledThreadPool(1,
      new ThreadFactoryBuilder().setDaemon(true).build());

  //  public RegisteredObjectBase() {
  //    register();
  //  }

  public static <T extends RegisteredObjectBase> RefStream<T> getLivingInstances(final Class<T> k) {
    return getInstances(k).filter(x -> {
      boolean temp_32_0001 = !x.isFinalized();
      if (null != x)
        x.freeRef();
      return temp_32_0001;
    });
  }

  public static <T extends RegisteredObjectBase> RefStream<T> getInstances(final Class<T> k) {
    RefSet<Map.Entry<Class<? extends RegisteredObjectBase>, RegisteredObjectBase.ObjectRecords<RegisteredObjectBase>>> temp_32_0006 = cache
        .entrySet();
    RefStream<T> temp_32_0005 = temp_32_0006.stream().filter(e -> {
      boolean temp_32_0002 = k.isAssignableFrom(e.getKey());
      if (null != e)
        RefUtil.freeRef(e);
      return temp_32_0002;
    }).map(x -> {
      RegisteredObjectBase.ObjectRecords<RegisteredObjectBase> temp_32_0003 = x
          .getValue();
      if (null != x)
        RefUtil.freeRef(x);
      return temp_32_0003;
    }).flatMap(ObjectRecords::stream).map(x -> (T) x.get()).filter(x -> {
      boolean temp_32_0004 = x != null;
      if (null != x)
        x.freeRef();
      return temp_32_0004;
    });
    if (null != temp_32_0006)
      temp_32_0006.freeRef();
    return temp_32_0005;
  }

  public static @SuppressWarnings("unused")
  RegisteredObjectBase[] addRefs(RegisteredObjectBase[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(RegisteredObjectBase::addRef)
        .toArray((x) -> new RegisteredObjectBase[x]);
  }

  public static @SuppressWarnings("unused")
  RegisteredObjectBase[][] addRefs(RegisteredObjectBase[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(RegisteredObjectBase::addRefs)
        .toArray((x) -> new RegisteredObjectBase[x][]);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  RegisteredObjectBase addRef() {
    return (RegisteredObjectBase) super.addRef();
  }

  protected void register() {
    RegisteredObjectBase.ObjectRecords<RegisteredObjectBase> temp_32_0007 = cache
        .computeIfAbsent(getClass(), k -> new ObjectRecords<>());
    temp_32_0007.add(new RefWeakReference<>(this.addRef()));
    if (null != temp_32_0007)
      temp_32_0007.freeRef();
  }

  private static @RefAware
  class ObjectRecords<T extends RegisteredObjectBase>
      extends RefConcurrentLinkedDeque<RefWeakReference<T>> {
    private volatile boolean dirty = false;
    private final ScheduledFuture<?> maintenanceFuture = maintenanceThread.scheduleAtFixedRate(this::maintain, 1, 1,
        TimeUnit.SECONDS);

    public static @SuppressWarnings("unused")
    ObjectRecords[] addRefs(ObjectRecords[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(ObjectRecords::addRef)
          .toArray((x) -> new ObjectRecords[x]);
    }

    @Override
    public boolean add(final RefWeakReference<T> tWeakReference) {
      dirty = true;
      return super.add(tWeakReference);
    }

    @Override
    public RefStream<RefWeakReference<T>> stream() {
      dirty = true;
      return super.stream();
    }

    public @SuppressWarnings("unused")
    void _free() {
    }

    public @Override
    @SuppressWarnings("unused")
    ObjectRecords<T> addRef() {
      return (ObjectRecords<T>) super.addRef();
    }

    private void maintain() {
      if (dirty) {
        this.removeIf(ref -> {
          return null == ref.get();
        });
        dirty = false;
      }
    }
  }

}
