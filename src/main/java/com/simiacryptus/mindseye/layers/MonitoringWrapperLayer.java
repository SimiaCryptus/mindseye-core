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
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.util.MonitoredItem;
import com.simiacryptus.util.MonitoredObject;
import com.simiacryptus.util.data.PercentileStatistics;
import com.simiacryptus.util.data.ScalarStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
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

  public static MonitoringWrapperLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new MonitoringWrapperLayer(json, rs);
  }

  @Nonnull
  public MonitoringWrapperLayer addTo(@Nonnull final MonitoredObject obj) {
    return addTo(obj, getInner().getName());
  }

  @Nonnull
  public MonitoringWrapperLayer addTo(@Nonnull final MonitoredObject obj, final String name) {
    setName(name);
    obj.addObj(getName(), this);
    return this;
  }

  @Override
  public Result evalAndFree(@Nonnull final Result... inObj) {
    @Nonnull final AtomicLong passbackNanos = new AtomicLong(0);
    final Result[] wrappedInput = Arrays.stream(inObj).map(result -> {
      return new Result(result.getData(), (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList data) -> {
        passbackNanos.addAndGet(TimedResult.time(() -> result.accumulate(buffer, data)).timeNanos);
      }) {

        @Override
        protected void _free() {
          result.freeRef();
        }


        @Override
        public boolean isAlive() {
          return result.isAlive();
        }
      };
    }).toArray(i -> new Result[i]);
    @Nonnull TimedResult<Result> timedResult = TimedResult.time(() -> getInner().evalAndFree(wrappedInput));
    final Result output = timedResult.result;
    forwardPerformance.add((timedResult.timeNanos) / 1000000000.0);
    totalBatches++;
    final int items = Arrays.stream(inObj).mapToInt(x -> x.getData().length()).max().orElse(1);
    totalItems += items;
    if (recordSignalMetrics) {
      forwardSignal.clear();
      output.getData().stream().parallel().forEach(t -> {
        forwardSignal.add(t.getData());
        t.freeRef();
      });
    }
    return new Result(output.getData(), (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList data) -> {
      if (recordSignalMetrics) {
        backwardSignal.clear();
        data.stream().parallel().forEach(t -> {
          backwardSignal.add(t.getData());
          t.freeRef();
        });
      }
      backwardPerformance.add((TimedResult.time(() -> output.accumulate(buffer, data)).timeNanos - passbackNanos.getAndSet(0)) / (items * 1e9));
    }) {

      @Override
      protected void _free() {
        output.freeRef();
      }

      @Override
      public boolean isAlive() {
        return output.isAlive();
      }
    };
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
  public Map<CharSequence, Object> getMetrics() {
    @Nonnull final HashMap<CharSequence, Object> map = new HashMap<>();
    map.put("class", getInner().getClass().getName());
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
    @Nullable final List<double[]> state = state();
    @Nonnull final ScalarStatistics statistics = new PercentileStatistics();
    for (@Nonnull final double[] s : state) {
      for (final double v : s) {
        statistics.add(v);
      }
    }
    if (statistics.getCount() > 0) {
      @Nonnull final HashMap<CharSequence, Object> weightStats = new HashMap<>();
      weightStats.put("buffers", state.size());
      weightStats.putAll(statistics.getMetrics());
      map.put("weights", weightStats);
    }
    return map;
  }

  @Nullable
  @Override
  public String getName() {
    return getInner().getName();
  }

  public boolean recordSignalMetrics() {
    return recordSignalMetrics;
  }

  @Nonnull
  @Override
  public Layer setName(final String name) {
    if (null != getInner()) {
      getInner().setName(name);
    }
    return this;
  }

  @Nonnull
  public MonitoringWrapperLayer shouldRecordSignalMetrics(final boolean recordSignalMetrics) {
    this.recordSignalMetrics = recordSignalMetrics;
    return this;
  }
}
