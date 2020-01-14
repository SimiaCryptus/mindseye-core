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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;
import java.util.function.IntFunction;

public class BasicTrainable extends ReferenceCountingBase implements DataTrainable, TrainableDataMask {

  @Nullable
  protected final Layer network;
  @Nullable
  protected RefList<Tensor[]> data;

  @Nullable
  boolean[] mask = null;
  private int verbosity = 0;

  public BasicTrainable(@Nullable final Layer network) {
    Layer temp_13_0001 = network == null ? null : network.addRef();
    this.network = temp_13_0001 == null ? null : temp_13_0001.addRef();
    if (null != temp_13_0001)
      temp_13_0001.freeRef();
    if (null != network)
      network.freeRef();
    RefList<Tensor[]> temp_13_0002 = null;
    data = null;
  }

  @Nonnull
  @Override
  public Tensor[][] getData() {
    assert data != null;
    return data.toArray(new Tensor[][]{});
  }

  @Nonnull
  @Override
  public synchronized BasicTrainable setData(@Nonnull final RefList<Tensor[]> data) {
    RefList<Tensor[]> temp_13_0003 = data.addRef();
    if (null != this.data)
      this.data.freeRef();
    this.data = temp_13_0003.addRef();
    temp_13_0003.freeRef();
    data.freeRef();
    return this.addRef();
  }

  @Nonnull
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
  @Override
  public BasicTrainable setMask(final boolean... mask) {
    this.mask = mask;
    return this.addRef();
  }

  @Nonnull
  public BasicTrainable setVerbosity(final int verbose) {
    verbosity = verbose;
    return this.addRef();
  }

  @Nonnull
  public static Result[] getNNContext(@Nullable final RefList<Tensor[]> data, @Nullable final boolean[] mask) {
    if (null == data) {
      throw new IllegalArgumentException();
    }
    if (0 >= data.size()) {
      data.freeRef();
      throw new IllegalArgumentException();
    }
    Tensor[] temp_13_0008 = data.get(0);
    final int cols = temp_13_0008.length;
    ReferenceCounting.freeRefs(temp_13_0008);
    Result[] temp_13_0007 = RefIntStream.range(0, cols)
        .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Result>) col -> {
          final Tensor[] tensors = RefIntStream.range(0, data.size())
              .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) row -> {
                    Tensor[] rowData = data.get(row);
                    Tensor cell = rowData[col].addRef();
                    ReferenceCounting.freeRefs(rowData);
                    return cell;
                  },
                  data.addRef()))
              .toArray(i -> new Tensor[i]);
          if (null == mask || col >= mask.length || !mask[col]) {
            ConstantResult temp_13_0004 = new ConstantResult(new TensorArray(Tensor.addRefs(tensors)));
            ReferenceCounting.freeRefs(tensors);
            return temp_13_0004;
          } else {
            MutableResult temp_13_0005 = new MutableResult(Tensor.addRefs(tensors));
            ReferenceCounting.freeRefs(tensors);
            return temp_13_0005;
          }
        }, data.addRef())).toArray(x1 -> new Result[x1]);
    data.freeRef();
    return temp_13_0007;
  }

  @Nullable
  public static @SuppressWarnings("unused")
  BasicTrainable[] addRefs(@Nullable BasicTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BasicTrainable::addRef)
        .toArray((x) -> new BasicTrainable[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  BasicTrainable[][] addRefs(@Nullable BasicTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BasicTrainable::addRefs)
        .toArray((x) -> new BasicTrainable[x][]);
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    assert data != null;
    assert !data.isEmpty();
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult
        .time(() -> eval(data == null ? null : data.addRef(), monitor));
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    if (null != monitor && verbosity() > 1) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", data.size(), timedResult.timeNanos / 1e9,
          timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
    assert null != timedResult.result;
    return timedResult.result;
  }

  public int verbosity() {
    return verbosity;
  }

  public void _free() {
    if (null != data)
      data.freeRef();
    data = null;
    if (null != network)
      network.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  BasicTrainable addRef() {
    return (BasicTrainable) super.addRef();
  }

  @Nonnull
  protected PointSample eval(@Nonnull final RefList<Tensor[]> list, @Nullable final TrainingMonitor monitor) {
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult
        .time(RefUtil.wrapInterface((UncheckedSupplier<PointSample>) () -> {
          final Result[] nnContext = BasicTrainable.getNNContext(list.addRef(), mask);
          assert network != null;
          final Result result = network.eval(Result.addRefs(nnContext));
          ReferenceCounting.freeRefs(nnContext);
          assert result != null;
          final TensorList resultData = result.getData();
          @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
          try {
            final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
              double[] array = RefArrays.stream(x.getData()).toArray();
              x.freeRef();
              return RefArrays.stream(array);
            }).summaryStatistics();
            final double sum = statistics.getSum();
            result.accumulate(deltaSet.addRef());
            StateSet<UUID> stateSet = new StateSet<>(deltaSet.addRef());
            try {
              RefMap<UUID, Delta<UUID>> deltaSetMap = deltaSet.getMap();
              list.stream().flatMap(Arrays::stream).filter(tensor -> deltaSetMap.containsKey(tensor.getId()))
                  .forEach(tensor -> stateSet.get(tensor.getId(), tensor.getData()));
              deltaSetMap.freeRef();
              result.freeRef();
              resultData.freeRef();
              PointSample temp_13_0006 = new PointSample(deltaSet,
                  stateSet, sum, 0.0, list.size());
              //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
              return temp_13_0006;
            } finally {
              stateSet.freeRefAsync();
            }
          } finally {
            resultData.freeRefAsync();
            result.freeRefAsync();
            deltaSet.freeRefAsync();
          }
        }, list.addRef()));
    if (null != monitor && verbosity() > 0) {
      monitor.log(RefString.format("Device completed %s items in %.3f sec", list.size(), timedResult.timeNanos / 1e9));
    }
    list.freeRef();
    return timedResult.result.normalize();
  }

}
