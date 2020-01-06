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
import com.simiacryptus.lang.UncheckedSupplier;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.layers.PlaceholderLayer;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;
import java.util.function.IntFunction;

public @RefAware
class TensorListTrainable extends ReferenceCountingBase implements TrainableDataMask {

  protected final Layer network;
  @Nullable
  protected TensorList[] data;

  @Nullable
  boolean[] mask = null;
  private int verbosity = 0;

  public TensorListTrainable(final Layer network, @org.jetbrains.annotations.Nullable final TensorList... data) {
    {
      Layer temp_20_0001 = network == null ? null : network.addRef();
      this.network = temp_20_0001 == null ? null : temp_20_0001.addRef();
      if (null != temp_20_0001)
        temp_20_0001.freeRef();
    }
    if (null != network)
      network.freeRef();
    {
      TensorList[] temp_20_0002 = TensorList
          .addRefs(data);
      if (null != this.data)
        ReferenceCounting.freeRefs(this.data);
      this.data = TensorList.addRefs(temp_20_0002);
      if (null != temp_20_0002)
        ReferenceCounting.freeRefs(temp_20_0002);
    }
    if (null != data)
      ReferenceCounting.freeRefs(data);
  }

  @Nonnull
  public TensorList[] getData() {
    return TensorList.addRefs(data);
  }

  @NotNull
  @Override
  public Layer getLayer() {
    return network == null ? null : network.addRef();
  }

  @Nullable
  @Override
  public boolean[] getMask() {
    return mask;
  }

  @Nonnull
  public TensorListTrainable setVerbosity(final int verbose) {
    verbosity = verbose;
    return this.addRef();
  }

  public static Result[] getNNContext(@Nullable final TensorList[] data, @Nullable final boolean[] mask) {
    if (null == data) {
      if (null != data)
        ReferenceCounting.freeRefs(data);
      throw new IllegalArgumentException();
    }
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    Result[] temp_20_0007 = RefIntStream.range(0, inputs)
        .mapToObj(RefUtil
            .wrapInterface((IntFunction<? extends Result>) col -> {
              final Tensor[] tensors = RefIntStream.range(0, items)
                  .mapToObj(RefUtil.wrapInterface(
                      (IntFunction<? extends Tensor>) row -> data[col]
                          .get(row),
                      TensorList.addRefs(data)))
                  .toArray(i -> new Tensor[i]);
              @Nonnull
              TensorArray tensorArray = new TensorArray(Tensor.addRefs(tensors));
              if (null == mask || col >= mask.length || !mask[col]) {
                if (null != tensors)
                  ReferenceCounting.freeRefs(tensors);
                ConstantResult temp_20_0005 = new ConstantResult(
                    tensorArray == null ? null : tensorArray);
                return temp_20_0005;
              } else {
                try {
                  try {
                    return new Result(tensorArray, new Result.Accumulator() {
                      {
                      }

                      @Override
                      public void accept(DeltaSet<UUID> buffer, TensorList delta) {
                        for (int index = 0; index < delta.length(); index++) {
                          final Tensor dt = delta.get(index);
                          @Nullable final double[] d = dt.getData();
                          if (null != dt)
                            dt.freeRef();
                          final Tensor t = tensors[index].addRef();
                          @Nullable final double[] p = t.getData();
                          if (null != t)
                            t.freeRef();
                          @Nonnull
                          PlaceholderLayer<double[]> layer = new PlaceholderLayer<>(p);
                          Delta<UUID> temp_20_0008 = buffer.get(layer.getId(),
                              p);
                          RefUtil.freeRef(temp_20_0008.addInPlace(d));
                          if (null != temp_20_0008)
                            temp_20_0008.freeRef();
                          layer.freeRef();
                        }
                        if (null != delta)
                          delta.freeRef();
                        if (null != buffer)
                          buffer.freeRef();
                      }

                      public @SuppressWarnings("unused")
                      void _free() {
                      }
                    }) {

                      @Override
                      public boolean isAlive() {
                        return true;
                      }

                      public @SuppressWarnings("unused")
                      void _free() {
                      }
                    };
                  } finally {
                    tensorArray.freeRef();
                  }
                } finally {
                  if (null != tensors)
                    ReferenceCounting.freeRefs(tensors);
                }
              }
            }, TensorList.addRefs(data)))
        .toArray(x1 -> new Result[x1]);
    if (null != data)
      ReferenceCounting.freeRefs(data);
    return temp_20_0007;
  }

  public static @SuppressWarnings("unused")
  TensorListTrainable[] addRefs(TensorListTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TensorListTrainable::addRef)
        .toArray((x) -> new TensorListTrainable[x]);
  }

  public static @SuppressWarnings("unused")
  TensorListTrainable[][] addRefs(TensorListTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(TensorListTrainable::addRefs)
        .toArray((x) -> new TensorListTrainable[x][]);
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult
        .time(() -> eval(TensorList.addRefs(data), monitor));
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    if (null != monitor && verbosity() > 1) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", items, timedResult.timeNanos / 1e9,
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
    {
      TensorList[] temp_20_0003 = TensorList
          .addRefs(data);
      if (null != this.data)
        ReferenceCounting.freeRefs(this.data);
      this.data = TensorList.addRefs(temp_20_0003);
      if (null != temp_20_0003)
        ReferenceCounting.freeRefs(temp_20_0003);
    }
    ReferenceCounting.freeRefs(data);
    return this.addRef();
  }

  @Nonnull
  @Override
  public TrainableDataMask setMask(final boolean... mask) {
    this.mask = mask;
    return this.addRef();
  }

  public int verbosity() {
    return verbosity;
  }

  public void _free() {
    if (null != data)
      ReferenceCounting.freeRefs(data);
    data = null;
    if (null != network)
      network.freeRef();
  }

  public @Override
  @SuppressWarnings("unused")
  TensorListTrainable addRef() {
    return (TensorListTrainable) super.addRef();
  }

  @Nonnull
  protected PointSample eval(@Nonnull final TensorList[] list, @Nullable final TrainingMonitor monitor) {
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(RefUtil
        .wrapInterface((UncheckedSupplier<PointSample>) () -> {
          final Result[] nnContext = TensorListTrainable
              .getNNContext(TensorList.addRefs(list), mask);
          final Result result = network.eval(Result.addRefs(nnContext));
          for (@Nonnull
              Result nnResult : nnContext) {
            RefUtil.freeRef(nnResult.getData());
          }
          if (null != nnContext)
            ReferenceCounting.freeRefs(nnContext);
          final TensorList resultData = result.getData();
          final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
            double[] array = RefArrays.stream(x.getData()).toArray();
            if (null != x)
              x.freeRef();
            return RefArrays.stream(array);
          }).summaryStatistics();
          if (null != resultData)
            resultData.freeRef();
          final double sum = statistics.getSum();
          @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
          @Nonnull
          PointSample pointSample;
          {
            result.accumulate(deltaSet == null ? null : deltaSet.addRef());
            //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
            @Nonnull
            StateSet<UUID> stateSet = new StateSet<>(deltaSet == null ? null : deltaSet.addRef());
            pointSample = new PointSample(deltaSet == null ? null : deltaSet.addRef(),
                stateSet == null ? null : stateSet, sum, 0.0, items);
          }
          deltaSet.freeRef();
          if (null != result)
            result.freeRef();
          return pointSample;
        }, TensorList.addRefs(list)));
    ReferenceCounting.freeRefs(list);
    if (null != monitor && verbosity() > 0) {
      monitor.log(RefString.format("Device completed %s items in %.3f sec", items, timedResult.timeNanos / 1e9));
    }
    return timedResult.result.normalize();
  }
}
