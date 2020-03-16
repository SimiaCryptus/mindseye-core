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

import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CountingResult extends Result {
  protected static final Logger logger = LoggerFactory.getLogger(CountingResult.class);

  public CountingResult(@Nonnull final Result inner) {
    super(inner.getData(), new CountingAccumulator(inner.getAccumulator()), inner.isAlive());
    inner.freeRef();
  }

  public CountingResult(@Nonnull final Result inner, final int samples, Layer consumer) {
    this(inner);
    Accumulator a = getAccumulator();
    if (a instanceof CountingResult.CountingAccumulator) {
      CountingResult.CountingAccumulator accumulator = (CountingResult.CountingAccumulator) a;
      for (int i = 0; i < samples; i++) {
        accumulator.incrementFwd(consumer.addRef());
      }
      accumulator.freeRef();
    } else {
      assert Result.isNull(a);
      a.freeRef();
    }
    consumer.freeRef();
  }

  @NotNull
  private static StackTraceElement[] getStackTrace() {
    return new StackTraceElement[]{};
//    return Thread.currentThread().getStackTrace();
  }

  public void _free() {
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
    private final RefList<Layer> fwdLinks;
    @Nonnull
    private final RefMap<StackTraceElement[], TensorList> passbackBuffers;
    @Nonnull
    private final List<StackTraceElement[]> accumulations;
    private Accumulator innerAccumulator;

    public CountingAccumulator(Accumulator accumulator) {
      innerAccumulator = accumulator;
      fwdLinks = new RefArrayList<>();
      passbackBuffers = new RefHashMap<>();
      accumulations = new ArrayList<>();
    }

    public int getFwdCount() {
      return this.fwdLinks.size();
    }

    public int incrementFwd(Layer consumer) {
      synchronized (this.fwdLinks) {
        this.fwdLinks.add(consumer);
        return this.fwdLinks.size();
      }
    }

    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      assertAlive();
      data.assertAlive();
      if (1 >= getFwdCount()) {
        accum(buffer, data);
      } else {
        add(buffer, data);
      }
    }

    public void accum(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      innerAccumulator.accept(buffer, data);
    }

    public void _free() {
      super._free();
      if (passbackBuffers.size() > 0 && accumulations.size() > 0) {
        logger.error("Passback incomplete");
      }
      fwdLinks.freeRef();
      if (null != innerAccumulator) innerAccumulator.freeRef();
      passbackBuffers.freeRef();
    }

    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    CountingAccumulator addRef() {
      return (CountingAccumulator) super.addRef();
    }

    private void add(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      //assert allAlive();
      @NotNull StackTraceElement[] stackTrace = getStackTrace();
      RefUtil.freeRef(passbackBuffers.put(stackTrace, data));
      synchronized (passbackBuffers) {
        if (passbackBuffers.size() > CoreSettings.INSTANCE().backpropAggregationSize) {
          RefUtil.freeRef(passbackBuffers.put(stackTrace, reduce()));
          //assert allAlive();
        }
      }
      int expected = getFwdCount();
      int accumulationCount;
      synchronized (accumulations) {
        accumulations.add(stackTrace);
        accumulationCount = accumulations.size() % expected;
      }
      if (accumulationCount == 0) {
        accum(buffer, reduce());
      } else {
        buffer.freeRef();
      }
    }

    private boolean allAlive() {
      synchronized (passbackBuffers) {
        passbackBuffers.forEach((k, v) -> {
          v.assertAlive();
          v.freeRef();
        });
        return true;
      }
    }

    @NotNull
    private TensorList reduce() {
      synchronized (passbackBuffers) {
        RefCollection<TensorList> values = passbackBuffers.values();
        RefStream<TensorList> stream = values.stream();
        if (!CoreSettings.INSTANCE().singleThreaded)
          stream = stream.parallel();
        TensorList reduced = RefUtil.get(stream.reduce((a, b) -> {
          TensorList c = a.addAndFree(b);
          a.freeRef();
          return c;
        }));
        values.freeRef();
        passbackBuffers.clear();
        return reduced;
      }
    }

  }
}
