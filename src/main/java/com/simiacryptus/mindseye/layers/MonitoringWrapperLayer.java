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

/**
 * MonitoringWrapperLayer is a class that monitors the performance of a system.
 *
 * @param backwardPerformance the performance of the system when going backwards
 * @param backwardSignal      the signal strength of the system when going backwards
 * @param forwardPerformance  the performance of the system when going forwards
 * @param forwardSignal       the signal strength of the system when going forwards
 * @param verbose             whether or not to print debug information
 * @param recordSignalMetrics whether or not to record signal metrics
 * @param totalBatches        the total number of batches processed
 * @param totalItems          the total number of items processed
 * @docgenVersion 9
 */
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

  /**
   * Instantiates a new Monitoring wrapper layer.
   *
   * @param json the json
   * @param rs   the rs
   */
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

  /**
   * Instantiates a new Monitoring wrapper layer.
   *
   * @param inner the inner
   */
  public MonitoringWrapperLayer(final Layer inner) {
    super(inner);
  }

  /**
   * @return the backwardPerformance
   * @docgenVersion 9
   */
  @Nonnull
  public PercentileStatistics getBackwardPerformance() {
    return backwardPerformance;
  }

  /**
   * Returns the backward signal.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public ScalarStatistics getBackwardSignal() {
    return backwardSignal;
  }

  /**
   * @return the forwardPerformance
   * @docgenVersion 9
   */
  @Nonnull
  public PercentileStatistics getForwardPerformance() {
    return forwardPerformance;
  }

  /**
   * @return the forwardSignal
   * @docgenVersion 9
   */
  @Nonnull
  public ScalarStatistics getForwardSignal() {
    return forwardSignal;
  }

  /**
   * Returns a map of metrics. This method cannot return null.
   *
   * @docgenVersion 9
   */
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

  /**
   * @return the name
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public String getName() {
    return inner.getName();
  }

  /**
   * @Override public void setName(final String name) {
   * if (null != inner) {
   * inner.setName(name);
   * }
   * }
   * @docgenVersion 9
   */
  @Override
  public void setName(final String name) {
    if (null != inner) {
      inner.setName(name);
    }
  }

  /**
   * Creates a new MonitoringWrapperLayer from the given JSON object and resource map.
   *
   * @param json the JSON object to create the layer from
   * @param rs   the resource map to use
   * @return the new MonitoringWrapperLayer
   * @docgenVersion 9
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static MonitoringWrapperLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new MonitoringWrapperLayer(json, rs);
  }

  /**
   * Adds the given monitored object to this layer with the name of this layer's inner object.
   *
   * @param obj the monitored object to add
   * @return this layer
   * @docgenVersion 9
   */
  @Nonnull
  public MonitoringWrapperLayer addTo2(@Nonnull final MonitoredObject obj) {
    addTo(obj, inner.getName());
    return this.addRef();
  }

  /**
   * Adds this object to the specified monitored object with the given name.
   *
   * @param obj  the monitored object to add this object to
   * @param name the name to use for this object in the monitored object
   * @docgenVersion 9
   */
  public void addTo(@Nonnull MonitoredObject obj, String name) {
    setName(name);
    obj.addObj(getName(), this.addRef());
    obj.freeRef();
  }

  /**
   * @param inObj the input objects
   * @return the result of the evaluation
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... inObj) {
    @Nonnull final AtomicLong passbackNanos = new AtomicLong(0);
    final Result[] wrappedInput = RefArrays.stream(RefUtil.addRef(inObj)).map(result -> {
      boolean alive = result.isAlive();
      TensorList data = result.getData();
      Result.Accumulator accumulator = new Accumulator(passbackNanos, result.getAccumulator());
      result.freeRef();
      return new Result(data, accumulator, alive);
    }).toArray(Result[]::new);
    @Nonnull
    TimedResult<Result> timedResult = TimedResult.time(RefUtil.wrapInterface((UncheckedSupplier<Result>) () -> {
      return inner.eval(RefUtil.addRef(wrappedInput));
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
    boolean alive = output.isAlive();
    TensorList data = output.getData();
    Result.Accumulator accumulator = new Accumulator2(passbackNanos, items, output.getAccumulator());
    output.freeRef();
    return new Result(data, accumulator, alive);
  }

  /**
   * @Nonnull
   * @Override public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer);
   * @docgenVersion 9
   */
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

  /**
   * Sets whether signal metrics should be recorded.
   *
   * @param recordSignalMetrics true if signal metrics should be recorded, false otherwise
   * @docgenVersion 9
   */
  public void shouldRecordSignalMetrics(boolean recordSignalMetrics) {
    this.recordSignalMetrics = recordSignalMetrics;
  }

  /**
   * This method is unused.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  /**
   * @return the MonitoringWrapperLayer object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  MonitoringWrapperLayer addRef() {
    return (MonitoringWrapperLayer) super.addRef();
  }

  /**
   * This class represents an Accumulator, which is used to
   * track the amount of time spent in a given task.
   *
   * @param passbackNanos The amount of time, in nanoseconds, spent in the task.
   * @param accumulator   The Result.Accumulator object used to track the task's progress.
   * @docgenVersion 9
   */
  private static class Accumulator extends Result.Accumulator {

    private final AtomicLong passbackNanos;
    private Result.Accumulator accumulator;

    /**
     * Instantiates a new Accumulator.
     *
     * @param passbackNanos the passback nanos
     * @param accumulator   the accumulator
     */
    public Accumulator(AtomicLong passbackNanos, Result.Accumulator accumulator) {
      this.passbackNanos = passbackNanos;
      this.accumulator = accumulator;
    }

    /**
     * @Override public void accept(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList data);
     * @docgenVersion 9
     */
    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nullable TensorList data) {
      TimedResult<Void> timedResult = TimedResult.time(RefUtil.wrapInterface(
          (UncheckedRunnable<Object>) () -> {
            this.accumulator.accept(buffer.addRef(), data.addRef());
          }, data, buffer));
      passbackNanos.addAndGet(timedResult.timeNanos);
      timedResult.freeRef();
    }

    /**
     * This method frees resources used by this object.
     *
     * @docgenVersion 9
     */
    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      accumulator.freeRef();
    }
  }

  /**
   * This class is an accumulator that uses an AtomicLong to keep track of passback nanoseconds.
   * It also has a field for the number of items, as well as an accumulator field.
   *
   * @docgenVersion 9
   */
  private class Accumulator2 extends Result.Accumulator {

    private final AtomicLong passbackNanos;
    private final int items;
    private Result.Accumulator accumulator;

    /**
     * Instantiates a new Accumulator 2.
     *
     * @param passbackNanos the passback nanos
     * @param items         the items
     * @param accumulator   the accumulator
     */
    public Accumulator2(AtomicLong passbackNanos, int items, Result.Accumulator accumulator) {
      this.passbackNanos = passbackNanos;
      this.items = items;
      this.accumulator = accumulator;
    }

    /**
     * @Override public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data);
     * @docgenVersion 9
     */
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
              (UncheckedRunnable<Object>) () -> {
                DeltaSet<UUID> buffer1 = buffer == null ? null : buffer.addRef();
                Result.Accumulator accumulator = this.accumulator;
                try {
                  accumulator.accept(buffer1, data.addRef());
                } finally {
                  accumulator.freeRef();
                }
              },
              data, accumulator.addRef(),
              buffer));
      backwardPerformance.add((timedResult1.timeNanos
          - passbackNanos.getAndSet(0)) / (items * 1e9));
      timedResult1.freeRef();
    }

    /**
     * This method frees resources used by this object.
     *
     * @docgenVersion 9
     */
    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      accumulator.freeRef();
    }
  }
}
