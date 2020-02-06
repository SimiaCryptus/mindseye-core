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

package com.simiacryptus.mindseye.layers;

import com.google.gson.JsonObject;
import com.simiacryptus.lang.TimedResult;
import com.simiacryptus.lang.UncheckedRunnable;
import com.simiacryptus.lang.UncheckedSupplier;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.util.MonitoredItem;
import com.simiacryptus.util.MonitoredObject;
import com.simiacryptus.util.data.PercentileStatistics;
import com.simiacryptus.util.data.ScalarStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"serial", "FieldCanBeLocal"})
public final class MonitoringWrapperLayer extends WrapperLayer implements MonitoredItem {

  private final PercentileStatistics backwardPerformance = new PercentileStatistics();
  private final ScalarStatistics backwardSignal = new PercentileStatistics();
  private final PercentileStatistics forwardPerformance = new PercentileStatistics();
  private final ScalarStatistics forwardSignal = new PercentileStatistics();
  private final boolean verbose = false;
  private boolean recordSignalMetrics = false;
  private int totalBatches = 0;
  private int totalItems = 0;

  protected MonitoringWrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
    if (json.has("forwardPerf")) {
      forwardPerformance.readJson(json.getAsJsonObject("forwardPerf"));
    }
    if (json.has("backwardPerf")) {
      backwardPerformance.readJson(json.getAsJsonObject("backwardPerf"));
    }
    if (json.has("backpropStatistics")) {
      backwardSignal.readJson(json.getAsJsonObject("backpropStatistics"));
    }
    if (json.has("outputStatistics")) {
      forwardSignal.readJson(json.getAsJsonObject("outputStatistics"));
    }
    recordSignalMetrics = json.get("recordSignalMetrics").getAsBoolean();
    totalBatches = json.get("totalBatches").getAsInt();
    totalItems = json.get("totalItems").getAsInt();
  }

  public MonitoringWrapperLayer(final Layer inner) {
    super(inner);
  }

  @Nonnull
  public PercentileStatistics getBackwardPerformance() {
    return backwardPerformance;
  }

  @Nonnull
  public ScalarStatistics getBackwardSignal() {
    return backwardSignal;
  }

  @Nonnull
  public PercentileStatistics getForwardPerformance() {
    return forwardPerformance;
  }

  @Nonnull
  public ScalarStatistics getForwardSignal() {
    return forwardSignal;
  }

  @Nonnull
  @Override
  public Map<CharSequence, Object> getMetrics() {
    @Nonnull final Map<CharSequence, Object> map = new HashMap<>();
    map.put("class", inner.getClass().getName());
    map.put("totalBatches", totalBatches);
    map.put("totalItems", totalItems);
    map.put("outputStatistics", forwardSignal.getMetrics());
    map.put("backpropStatistics", backwardSignal.getMetrics());
    if (verbose) {
      map.put("forwardPerformance", forwardPerformance.getMetrics());
      map.put("backwardPerformance", backwardPerformance.getMetrics());
    }
    final double batchesPerItem = totalBatches * 1.0 / totalItems;
    map.put("avgMsPerItem", 1000 * batchesPerItem * forwardPerformance.getMean());
    map.put("medianMsPerItem", 1000 * batchesPerItem * forwardPerformance.getPercentile(0.5));
    final double backpropMean = backwardPerformance.getMean();
    final double backpropMedian = backwardPerformance.getPercentile(0.5);
    map.put("avgMsPerItem_Backward", 1000 * batchesPerItem * backpropMean);
    map.put("medianMsPerItem_Backward", 1000 * batchesPerItem * backpropMedian);
    @Nullable final RefList<double[]> state = state();
    @Nonnull final ScalarStatistics statistics = new PercentileStatistics();
    assert state != null;
    state.stream().flatMapToDouble(Arrays::stream).forEach(statistics::add);
    if (statistics.getCount() > 0) {
      @Nonnull final Map<CharSequence, Object> weightStats = new HashMap<>();
      weightStats.put("buffers", state.size());
      weightStats.putAll(statistics.getMetrics());
      map.put("weights", weightStats);
    }
    state.freeRef();
    return map;
  }

  @Nullable
  @Override
  public String getName() {
    return inner.getName();
  }

  @Nonnull
  @SuppressWarnings("unused")
  public static MonitoringWrapperLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new MonitoringWrapperLayer(json, rs);
  }

  @Nonnull
  public MonitoringWrapperLayer addTo2(@Nonnull final MonitoredObject obj) {
    addTo(obj, inner.getName());
    return this.addRef();
  }

  public void addTo(@Nonnull MonitoredObject obj, String name) {
    setName(name);
    obj.addObj(getName(), this.addRef());
    obj.freeRef();
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... inObj) {
    @Nonnull final AtomicLong passbackNanos = new AtomicLong(0);
    final Result[] wrappedInput = RefArrays.stream(RefUtil.addRefs(inObj)).map(result -> {
      try {
        Result.Accumulator accumulator = new Result.Accumulator() {
          {
            result.addRef();
          }

          @Override
          public void accept(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList data) {
            TimedResult<Void> timedResult = TimedResult.time(RefUtil.wrapInterface(
                (UncheckedRunnable<Object>) () -> result.accumulate(
                    buffer == null ? null : buffer.addRef(),
                    data == null ? null : data.addRef()),
                result.addRef(), data, buffer));
            passbackNanos.addAndGet(timedResult.timeNanos);
            timedResult.freeRef();
          }

          public @SuppressWarnings("unused")
          void _free() {
            super._free();
            result.freeRef();
          }
        };
        return new Result(result.getData(), accumulator) {
          {
            result.addRef();
          }

          @Override
          public boolean isAlive() {
            return result.isAlive();
          }

          @Override
          public void _free() {
            result.freeRef();
            super._free();
          }
        };
      } finally {
        if (null != result)
          result.freeRef();
      }
    }).toArray(Result[]::new);
    @Nonnull
    TimedResult<Result> timedResult = TimedResult.time(RefUtil.wrapInterface((UncheckedSupplier<Result>) () -> {
      return inner.eval(RefUtil.addRefs(wrappedInput));
    }, wrappedInput));
    final Result output = timedResult.getResult();
    forwardPerformance.add(timedResult.timeNanos / 1000000000.0);
    timedResult.freeRef();
    totalBatches++;
    final int items = RefArrays.stream(inObj).mapToInt(x -> {
      TensorList temp_31_0009 = x.getData();
      int temp_31_0003 = temp_31_0009.length();
      temp_31_0009.freeRef();
      x.freeRef();
      return temp_31_0003;
    }).max().orElse(1);
    totalItems += items;
    if (recordSignalMetrics) {
      forwardSignal.clear();
      TensorList temp_31_0010 = output.getData();
      temp_31_0010.stream().parallel().forEach(t -> {
        forwardSignal.add(t.getData());
        t.freeRef();
      });
      temp_31_0010.freeRef();
    }
    try {
      Result.Accumulator accumulator = new Result.Accumulator() {
        {
          output.addRef();
        }

        @Override
        public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
          if (recordSignalMetrics) {
            backwardSignal.clear();
            data.stream().parallel().forEach(t -> {
              backwardSignal.add(t.getData());
              t.freeRef();
            });
          }
          TimedResult<Void> timedResult1 = TimedResult
              .time(RefUtil.wrapInterface(
                  (UncheckedRunnable<Object>) () -> output.accumulate(buffer == null ? null : buffer.addRef(),
                      data.addRef()),
                  data, output.addRef(),
                  buffer));
          backwardPerformance.add((timedResult1.timeNanos
              - passbackNanos.getAndSet(0)) / (items * 1e9));
          timedResult1.freeRef();
        }

        public @SuppressWarnings("unused")
        void _free() {
          super._free();
          output.freeRef();
        }
      };
      return new Result(output.getData(), accumulator) {
        {
          output.addRef();
        }

        @Override
        public boolean isAlive() {
          return output.isAlive();
        }

        @Override
        public void _free() {
          output.freeRef();
          super._free();
        }
      };
    } finally {
      output.freeRef();
    }
  }

  @Nonnull
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    @Nonnull final JsonObject json = super.getJson(resources, dataSerializer);
    //json.fn("forwardPerf",forwardPerf.getJson());
    //json.fn("backwardPerf",backwardPerf.getJson());
    json.addProperty("totalBatches", totalBatches);
    json.addProperty("totalItems", totalItems);
    json.addProperty("recordSignalMetrics", recordSignalMetrics);
    return json;
  }

  @Nonnull
  @Override
  public void setName(final String name) {
    if (null != inner) {
      inner.setName(name);
    }
  }

  public void shouldRecordSignalMetrics(boolean recordSignalMetrics) {
    this.recordSignalMetrics = recordSignalMetrics;
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  MonitoringWrapperLayer addRef() {
    return (MonitoringWrapperLayer) super.addRef();
  }
}
