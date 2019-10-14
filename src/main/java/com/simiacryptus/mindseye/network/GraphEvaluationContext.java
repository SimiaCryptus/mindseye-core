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

import com.simiacryptus.lang.ref.ReferenceCountingBase;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

class GraphEvaluationContext extends ReferenceCountingBase {

  final Map<UUID, Long> expectedCounts = new ConcurrentHashMap<>();

  final Map<UUID, Supplier<CountingResult>> calculated = new ConcurrentHashMap<>();

  @Override
  protected synchronized void _free() {
    calculated.entrySet().stream().filter(entry -> {
      Object o = entry.getValue().get();
      if (o instanceof RuntimeException) throw (RuntimeException) o;
      if (o instanceof Throwable) throw new RuntimeException((Throwable) o);
      CountingResult countingNNResult = (CountingResult) o;
      if (expectedCounts.containsKey(entry.getKey())) {
        return expectedCounts.get(entry.getKey()) > countingNNResult.getAccumulator().getCount();
      } else {
        return true;
      }
    }).forEach(entry -> {
      CountingResult result = entry.getValue().get();
      result.freeRef();
      result.getData().freeRef();
    });
    calculated.clear();
  }
}
