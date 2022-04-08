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
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * This is a Singleton class that creates a new RefLinkedBlockingQueue.
 *
 * @docgenVersion 9
 */
public class Singleton<T> extends ReferenceCountingBase implements Supplier<T> {
  private final RefLinkedBlockingQueue<T> queue = new RefLinkedBlockingQueue<T>();

  /**
   * Instantiates a new Singleton.
   */
  public Singleton() {
  }

  /**
   * Returns true if the value of this Option is non-empty.
   *
   * @docgenVersion 9
   */
  public boolean isDefined() {
    return !queue.isEmpty();
  }

  /**
   * Returns the value in this `Option`, or computes it if it is not present,
   * and stores it in this `Option` for future calls.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @RefAware
  public synchronized T getOrInit(@Nonnull Supplier<T> fn) {
    assertAlive();
    if (queue.isEmpty()) {
      set(fn.get());
    }
    return get();
  }

  /**
   * Returns the value of type T.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  @RefAware
  public synchronized T get() {
    assertAlive();
    return queue.peek();
  }

  /**
   * Sets the value.
   *
   * @docgenVersion 9
   */
  public synchronized void set(@RefAware @Nonnull T obj) {
    assertAlive();
    if (!queue.isEmpty()) {
      RefUtil.freeRef(obj);
      throw new IllegalStateException();
    }
    queue.add(obj);
  }

  /**
   * Removes an element from the beginning of the list and returns it.
   *
   * @docgenVersion 9
   */
  @Nullable
  @RefAware
  public synchronized T remove() {
    assertAlive();
    try {
      return queue.poll(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw Util.throwException(e);
    }
  }

  /**
   * Add a reference to the Singleton of type T.
   *
   * @docgenVersion 9
   */
  @Override
  public Singleton<T> addRef() {
    return (Singleton<T>) super.addRef();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    queue.freeRef();
    super._free();
  }
}
