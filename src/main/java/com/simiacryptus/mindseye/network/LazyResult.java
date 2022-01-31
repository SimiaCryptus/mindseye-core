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

import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * The type Lazy result.
 */
@SuppressWarnings("serial")
abstract class LazyResult extends ReferenceCountingBase implements DAGNode {
  private static final Logger log = LoggerFactory.getLogger(LazyResult.class);
  /**
   * The Id.
   */
  public final UUID id;

//  public LazyResult() {
//    this(UUID.randomUUID());
//  }

  /**
   * Instantiates a new Lazy result.
   *
   * @param id the id
   */
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
  public CountingResult get(@Nonnull final GraphEvaluationContext context, Layer consumer) {
    Map<UUID, Long> expectedCounts = context.getExpectedCounts();
    try {
      assertAlive();
      Long expectedCount = expectedCounts.get(id);
      CountingResult countingResult = context.get(id, prev -> {
        if (null != prev) return prev;
        else RefUtil.freeRef(prev);
        try {
          @Nullable
          Result result = this.eval(context.addRef());
          if (null == result) {
            throw new IllegalStateException();
          }
          return new CountingResult(result);
        } catch (Throwable e) {
          throw new RuntimeException("Error execuing network component", e);
        }
      });
      Result.Accumulator accumulator = countingResult.getAccumulator();
      if (accumulator instanceof CountingResult.CountingAccumulator) {
        CountingResult.CountingAccumulator countingAccumulator = (CountingResult.CountingAccumulator) accumulator;
        int references = countingAccumulator.incrementFwd(consumer);
        countingAccumulator.freeRef();
        if (references <= 0) {
          countingResult.freeRef();
          throw new IllegalStateException();
        }
        if (null != expectedCount) {
          if (references == expectedCount) {
            //RefUtil.freeRef(calculated.remove(id));
          } else if (references > expectedCount) {
//            nnResult.freeRef();
//            throw new IllegalStateException();
          }
        }
      } else {
        if (null != consumer) consumer.freeRef();
        if (null != accumulator) accumulator.freeRef();
      }
      return countingResult;
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

  /**
   * Eval result.
   *
   * @param t the t
   * @return the result
   */
  @Nullable
  public abstract Result eval(GraphEvaluationContext t);

}
