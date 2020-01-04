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

package com.simiacryptus.mindseye.eval;

import com.simiacryptus.lang.TimedResult;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.layers.PlaceholderLayer;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;
import java.util.stream.IntStream;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;

public @com.simiacryptus.ref.lang.RefAware class TensorListTrainable extends ReferenceCountingBase
    implements TrainableDataMask {

  protected final Layer network;
  @Nullable
  protected TensorList[] data;

  @Nullable
  boolean[] mask = null;
  private int verbosity = 0;

  public TensorListTrainable(final Layer network, @org.jetbrains.annotations.Nullable final TensorList... data) {
    this.network = network;
    this.data = data;
  }

  @Nonnull
  public TensorList[] getData() {
    return data;
  }

  @NotNull
  @Override
  public Layer getLayer() {
    return network;
  }

  @Nullable
  @Override
  public boolean[] getMask() {
    return mask;
  }

  @Nonnull
  public TensorListTrainable setVerbosity(final int verbose) {
    verbosity = verbose;
    return this;
  }

  public static Result[] getNNContext(@Nullable final TensorList[] data, @Nullable final boolean[] mask) {
    if (null == data)
      throw new IllegalArgumentException();
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    return com.simiacryptus.ref.wrappers.RefIntStream.range(0, inputs).mapToObj(col -> {
      final Tensor[] tensors = com.simiacryptus.ref.wrappers.RefIntStream.range(0, items)
          .mapToObj(row -> data[col].get(row)).toArray(i -> new Tensor[i]);
      @Nonnull
      TensorArray tensorArray = new TensorArray(tensors);
      if (null == mask || col >= mask.length || !mask[col]) {
        return new ConstantResult(tensorArray);
      } else {
        return new Result(tensorArray, (@Nonnull final DeltaSet<UUID> buffer, @Nonnull final TensorList delta) -> {
          for (int index = 0; index < delta.length(); index++) {
            final Tensor dt = delta.get(index);
            @Nullable
            final double[] d = dt.getData();
            final Tensor t = tensors[index];
            @Nullable
            final double[] p = t.getData();
            @Nonnull
            PlaceholderLayer<double[]> layer = new PlaceholderLayer<>(p);
            buffer.get(layer.getId(), p).addInPlace(d);
          }
        }) {

          @Override
          public boolean isAlive() {
            return true;
          }

          public @SuppressWarnings("unused") void _free() {
          }
        };
      }
    }).toArray(x1 -> new Result[x1]);
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    @Nonnull
    final TimedResult<PointSample> timedResult = TimedResult.time(() -> eval(data, monitor));
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    if (null != monitor && verbosity() > 1) {
      monitor.log(String.format("Evaluated %s items in %.4fs (%s/%s)", items, timedResult.timeNanos / 1e9,
          timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
    assert null != timedResult.result;
    return timedResult.result;
  }

  @Nonnull
  public synchronized Trainable setData(@Nonnull final TensorList[] data) {
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    this.data = data;
    return this;
  }

  @Nonnull
  @Override
  public TrainableDataMask setMask(final boolean... mask) {
    this.mask = mask;
    return this;
  }

  public int verbosity() {
    return verbosity;
  }

  @Nonnull
  protected PointSample eval(@Nonnull final TensorList[] list, @Nullable final TrainingMonitor monitor) {
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    @Nonnull
    final TimedResult<PointSample> timedResult = TimedResult.time(() -> {
      final Result[] nnContext = TensorListTrainable.getNNContext(list, mask);
      final Result result = network.eval(nnContext);
      for (@Nonnull
      Result nnResult : nnContext) {
        nnResult.getData();
      }
      final TensorList resultData = result.getData();
      final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
        double[] array = com.simiacryptus.ref.wrappers.RefArrays.stream(x.getData()).toArray();
        return com.simiacryptus.ref.wrappers.RefArrays.stream(array);
      }).summaryStatistics();
      final double sum = statistics.getSum();
      @Nonnull
      final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
      @Nonnull
      PointSample pointSample;
      {
        result.accumulate(deltaSet);
        //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
        @Nonnull
        StateSet<UUID> stateSet = new StateSet<>(deltaSet);
        pointSample = new PointSample(deltaSet, stateSet, sum, 0.0, items);
      }
      return pointSample;
    });
    if (null != monitor && verbosity() > 0) {
      monitor.log(String.format("Device completed %s items in %.3f sec", items, timedResult.timeNanos / 1e9));
    }
    return timedResult.result.normalize();
  }

  public void _free() {
  }

  public @Override @SuppressWarnings("unused") TensorListTrainable addRef() {
    return (TensorListTrainable) super.addRef();
  }

  public static @SuppressWarnings("unused") TensorListTrainable[] addRefs(TensorListTrainable[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TensorListTrainable::addRef)
        .toArray((x) -> new TensorListTrainable[x]);
  }

  public static @SuppressWarnings("unused") TensorListTrainable[][] addRefs(TensorListTrainable[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TensorListTrainable::addRefs)
        .toArray((x) -> new TensorListTrainable[x][]);
  }
}
