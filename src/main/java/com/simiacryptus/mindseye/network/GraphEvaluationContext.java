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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConcurrentHashMap;
import com.simiacryptus.ref.wrappers.RefMap;
import com.simiacryptus.ref.wrappers.RefSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

class GraphEvaluationContext extends ReferenceCountingBase {

  final RefMap<UUID, Long> expectedCounts = new RefConcurrentHashMap<>();

  final RefMap<UUID, Supplier<CountingResult>> calculated = new RefConcurrentHashMap<>();

  @Nullable
  public static @SuppressWarnings("unused")
  GraphEvaluationContext[] addRefs(@Nullable GraphEvaluationContext[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(GraphEvaluationContext::addRef)
        .toArray((x) -> new GraphEvaluationContext[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  GraphEvaluationContext[][] addRefs(@Nullable GraphEvaluationContext[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(GraphEvaluationContext::addRefs)
        .toArray((x) -> new GraphEvaluationContext[x][]);
  }

  public void _free() {
    RefSet<Map.Entry<UUID, Supplier<CountingResult>>> temp_43_0004 = calculated.entrySet();
    temp_43_0004.stream().filter(entry -> {
      ReferenceCounting o = entry.getValue().get();
      CountingResult countingNNResult = (CountingResult) o;
      if (null != o)
        o.freeRef();
      if (expectedCounts.containsKey(entry.getKey())) {
        CountingResult.CountingAccumulator temp_43_0005 = countingNNResult.getAccumulator();
        boolean temp_43_0001 = expectedCounts.get(entry.getKey()) > temp_43_0005.getCount();
        temp_43_0005.freeRef();
        RefUtil.freeRef(entry);
        countingNNResult.freeRef();
        return temp_43_0001;
      } else {
        RefUtil.freeRef(entry);
        if (null != countingNNResult)
          countingNNResult.freeRef();
        return true;
      }
    }).forEach(entry -> {
      CountingResult result = entry.getValue().get();
      RefUtil.freeRef(entry);
      RefUtil.freeRef(result.getData());
      result.freeRef();
    });
    temp_43_0004.freeRef();
    expectedCounts.freeRef();
    calculated.clear();
    calculated.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  GraphEvaluationContext addRef() {
    return (GraphEvaluationContext) super.addRef();
  }
}
