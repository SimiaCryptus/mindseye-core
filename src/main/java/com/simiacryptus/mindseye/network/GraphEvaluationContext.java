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

import java.util.UUID;
import java.util.function.Supplier;

@com.simiacryptus.ref.lang.RefAware
class GraphEvaluationContext extends ReferenceCountingBase {

  final com.simiacryptus.ref.wrappers.RefMap<UUID, Long> expectedCounts = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();

  final com.simiacryptus.ref.wrappers.RefMap<UUID, Supplier<CountingResult>> calculated = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();

  public static @SuppressWarnings("unused")
  GraphEvaluationContext[] addRefs(GraphEvaluationContext[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(GraphEvaluationContext::addRef)
        .toArray((x) -> new GraphEvaluationContext[x]);
  }

  public static @SuppressWarnings("unused")
  GraphEvaluationContext[][] addRefs(GraphEvaluationContext[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(GraphEvaluationContext::addRefs)
        .toArray((x) -> new GraphEvaluationContext[x][]);
  }

  public void _free() {
    calculated.entrySet().stream().filter(entry -> {
      com.simiacryptus.ref.lang.ReferenceCounting o = entry.getValue().get();
      if (o instanceof RuntimeException)
        throw (RuntimeException) o;
      if (o instanceof Throwable)
        throw new RuntimeException((Throwable) o);
      CountingResult countingNNResult = (CountingResult) o;
      if (expectedCounts.containsKey(entry.getKey())) {
        return expectedCounts.get(entry.getKey()) > countingNNResult.getAccumulator().getCount();
      } else {
        return true;
      }
    }).forEach(entry -> {
      CountingResult result = entry.getValue().get();
      result.getData();
    });
    calculated.clear();
  }

  public @Override
  @SuppressWarnings("unused")
  GraphEvaluationContext addRef() {
    return (GraphEvaluationContext) super.addRef();
  }
}
