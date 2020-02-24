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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;
import java.util.function.IntFunction;

public class BasicTrainable extends ReferenceCountingBase implements DataTrainable, TrainableDataMask {

  @Nullable
  private final Layer network;
  @Nullable
  private RefList<Tensor[]> data;

  @Nullable
  private boolean[] mask = null;
  private int verbosity = 0;
  private int batchSize;

  public BasicTrainable(@Nullable final Layer network) {
    this.network = network;
    data = null;
    batchSize = 0;
  }

  @Nonnull
  @Override
  public Tensor[][] getData() {
    assert data != null;
    return data.toArray(new Tensor[][]{});
  }

  @Nonnull
  public synchronized void setData(@Nonnull final RefList<Tensor[]> data) {
    if (null != this.data)
      this.data.freeRef();
    batchSize = getBatchSize(data);
    this.data = data;
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

  @Nonnull
  @Override
  public void setMask(final boolean... mask) {
    this.mask = mask;
  }

  public void setVerbosity(int verbose) {
    verbosity = verbose;
  }

  @Nonnull
  public static Result[] getNNContext(@Nullable final RefList<Tensor[]> data, @Nullable final boolean[] mask, int batchSize) {
    if (null == data) {
      throw new IllegalArgumentException();
    }
    if (0 >= data.size()) {
      data.freeRef();
      throw new IllegalArgumentException();
    }
    return RefIntStream.range(0, batchSize)
        .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Result>) col -> {
          final Tensor[] tensors = RefIntStream.range(0, data.size())
              .mapToObj(RefUtil.wrapInterface((IntFunction<? extends Tensor>) row -> {
                    Tensor[] rowData = data.get(row);
                    Tensor cell = rowData[col].addRef();
                    RefUtil.freeRef(rowData);
                    return cell;
                  },
                  data.addRef()))
              .toArray(Tensor[]::new);
          if (null == mask || col >= mask.length || !mask[col]) {
            return new ConstantResult(new TensorArray(tensors));
          } else {
            return new MutableResult(tensors);
          }
        }, data)).toArray(Result[]::new);
  }

  @RefIgnore
  private static int getBatchSize(@NotNull @RefIgnore RefList<Tensor[]> data) {
    if (null == data) return 0;
    if (data.isEmpty()) return 0;
    Tensor[] tensors = data.get(0);
    int length = tensors.length;
    RefUtil.freeRef(tensors);
    return length;
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    assert data != null;
    assert !data.isEmpty();
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult
        .time(() -> {
          return eval(data == null ? null : data.addRef(), monitor, batchSize);
        });
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    PointSample result = timedResult.getResult();
    if (null != monitor && verbosity() > 1) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", data.size(), timedResult.timeNanos / 1e9,
          result.getMean(), result.delta.getMagnitude()));
    }
    timedResult.freeRef();
    return result;
  }

  public int verbosity() {
    return verbosity;
  }

  public void _free() {
    super._free();
    if (null != data)
      data.freeRef();
    data = null;
    batchSize = 0;
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
  protected PointSample eval(@Nonnull final RefList<Tensor[]> list, @Nullable final TrainingMonitor monitor, int batchSize) {
    int size = list.size();
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult
        .time(RefUtil.wrapInterface((UncheckedSupplier<PointSample>) () -> {
          final Result[] nnContext = BasicTrainable.getNNContext(list.addRef(), mask, batchSize);
          assert network != null;
          final Result result = network.eval(nnContext);
          assert result != null;
          final TensorList resultData = result.getData();
          @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
          final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
            double[] array = RefArrays.stream(x.getData()).toArray();
            x.freeRef();
            return RefArrays.stream(array);
          }).summaryStatistics();
          final double sum = statistics.getSum();
          result.accumulate(deltaSet.addRef());
          result.freeRef();
          StateSet<UUID> stateSet = new StateSet<>(deltaSet.addRef());
          RefMap<UUID, Delta<UUID>> deltaSetMap = deltaSet.getMap();
          list.stream().flatMap(RefArrays::stream).filter(tensor -> {
            UUID id = tensor.getId();
            tensor.freeRef();
            return deltaSetMap.containsKey(id);
          }).forEach(tensor -> {
            RefUtil.freeRef(stateSet.get(tensor.getId(), tensor.getData()));
            tensor.freeRef();
          });
          deltaSetMap.freeRef();
          resultData.freeRef();
          //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
          return new PointSample(deltaSet, stateSet, sum, 0.0, size);
        }, list));
    if (null != monitor && verbosity() > 0) {
      monitor.log(RefString.format("Device completed %s items in %.3f sec", size, timedResult.timeNanos / 1e9));
    }
    PointSample result = timedResult.getResult();
    timedResult.freeRef();
    PointSample normalize = result.normalize();
    result.freeRef();
    return normalize;
  }

}
