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

package com.simiacryptus.mindseye.network;

import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.mindseye.lang.Singleton;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("serial")
abstract @RefAware
class LazyResult extends ReferenceCountingBase implements DAGNode {
  private static final Logger log = LoggerFactory.getLogger(LazyResult.class);
  public final UUID id;

  public LazyResult() {
    this(UUID.randomUUID());
  }

  protected LazyResult(final UUID id) {
    super();
    this.id = id;
  }

  @Override
  public final UUID getId() {
    return id;
  }

  public static @SuppressWarnings("unused")
  LazyResult[] addRefs(LazyResult[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LazyResult::addRef)
        .toArray((x) -> new LazyResult[x]);
  }

  public static @SuppressWarnings("unused")
  LazyResult[][] addRefs(LazyResult[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LazyResult::addRefs)
        .toArray((x) -> new LazyResult[x][]);
  }

  @Nullable
  @Override
  public CountingResult get(@Nonnull final GraphEvaluationContext context) {
    context.assertAlive();
    assertAlive();
    long expectedCount = context.expectedCounts.getOrDefault(id, -1L);
    if (!context.calculated.containsKey(id)) {
      @Nullable
      Singleton singleton = null;
      synchronized (context) {
        if (!context.calculated.containsKey(id)) {
          singleton = new Singleton();
          context.calculated.put(id, singleton);
        }
      }
      if (null != singleton) {
        try {
          @Nullable
          Result result = eval(context == null ? null : context.addRef());
          if (null == result) {
            if (null != result)
              result.freeRef();
            context.freeRef();
            throw new IllegalStateException();
          }
          singleton.set(new CountingResult(result == null ? null : result.addRef()));
          if (null != result)
            result.freeRef();
        } catch (Throwable e) {
          log.warn("Error execuing network component", e);
          singleton.set(e);
        }
      }
    }
    Supplier resultSupplier = context.calculated.get(id);
    if (null == resultSupplier) {
      context.freeRef();
      throw new IllegalStateException();
    }
    Object obj = null == resultSupplier ? null : resultSupplier.get();
    if (obj != null && obj instanceof Throwable) {
      context.freeRef();
      throw new RuntimeException((Throwable) obj);
    }
    if (obj != null && obj instanceof RuntimeException) {
      context.freeRef();
      throw ((RuntimeException) obj);
    }
    @Nullable
    CountingResult nnResult = (CountingResult) obj;
    if (null == nnResult) {
      if (null != nnResult)
        nnResult.freeRef();
      context.freeRef();
      throw new IllegalStateException();
    }
    CountingResult.CountingAccumulator temp_56_0001 = nnResult.getAccumulator();
    int references = temp_56_0001.increment();
    if (null != temp_56_0001)
      temp_56_0001.freeRef();
    if (references <= 0) {
      if (null != nnResult)
        nnResult.freeRef();
      context.freeRef();
      throw new IllegalStateException();
    }
    if (expectedCount >= 0 && references > expectedCount) {
      if (null != nnResult)
        nnResult.freeRef();
      context.freeRef();
      throw new IllegalStateException();
    }
    if (expectedCount <= 0 || references < expectedCount) {
      RefUtil.freeRef(nnResult.getData());
    } else {
      context.calculated.remove(id);
    }
    context.freeRef();
    return nnResult;
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  LazyResult addRef() {
    return (LazyResult) super.addRef();
  }

  @Nullable
  protected abstract Result eval(GraphEvaluationContext t);

}
