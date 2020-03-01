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

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefAtomicReference;
import com.simiacryptus.ref.wrappers.RefConcurrentHashMap;
import com.simiacryptus.ref.wrappers.RefMap;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class GraphEvaluationContext extends ReferenceCountingBase {

  private final RefMap<UUID, Long> expectedCounts = new RefConcurrentHashMap<>();

  //final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  private final RefMap<UUID, RefAtomicReference<CountingResult>> calculated = new RefConcurrentHashMap<>();

  public RefMap<UUID, RefAtomicReference<CountingResult>> getCalculated() {
    assertAlive();
    return calculated.addRef();
  }

  public RefMap<UUID, Long> getExpectedCounts() {
    assertAlive();
    return expectedCounts.addRef();
  }

  public void _free() {
    super._free();
    expectedCounts.freeRef();
    calculated.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  GraphEvaluationContext addRef() {
    return (GraphEvaluationContext) super.addRef();
  }
}
