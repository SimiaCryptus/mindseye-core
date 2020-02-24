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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefFunction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("serial")
abstract class LazyResult extends ReferenceCountingBase implements DAGNode {
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


  @Nullable
  @Override
  public CountingResult get(@Nonnull final GraphEvaluationContext context) {
    context.assertAlive();
    assertAlive();
    try {
      Long expectedCount = context.expectedCounts.getOrDefault(id, -1L);
      Supplier resultSupplier = context.calculated.computeIfAbsent(id, new Function<UUID, Supplier<CountingResult>>() {
        @Nonnull
        @Override
        @RefIgnore
        public Supplier<CountingResult> apply(UUID id) {
          Singleton singleton = new Singleton();
          try {
            @Nullable
            Result result = LazyResult.this.eval(context.addRef());
            if (null == result) {
              throw new IllegalStateException();
            }
            singleton.set(new CountingResult(result));
          } catch (Throwable e) {
            log.warn("Error execuing network component", e);
            singleton.set(e);
          }
          return singleton;
        }
      });
      if (null == resultSupplier) {
        throw new IllegalStateException();
      }
      Object obj = resultSupplier.get();
      RefUtil.freeRef(resultSupplier);
      if (null == obj) {
        throw new IllegalStateException();
      }
      if (obj instanceof Throwable) {
        throw new RuntimeException((Throwable) obj);
      }
      @Nullable
      CountingResult nnResult = (CountingResult) obj;
      CountingResult.CountingAccumulator countingAccumulator = nnResult.getAccumulator();
      int references = countingAccumulator.increment();
      countingAccumulator.freeRef();
      if (references <= 0) {
        nnResult.freeRef();
        throw new IllegalStateException();
      }
      if (null != expectedCount && expectedCount >= 0 && references > expectedCount) {
        nnResult.freeRef();
        throw new IllegalStateException();
      }
      if (null != expectedCount && expectedCount > 0 && references >= expectedCount) {
        RefUtil.freeRef(context.calculated.remove(id));
      }
      return nnResult;
    } finally {
      context.freeRef();
    }
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LazyResult addRef() {
    return (LazyResult) super.addRef();
  }

  @Nullable
  protected abstract Result eval(GraphEvaluationContext t);

}
