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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefHashMap;
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
public final @RefAware
class MonitoringWrapperLayer extends WrapperLayer implements MonitoredItem {

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
    if (null != inner)
      inner.freeRef();
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
    Layer temp_31_0005 = getInner();
    map.put("class", temp_31_0005.getClass().getName());
    if (null != temp_31_0005)
      temp_31_0005.freeRef();
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
    for (@Nonnull final double[] s : state) {
      for (final double v : s) {
        statistics.add(v);
      }
    }
    if (statistics.getCount() > 0) {
      @Nonnull final Map<CharSequence, Object> weightStats = new HashMap<>();
      weightStats.put("buffers", state.size());
      weightStats.putAll(statistics.getMetrics());
      map.put("weights", weightStats);
    }
    if (null != state)
      state.freeRef();
    return map;
  }

  @Nullable
  @Override
  public String getName() {
    Layer temp_31_0007 = getInner();
    String temp_31_0006 = temp_31_0007.getName();
    if (null != temp_31_0007)
      temp_31_0007.freeRef();
    return temp_31_0006;
  }

  @SuppressWarnings("unused")
  public static MonitoringWrapperLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new MonitoringWrapperLayer(json, rs);
  }

  public static @SuppressWarnings("unused")
  MonitoringWrapperLayer[] addRefs(MonitoringWrapperLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MonitoringWrapperLayer::addRef)
        .toArray((x) -> new MonitoringWrapperLayer[x]);
  }

  public static @SuppressWarnings("unused")
  MonitoringWrapperLayer[][] addRefs(MonitoringWrapperLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(MonitoringWrapperLayer::addRefs)
        .toArray((x) -> new MonitoringWrapperLayer[x][]);
  }

  @Nonnull
  public MonitoringWrapperLayer addTo(@Nonnull final MonitoredObject obj) {
    Layer temp_31_0008 = getInner();
    MonitoringWrapperLayer temp_31_0004 = addTo(obj == null ? null : obj,
        temp_31_0008.getName());
    if (null != temp_31_0008)
      temp_31_0008.freeRef();
    return temp_31_0004;
  }

  @Nonnull
  public MonitoringWrapperLayer addTo(@Nonnull final MonitoredObject obj, final String name) {
    RefUtil.freeRef(setName(name));
    RefUtil.freeRef(obj.addObj(getName(), this.addRef()));
    obj.freeRef();
    return this.addRef();
  }

  @Override
  public Result eval(@Nonnull final Result... inObj) {
    @Nonnull final AtomicLong passbackNanos = new AtomicLong(0);
    final Result[] wrappedInput = RefArrays.stream(Result.addRefs(inObj)).map(result -> {
      try {
        return new Result(result.getData(), new Result.Accumulator() {
          {
          }

          @Override
          public void accept(DeltaSet<UUID> buffer, TensorList data) {
            passbackNanos.addAndGet(TimedResult.time(RefUtil.wrapInterface(
                (UncheckedRunnable<Object>) () -> result
                    .accumulate(buffer == null ? null : buffer.addRef(), data == null ? null : data.addRef()),
                result == null ? null : result.addRef(), data == null ? null : data.addRef(),
                buffer == null ? null : buffer.addRef())).timeNanos);
            if (null != data)
              data.freeRef();
            if (null != buffer)
              buffer.freeRef();
          }

          public @SuppressWarnings("unused")
          void _free() {
          }
        }) {

          {
          }

          @Override
          public boolean isAlive() {
            return result.isAlive();
          }

          public void _free() {
          }
        };
      } finally {
        if (null != result)
          result.freeRef();
      }
    }).toArray(i -> new Result[i]);
    @Nonnull
    TimedResult<Result> timedResult = TimedResult.time(RefUtil
        .wrapInterface((UncheckedSupplier<Result>) () -> {
          return getInner().eval(Result.addRefs(wrappedInput));
        }, Result.addRefs(wrappedInput)));
    if (null != wrappedInput)
      ReferenceCounting.freeRefs(wrappedInput);
    final Result output = timedResult.result.addRef();
    forwardPerformance.add((timedResult.timeNanos) / 1000000000.0);
    totalBatches++;
    final int items = RefArrays.stream(Result.addRefs(inObj)).mapToInt(x -> {
      TensorList temp_31_0009 = x.getData();
      int temp_31_0003 = temp_31_0009.length();
      if (null != temp_31_0009)
        temp_31_0009.freeRef();
      if (null != x)
        x.freeRef();
      return temp_31_0003;
    }).max().orElse(1);
    ReferenceCounting.freeRefs(inObj);
    totalItems += items;
    if (recordSignalMetrics) {
      forwardSignal.clear();
      TensorList temp_31_0010 = output.getData();
      temp_31_0010.stream().parallel().forEach(t -> {
        forwardSignal.add(t.getData());
        if (null != t)
          t.freeRef();
      });
      if (null != temp_31_0010)
        temp_31_0010.freeRef();
    }
    try {
      return new Result(output.getData(), new Result.Accumulator() {
        {
        }

        @Override
        public void accept(DeltaSet<UUID> buffer, TensorList data) {
          if (recordSignalMetrics) {
            backwardSignal.clear();
            data.stream().parallel().forEach(t -> {
              backwardSignal.add(t.getData());
              if (null != t)
                t.freeRef();
            });
          }
          backwardPerformance.add((TimedResult.time(RefUtil.wrapInterface(
              (UncheckedRunnable<Object>) () -> output
                  .accumulate(buffer == null ? null : buffer.addRef(), data == null ? null : data.addRef()),
              data == null ? null : data.addRef(), output == null ? null : output.addRef(),
              buffer == null ? null : buffer.addRef())).timeNanos - passbackNanos.getAndSet(0)) / (items * 1e9));
          if (null != data)
            data.freeRef();
          if (null != buffer)
            buffer.freeRef();
        }

        public @SuppressWarnings("unused")
        void _free() {
        }
      }) {

        {
        }

        @Override
        public boolean isAlive() {
          return output.isAlive();
        }

        public void _free() {
        }
      };
    } finally {
      if (null != output)
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
  public Layer setName(final String name) {
    Layer temp_31_0011 = getInner();
    if (null != temp_31_0011) {
      Layer temp_31_0012 = getInner();
      RefUtil.freeRef(temp_31_0012.setName(name));
      if (null != temp_31_0012)
        temp_31_0012.freeRef();
    }
    if (null != temp_31_0011)
      temp_31_0011.freeRef();
    return this.addRef();
  }

  @Nonnull
  public MonitoringWrapperLayer shouldRecordSignalMetrics(final boolean recordSignalMetrics) {
    this.recordSignalMetrics = recordSignalMetrics;
    return this.addRef();
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  MonitoringWrapperLayer addRef() {
    return (MonitoringWrapperLayer) super.addRef();
  }
}
