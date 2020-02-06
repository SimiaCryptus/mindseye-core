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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefLinkedBlockingQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Singleton<T> extends ReferenceCountingBase implements Supplier<T> {
  private final RefLinkedBlockingQueue<T> queue = new RefLinkedBlockingQueue<T>();

  public Singleton() {
  }

  public boolean isDefined() {
    return !queue.isEmpty();
  }

  @Nonnull
  @RefAware
  public synchronized T getOrInit(@Nonnull Supplier<T> fn) {
    if (queue.isEmpty()) {
      set(fn.get());
    }
    return get();
  }

  @Nonnull
  @Override
  @RefAware
  public synchronized T get() {
    return queue.peek();
  }

  public synchronized void set(@RefAware @Nonnull T obj) {
    if (!queue.isEmpty()) {
      RefUtil.freeRef(obj);
      throw new IllegalStateException();
    }
    queue.add(obj);
  }

  @Nullable
  @RefAware
  public synchronized T remove() {
    try {
      return queue.poll(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Singleton<T> addRef() {
    return (Singleton<T>) super.addRef();
  }

  @Override
  protected void _free() {
    queue.freeRef();
    super._free();
  }
}
