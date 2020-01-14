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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Singleton<T> implements Supplier<T> {
  private final BlockingDeque<T> deque = new LinkedBlockingDeque<>();

  public Singleton() {
  }

  public boolean isDefined() {
    return !deque.isEmpty();
  }

  @Nonnull
  public synchronized T getOrInit(@Nonnull Supplier<T> fn) {
    if (deque.isEmpty())
      set(fn.get());
    return get();
  }

  @Nonnull
  @Override
  public T get() {
    try {
      T take = deque.take();
      deque.add(take);
      return take;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public Singleton<T> set(@Nonnull @RefAware T obj) {
    assert deque.isEmpty();
    deque.add(obj);
    return this;
  }

  @Nullable
  public T remove() {
    try {
      return deque.poll(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
