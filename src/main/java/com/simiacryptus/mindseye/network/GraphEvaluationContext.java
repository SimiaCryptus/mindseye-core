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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefAtomicReference;
import com.simiacryptus.ref.wrappers.RefConcurrentHashMap;
import com.simiacryptus.ref.wrappers.RefMap;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The type Graph evaluation context.
 */
class GraphEvaluationContext extends ReferenceCountingBase {

  private final Map<UUID, Long> expectedCounts = new ConcurrentHashMap<>();

  //final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  private final RefMap<UUID, RefAtomicReference<CountingResult>> calculated = new RefConcurrentHashMap<>();

  /**
   * Gets calculated.
   *
   * @return the calculated
   */
  public RefMap<UUID, RefAtomicReference<CountingResult>> getCalculated() {
    assertAlive();
    return calculated.addRef();
  }

  /**
   * Gets expected counts.
   *
   * @return the expected counts
   */
  public Map<UUID, Long> getExpectedCounts() {
    assertAlive();
    return expectedCounts;
  }

  /**
   * Get counting result.
   *
   * @param key           the key
   * @param unaryOperator the unary operator
   * @return the counting result
   */
  public CountingResult get(UUID key, UnaryOperator<CountingResult> unaryOperator) {
    final RefAtomicReference<CountingResult> reference;
    synchronized (calculated) {
      reference = calculated.computeIfAbsent(key, new Function<UUID, RefAtomicReference<CountingResult>>() {
        @Nonnull
        @Override
        @RefIgnore
        public RefAtomicReference<CountingResult> apply(UUID id) {
          return new RefAtomicReference<CountingResult>();
        }
      });
    }
    try {
      return reference.updateAndGet(unaryOperator);
    } finally {
      reference.freeRef();
    }
  }

  /**
   * Get counting result.
   *
   * @param id the id
   * @return the counting result
   */
  public CountingResult get(UUID id) {
    assertAlive();
    return RefAtomicReference.get(calculated.get(id));
  }

  public void _free() {
    super._free();
    calculated.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  GraphEvaluationContext addRef() {
    return (GraphEvaluationContext) super.addRef();
  }
}
