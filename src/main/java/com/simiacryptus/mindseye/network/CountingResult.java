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

import com.simiacryptus.mindseye.lang.CoreSettings;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.mindseye.lang.TensorList;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public @com.simiacryptus.ref.lang.RefAware
class CountingResult extends Result {
  protected static final Logger logger = LoggerFactory.getLogger(CountingResult.class);

  @Nonnull
  private final Result inner;

  public CountingResult(@Nonnull final Result inner) {
    super(inner.getData(), new CountingAccumulator(inner));
    this.inner = inner;
  }

  public CountingResult(final Result r, final int samples) {
    this(r);
    getAccumulator().fwdLinks.set(samples);
  }

  @Nonnull
  @Override
  public CountingAccumulator getAccumulator() {
    return (CountingAccumulator) super.getAccumulator();
  }

  @Override
  public boolean isAlive() {
    return inner.isAlive();
  }

  public static @SuppressWarnings("unused")
  CountingResult[] addRefs(CountingResult[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(CountingResult::addRef)
        .toArray((x) -> new CountingResult[x]);
  }

  public static @SuppressWarnings("unused")
  CountingResult[][] addRefs(CountingResult[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(CountingResult::addRefs)
        .toArray((x) -> new CountingResult[x][]);
  }

  public void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  CountingResult addRef() {
    return (CountingResult) super.addRef();
  }

  static @com.simiacryptus.ref.lang.RefAware
  class CountingAccumulator extends ReferenceCountingBase
      implements BiConsumer<DeltaSet<UUID>, TensorList> {
    @Nonnull
    private final AtomicInteger fwdLinks;
    private final Result inner;
    @Nonnull
    private final com.simiacryptus.ref.wrappers.RefLinkedList<TensorList> passbackBuffers;
    @Nonnull
    private final AtomicInteger accumulations;

    public CountingAccumulator(Result inner) {
      this.inner = inner;
      fwdLinks = new AtomicInteger(0);
      passbackBuffers = new com.simiacryptus.ref.wrappers.RefLinkedList<>();
      accumulations = new AtomicInteger(0);
    }

    public int getCount() {
      return this.fwdLinks.get();
    }

    public static @SuppressWarnings("unused")
    CountingAccumulator[] addRefs(CountingAccumulator[] array) {
      if (array == null)
        return null;
      return java.util.Arrays.stream(array).filter((x) -> x != null).map(CountingAccumulator::addRef)
          .toArray((x) -> new CountingAccumulator[x]);
    }

    public int increment() {
      return this.fwdLinks.incrementAndGet();
    }

    @Override
    public void accept(DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      //assert null == CudaSystem.getThreadHandle();
      assertAlive();
      data.assertAlive();
      if (1 >= fwdLinks.get()) {
        inner.accumulate(buffer, data);
      } else {
        @Nonnull
        TensorList reduced = null;
        synchronized (passbackBuffers) {
          assert passbackBuffers.stream().allMatch(x -> x.assertAlive());
          passbackBuffers.add(data);
          if (passbackBuffers.size() > CoreSettings.INSTANCE().backpropAggregationSize) {
            com.simiacryptus.ref.wrappers.RefStream<TensorList> stream = passbackBuffers.stream();
            if (!CoreSettings.INSTANCE().isSingleThreaded())
              stream = stream.parallel();
            @Nonnull
            TensorList compacted = stream.reduce((a, b) -> {
              TensorList c;
              c = a.addAndFree(b);
              return c;
            }).get();
            passbackBuffers.clear();
            passbackBuffers.add(compacted);
            assert passbackBuffers.stream().allMatch(x -> x.assertAlive());
          }
          if (accumulations.incrementAndGet() == fwdLinks.get()) {
            com.simiacryptus.ref.wrappers.RefStream<TensorList> stream = passbackBuffers.stream();
            if (!CoreSettings.INSTANCE().isSingleThreaded())
              stream = stream.parallel();
            reduced = stream.reduce((a, b) -> {
              TensorList c;
              c = a.addAndFree(b);
              return c;
            }).get();
            passbackBuffers.clear();
          }
          assert passbackBuffers.stream().allMatch(x -> x.assertAlive());
        }
        if (null != reduced) {
          inner.accumulate(buffer, reduced);
          accumulations.set(0);
        }
      }
    }

    public void _free() {
      synchronized (passbackBuffers) {
        passbackBuffers.clear();
      }
    }

    public @Override
    @SuppressWarnings("unused")
    CountingAccumulator addRef() {
      return (CountingAccumulator) super.addRef();
    }

  }
}
