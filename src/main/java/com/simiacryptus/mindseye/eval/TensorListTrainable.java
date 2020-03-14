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
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefDoubleStream;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;
import java.util.function.IntFunction;

public class TensorListTrainable extends ReferenceCountingBase implements TrainableDataMask {

  @Nullable
  protected final Layer network;
  @Nullable
  protected TensorList[] data;

  @Nullable
  boolean[] mask = null;
  private int verbosity = 0;

  public TensorListTrainable(@Nullable final Layer network, @Nullable final TensorList... data) {
    Layer temp_20_0001 = network == null ? null : network.addRef();
    this.network = temp_20_0001 == null ? null : temp_20_0001.addRef();
    if (null != temp_20_0001)
      temp_20_0001.freeRef();
    if (null != network)
      network.freeRef();
    TensorList[] temp_20_0002 = RefUtil.addRef(data);
    this.data = RefUtil.addRef(temp_20_0002);
    if (null != temp_20_0002)
      RefUtil.freeRef(temp_20_0002);
    if (null != data)
      RefUtil.freeRef(data);
  }

  @Nonnull
  public TensorList[] getData() {
    return RefUtil.addRef(data);
  }

  public synchronized void setData(@Nonnull final TensorList[] data) {
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    TensorList[] temp_20_0003 = RefUtil.addRef(data);
    if (null != this.data)
      RefUtil.freeRef(this.data);
    this.data = RefUtil.addRef(temp_20_0003);
    RefUtil.freeRef(temp_20_0003);
    RefUtil.freeRef(data);
  }

  @Override
  public Layer getLayer() {
    return network == null ? null : network.addRef();
  }

  @Nullable
  @Override
  public boolean[] getMask() {
    return mask;
  }

  @Override
  public void setMask(final boolean... mask) {
    this.mask = mask;
  }

  public void setVerbosity(int verbose) {
    verbosity = verbose;
  }

  @Nonnull
  public static Result[] getNNContext(@Nullable final TensorList[] data, @Nullable final boolean[] mask) {
    if (null == data) {
      throw new IllegalArgumentException();
    }
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    Result[] temp_20_0007 = RefIntStream.range(0, inputs)
        .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Result>) col -> {
          final Tensor[] tensors = RefIntStream.range(0, items).mapToObj(RefUtil
              .wrapInterface((IntFunction<? extends Tensor>) row -> data[col].get(row), RefUtil.addRef(data)))
              .toArray(Tensor[]::new);
          @Nonnull
          TensorArray tensorArray = new TensorArray(RefUtil.addRef(tensors));
          if (null == mask || col >= mask.length || !mask[col]) {
            RefUtil.freeRef(tensors);
            return new ConstantResult(tensorArray);
          } else {
            try {
              Result.Accumulator accumulator = new Result.Accumulator() {
                {
                  RefUtil.addRef(tensors);
                }

                @Override
                public void accept(@Nonnull DeltaSet<UUID> buffer, @Nonnull TensorList delta) {
                  for (int index = 0; index < delta.length(); index++) {
                    final Tensor dt = delta.get(index);
                    final Tensor t = tensors[index].addRef();
                    @Nonnull
                    Delta<UUID> tensorBuffer = buffer.get(UUID.randomUUID(), t);
                    tensorBuffer.addInPlace(dt);
                    tensorBuffer.freeRef();
                  }
                  delta.freeRef();
                  buffer.freeRef();
                }

                public @SuppressWarnings("unused")
                void _free() {
                  super._free();
                  RefUtil.freeRef(tensors);
                }
              };
              return new Result(tensorArray, accumulator);
            } finally {
              RefUtil.freeRef(tensors);
            }
          }
        }, RefUtil.addRef(data))).toArray(Result[]::new);
    RefUtil.freeRef(data);
    return temp_20_0007;
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    assert data != null;
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(() -> eval(RefUtil.addRef(data), monitor));
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    PointSample result = timedResult.getResult();
    if (null != monitor && verbosity() > 1) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", items, timedResult.timeNanos / 1e9,
          result.getMean(), result.delta.getMagnitude()));
    }
    timedResult.freeRef();
    assert null != result;
    return result;
  }

  public int verbosity() {
    return verbosity;
  }

  public void _free() {
    super._free();
    if (null != data)
      RefUtil.freeRef(data);
    data = null;
    if (null != network)
      network.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  TensorListTrainable addRef() {
    return (TensorListTrainable) super.addRef();
  }

  @Nonnull
  protected PointSample eval(@Nonnull final TensorList[] list, @Nullable final TrainingMonitor monitor) {
    assert data != null;
    int inputs = data.length;
    assert 0 < inputs;
    int items = data[0].length();
    assert 0 < items;
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult
        .time(RefUtil.wrapInterface((UncheckedSupplier<PointSample>) () -> {
          final Result[] nnContext = TensorListTrainable.getNNContext(RefUtil.addRef(list), mask);
          assert network != null;
          final Result result = network.eval(nnContext);
          assert result != null;
          final TensorList resultData = result.getData();
          final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
            RefDoubleStream doubleStream = x.doubleStream();
            x.freeRef();
            return doubleStream;
          }).summaryStatistics();
          resultData.freeRef();
          final double sum = statistics.getSum();
          @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
          result.accumulate(deltaSet.addRef());
          //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
          @Nonnull
          StateSet<UUID> stateSet = new StateSet<>(deltaSet.addRef());
          @Nonnull
          PointSample pointSample = new PointSample(deltaSet.addRef(), stateSet,
              sum, 0.0, items);
          deltaSet.freeRef();
          result.freeRef();
          return pointSample;
        }, RefUtil.addRef(list)));
    RefUtil.freeRef(list);
    if (null != monitor && verbosity() > 0) {
      monitor.log(RefString.format("Device completed %s items in %.3f sec", items, timedResult.timeNanos / 1e9));
    }
    PointSample result = timedResult.getResult();
    PointSample normalize = result.normalize();
    result.freeRef();
    timedResult.freeRef();
    return normalize;
  }
}
