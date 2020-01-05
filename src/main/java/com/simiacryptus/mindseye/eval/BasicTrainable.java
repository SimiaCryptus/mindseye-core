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
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;

public @RefAware
class BasicTrainable extends ReferenceCountingBase
    implements DataTrainable, TrainableDataMask {

  protected final Layer network;
  @Nullable
  protected RefList<Tensor[]> data;

  @Nullable
  boolean[] mask = null;
  private int verbosity = 0;

  public BasicTrainable(final Layer network) {
    this.network = network;
    data = null;
  }

  @Nonnull
  @Override
  public Tensor[][] getData() {
    return data.toArray(new Tensor[][]{});
  }

  @Nonnull
  @Override
  public synchronized BasicTrainable setData(@Nonnull final RefList<Tensor[]> data) {
    this.data = data;
    return this;
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
  @Override
  public BasicTrainable setMask(final boolean... mask) {
    this.mask = mask;
    return this;
  }

  @Nonnull
  public BasicTrainable setVerbosity(final int verbose) {
    verbosity = verbose;
    return this;
  }

  public static Result[] getNNContext(@Nullable final RefList<Tensor[]> data,
                                      @Nullable final boolean[] mask) {
    if (null == data)
      throw new IllegalArgumentException();
    if (0 >= data.size())
      throw new IllegalArgumentException();
    final int cols = data.get(0).length;
    return RefIntStream.range(0, cols).mapToObj(col -> {
      final Tensor[] tensors = RefIntStream.range(0, data.size())
          .mapToObj(row -> data.get(row)[col]).toArray(i -> new Tensor[i]);
      if (null == mask || col >= mask.length || !mask[col]) {
        return new ConstantResult(new TensorArray(tensors));
      } else {
        return new MutableResult(tensors);
      }
    }).toArray(x1 -> new Result[x1]);
  }

  public static @SuppressWarnings("unused")
  BasicTrainable[] addRefs(BasicTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BasicTrainable::addRef)
        .toArray((x) -> new BasicTrainable[x]);
  }

  public static @SuppressWarnings("unused")
  BasicTrainable[][] addRefs(BasicTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BasicTrainable::addRefs)
        .toArray((x) -> new BasicTrainable[x][]);
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    assert !data.isEmpty();
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(() -> eval(data, monitor));
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    if (null != monitor && verbosity() > 1) {
      monitor.log(String.format("Evaluated %s items in %.4fs (%s/%s)", data.size(), timedResult.timeNanos / 1e9,
          timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
    assert null != timedResult.result;
    return timedResult.result;
  }

  public int verbosity() {
    return verbosity;
  }

  public void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  BasicTrainable addRef() {
    return (BasicTrainable) super.addRef();
  }

  @Nonnull
  protected PointSample eval(@Nonnull final RefList<Tensor[]> list,
                             @Nullable final TrainingMonitor monitor) {
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(() -> {
      final Result[] nnContext = BasicTrainable.getNNContext(list, mask);
      final Result result = network.eval(nnContext);
      final TensorList resultData = result.getData();
      @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
      @Nonnull
      StateSet<UUID> stateSet = null;
      try {
        final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
          double[] array = RefArrays.stream(x.getData()).toArray();
          return RefArrays.stream(array);
        }).summaryStatistics();
        final double sum = statistics.getSum();
        result.accumulate(deltaSet);
        stateSet = new StateSet<>(deltaSet);
        RefMap<UUID, Delta<UUID>> deltaSetMap = deltaSet.getMap();
        for (Tensor[] tensors : list) {
          for (Tensor tensor : tensors) {
            if (deltaSetMap.containsKey(tensor.getId()))
              stateSet.get(tensor.getId(), tensor.getData());
          }
        }
        //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
        return new PointSample(deltaSet, stateSet, sum, 0.0, list.size());
      } finally {
        if (null != stateSet)
          stateSet.freeRefAsync();
        resultData.freeRefAsync();
        result.freeRefAsync();
        deltaSet.freeRefAsync();
      }
    });
    if (null != monitor && verbosity() > 0) {
      monitor.log(String.format("Device completed %s items in %.3f sec", list.size(), timedResult.timeNanos / 1e9));
    }
    return timedResult.result.normalize();
  }

}
