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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefLinkedList;
import com.simiacryptus.ref.wrappers.RefStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingResult extends Result {
  protected static final Logger logger = LoggerFactory.getLogger(CountingResult.class);

  @Nonnull
  private final Result inner;

  public CountingResult(@Nonnull final Result inner) {
    super(inner.getData(), new CountingAccumulator(inner.addRef()));
    Result temp_09_0001 = inner.addRef();
    this.inner = temp_09_0001.addRef();
    temp_09_0001.freeRef();
    inner.freeRef();
  }

  public CountingResult(@Nonnull final Result r, final int samples) {
    this(r);
    CountingResult.CountingAccumulator temp_09_0007 = getAccumulator();
    temp_09_0007.fwdLinks.set(samples);
    temp_09_0007.freeRef();
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

  public void _free() {
    inner.freeRef();
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  CountingResult addRef() {
    return (CountingResult) super.addRef();
  }

  static class CountingAccumulator extends Result.Accumulator {
    @Nonnull
    private final AtomicInteger fwdLinks;
    @Nullable
    private final Result inner;
    @Nonnull
    private final RefLinkedList<TensorList> passbackBuffers;
    @Nonnull
    private final AtomicInteger accumulations;

    public CountingAccumulator(@Nullable Result inner) {
      Result temp_09_0002 = inner == null ? null : inner.addRef();
      this.inner = temp_09_0002 == null ? null : temp_09_0002.addRef();
      if (null != temp_09_0002)
        temp_09_0002.freeRef();
      if (null != inner)
        inner.freeRef();
      fwdLinks = new AtomicInteger(0);
      RefLinkedList<TensorList> temp_09_0003 = new RefLinkedList<>();
      passbackBuffers = temp_09_0003.addRef();
      temp_09_0003.freeRef();
      accumulations = new AtomicInteger(0);
    }

    public int getCount() {
      return this.fwdLinks.get();
    }

    @Nullable
    public static @SuppressWarnings("unused")
    CountingAccumulator[] addRefs(@Nullable CountingAccumulator[] array) {
      if (array == null)
        return null;
      return Arrays.stream(array).filter((x) -> x != null).map(CountingAccumulator::addRef)
          .toArray((x) -> new CountingAccumulator[x]);
    }

    public int increment() {
      return this.fwdLinks.incrementAndGet();
    }

    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      //assert null == CudaSystem.getThreadHandle();
      assertAlive();
      data.assertAlive();
      if (1 >= fwdLinks.get()) {
        assert inner != null;
        inner.accumulate(buffer == null ? null : buffer.addRef(), data.addRef());
      } else {
        @Nonnull
        TensorList reduced = null;
        synchronized (passbackBuffers) {
          assert passbackBuffers.stream().allMatch(x -> {
            boolean temp_09_0004 = x.assertAlive();
            x.freeRef();
            return temp_09_0004;
          });
          passbackBuffers.add(data.addRef());
          if (passbackBuffers.size() > CoreSettings.INSTANCE().backpropAggregationSize) {
            RefStream<TensorList> stream = passbackBuffers.stream();
            if (!CoreSettings.INSTANCE().isSingleThreaded())
              stream = stream.parallel();
            @Nonnull
            TensorList compacted = RefUtil.get(stream.reduce((a, b) -> {
              TensorList c = a.addAndFree(b == null ? null : b.addRef());
              if (null != b)
                b.freeRef();
              a.freeRef();
              return c;
            }));
            passbackBuffers.clear();
            passbackBuffers.add(compacted);
            assert passbackBuffers.stream().allMatch(x -> {
              boolean temp_09_0005 = x.assertAlive();
              x.freeRef();
              return temp_09_0005;
            });
          }
          if (accumulations.incrementAndGet() == fwdLinks.get()) {
            RefStream<TensorList> stream = passbackBuffers.stream();
            if (!CoreSettings.INSTANCE().isSingleThreaded())
              stream = stream.parallel();
            if (null != reduced) reduced.freeRef();
            reduced = RefUtil.get(stream.reduce((a, b) -> {
              TensorList c = a.addAndFree(b == null ? null : b.addRef());
              if (null != b)
                b.freeRef();
              a.freeRef();
              return c;
            }));
            passbackBuffers.clear();
          }
          assert passbackBuffers.stream().allMatch(x -> {
            boolean temp_09_0006 = x.assertAlive();
            x.freeRef();
            return temp_09_0006;
          });
        }
        if (null != reduced) {
          assert inner != null;
          inner.accumulate(buffer == null ? null : buffer.addRef(), reduced.addRef());
          accumulations.set(0);
        }
        assert reduced != null;
        reduced.freeRef();
      }
      data.freeRef();
      if (null != buffer)
        buffer.freeRef();
    }

    public void _free() {
      if (null != inner)
        inner.freeRef();
      synchronized (passbackBuffers) {
        passbackBuffers.clear();
        passbackBuffers.freeRef();
      }
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    CountingAccumulator addRef() {
      return (CountingAccumulator) super.addRef();
    }
  }
}
