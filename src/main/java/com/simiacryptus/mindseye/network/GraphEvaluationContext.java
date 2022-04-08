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
 * This class provides a context for evaluating graphs. It contains a map of expected counts,
 * as well as a stack trace element for the thread that created it. Additionally, it has a
 * reference map of calculated results.
 *
 * @docgenVersion 9
 */
public class GraphEvaluationContext extends ReferenceCountingBase {

  private final Map<UUID, Long> expectedCounts = new ConcurrentHashMap<>();

  //final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  private final RefMap<UUID, RefAtomicReference<CountingResult>> calculated = new RefConcurrentHashMap<>();

  /**
   * Returns a map of UUIDs to CountingResults.
   * Asserts that the object is alive.
   *
   * @docgenVersion 9
   */
  public RefMap<UUID, RefAtomicReference<CountingResult>> getCalculated() {
    assertAlive();
    return calculated.addRef();
  }

  /**
   * Returns a map of UUIDs to expected counts.
   * Asserts that the node is alive.
   *
   * @docgenVersion 9
   */
  public Map<UUID, Long> getExpectedCounts() {
    assertAlive();
    return expectedCounts;
  }

  /**
   * @param key           the key to use
   * @param unaryOperator the unary operator to use
   * @return the counting result
   * @docgenVersion 9
   */
  public CountingResult get(UUID key, UnaryOperator<CountingResult> unaryOperator) {
    final RefAtomicReference<CountingResult> reference;
    synchronized (calculated) {
      reference = calculated.computeIfAbsent(key, new Function<UUID, RefAtomicReference<CountingResult>>() {
        /**
         * @Nonnull
         * @Override
         * @RefIgnore
         *
         *   @docgenVersion 9
         */
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
   * Returns the CountingResult with the specified id.
   * Asserts that the object is alive.
   *
   * @param id the id of the CountingResult to return
   * @return the CountingResult with the specified id
   * @docgenVersion 9
   */
  public CountingResult get(UUID id) {
    assertAlive();
    return RefAtomicReference.get(calculated.get(id));
  }

  /**
   * Frees this object and its resources.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    calculated.freeRef();
  }

  /**
   * @return a new GraphEvaluationContext with an incremented reference count
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  GraphEvaluationContext addRef() {
    return (GraphEvaluationContext) super.addRef();
  }
}
