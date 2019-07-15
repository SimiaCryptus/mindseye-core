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
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.opt.TrainingMonitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class BasicTrainable extends ReferenceCountingBase implements DataTrainable, TrainableDataMask {

  protected final Layer network;
  @Nullable
  protected List<Tensor[]> data;

  @Nullable
  boolean[] mask = null;
  private int verbosity = 0;

  public BasicTrainable(final Layer network) {
    this.network = network;
    this.network.addRef();
    data = null;
  }

  public static Result[] getNNContext(@Nullable final List<Tensor[]> data, @Nullable final boolean[] mask) {
    if (null == data) throw new IllegalArgumentException();
    if (0 >= data.size()) throw new IllegalArgumentException();
    final int cols = data.get(0).length;
    return IntStream.range(0, cols).mapToObj(col -> {
      final Tensor[] tensors = IntStream.range(0, data.size()).mapToObj(row -> data.get(row)[col]).toArray(i -> new Tensor[i]);
      if (null == mask || col >= mask.length || !mask[col]) {
        return new ConstantResult(TensorArray.create(tensors));
      } else {
        return new MutableResult(tensors);
      }
    }).toArray(x1 -> new Result[x1]);
  }

  @Nonnull
  protected PointSample eval(@Nonnull final List<Tensor[]> list, @Nullable final TrainingMonitor monitor) {
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(() -> {
      final Result[] nnContext = BasicTrainable.getNNContext(list, mask);
      final Result result = network.evalAndFree(nnContext);
      final TensorList resultData = result.getData();
      @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
      @Nonnull StateSet<UUID> stateSet = null;
      try {
        final DoubleSummaryStatistics statistics = resultData.stream()
            .flatMapToDouble(x -> {
              double[] array = Arrays.stream(x.getData()).toArray();
              x.freeRef();
              return Arrays.stream(array);
            }).summaryStatistics();
        final double sum = statistics.getSum();
        result.accumulate(deltaSet);
        stateSet = new StateSet<>(deltaSet);
        Map<UUID, Delta<UUID>> deltaSetMap = deltaSet.getMap();
        for (Tensor[] tensors : list) {
          for (Tensor tensor : tensors) {
            if (deltaSetMap.containsKey(tensor.getId()))
              stateSet.get(tensor.getId(), tensor.getData()).freeRef();
          }
        }
        //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
        return new PointSample(deltaSet, stateSet, sum, 0.0, list.size());
      } finally {
        if (null != stateSet) stateSet.freeRefAsync();
        resultData.freeRefAsync();
        result.freeRefAsync();
        deltaSet.freeRefAsync();
      }
    });
    if (null != monitor && verbosity() > 0) {
      monitor.log(String.format("Device completed %s items in %.3f sec", list.size(), timedResult.timeNanos / 1e9));
    }
    try {
      return timedResult.result.normalize();
    } finally {
      timedResult.result.freeRef();
    }
  }

  @Nonnull
  @Override
  public Tensor[][] getData() {
    return data.toArray(new Tensor[][]{});
  }

  @Nonnull
  @Override
  public synchronized BasicTrainable setData(@Nonnull final List<Tensor[]> data) {
    if (null != data) data.stream().flatMap(x -> Arrays.stream(x)).forEach(x -> x.addRef());
    if (null != this.data) this.data.stream().flatMap(x -> Arrays.stream(x)).forEach(x -> x.freeRef());
    this.data = data;
    return this;
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

  @Override
  public Layer getLayer() {
    return network;
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    assert !data.isEmpty();
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(() -> eval(data, monitor));
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    if (null != monitor && verbosity() > 1) {
      monitor.log(String.format("Evaluated %s items in %.4fs (%s/%s)", data.size(), timedResult.timeNanos / 1e9, timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
    assert null != timedResult.result;
    return timedResult.result;
  }

  @Nonnull
  public BasicTrainable setVerbosity(final int verbose) {
    verbosity = verbose;
    return this;
  }

  public int verbosity() {
    return verbosity;
  }

  @Override
  protected void _free() {
    this.network.freeRef();
    if (null != this.data) this.data.stream().flatMap(x -> Arrays.stream(x)).forEach(x -> x.freeRef());
  }

}
