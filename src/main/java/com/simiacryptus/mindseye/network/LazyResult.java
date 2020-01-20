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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
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
    long expectedCount = context.expectedCounts.getOrDefault(id, -1L);
    if (!context.calculated.containsKey(id)) {
      @Nullable
      Singleton singleton = null;
      synchronized (context) {
        if (!context.calculated.containsKey(id)) {
          if (null != singleton) singleton.freeRef();
          singleton = new Singleton();
          context.calculated.put(id, singleton.addRef());
        }
      }
      if (null != singleton) {
        try {
          @Nullable
          Result result = eval(context.addRef());
          if (null == result) {
            context.freeRef();
            throw new IllegalStateException();
          }
          singleton.set(new CountingResult(result.addRef()));
          result.freeRef();
        } catch (Throwable e) {
          log.warn("Error execuing network component", e);
          singleton.set(e);
        } finally {
          singleton.freeRef();
        }
      }
    }
    Supplier resultSupplier = context.calculated.get(id);
    if (null == resultSupplier) {
      context.freeRef();
      throw new IllegalStateException();
    }
    Object obj = resultSupplier.get();
    if (obj != null && obj instanceof Throwable) {
      context.freeRef();
      throw new RuntimeException((Throwable) obj);
    }
    @Nullable
    CountingResult nnResult = (CountingResult) obj;
    if (null == nnResult) {
      context.freeRef();
      throw new IllegalStateException();
    }
    CountingResult.CountingAccumulator temp_56_0001 = nnResult.getAccumulator();
    int references = temp_56_0001.increment();
    temp_56_0001.freeRef();
    if (references <= 0) {
      nnResult.freeRef();
      context.freeRef();
      throw new IllegalStateException();
    }
    if (expectedCount >= 0 && references > expectedCount) {
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

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LazyResult addRef() {
    return (LazyResult) super.addRef();
  }

  @Nullable
  protected abstract Result eval(GraphEvaluationContext t);

}
