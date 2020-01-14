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
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Function;

public abstract class BatchedTrainable extends TrainableWrapper<DataTrainable> implements DataTrainable {

  protected final int batchSize;
  private boolean verbose = false;

  public BatchedTrainable(final DataTrainable inner, final int batchSize) {
    super(inner);
    this.batchSize = batchSize;
  }

  public BatchedTrainable(@Nullable final Layer network, final int batchSize) {
    this(new BasicTrainable(network == null ? null : network.addRef()), batchSize);
    if (null != network)
      network.freeRef();
    RefUtil.freeRef(getInner());
  }

  public int getBatchSize() {
    return batchSize;
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Nonnull
  public BatchedTrainable setVerbose(final boolean verbose) {
    this.verbose = verbose;
    return this.addRef();
  }

  @Nullable
  public static @SuppressWarnings("unused")
  BatchedTrainable[] addRefs(@Nullable BatchedTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BatchedTrainable::addRef)
        .toArray((x) -> new BatchedTrainable[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  BatchedTrainable[][] addRefs(@Nullable BatchedTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BatchedTrainable::addRefs)
        .toArray((x) -> new BatchedTrainable[x][]);
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    @Nonnull final RefList<Tensor[]> tensors = RefArrays.asList(getData());
    TimedResult<PointSample> timedResult = TimedResult
        .time(RefUtil.wrapInterface((UncheckedSupplier<PointSample>) () -> {
          DataTrainable inner = getInner();
          if (batchSize < tensors.size()) {
            final int batches = (int) Math.ceil(tensors.size() * 1.0 / batchSize);
            final int evenBatchSize = (int) Math.ceil(tensors.size() * 1.0 / batches);
            @Nonnull final RefList<RefList<Tensor[]>> collection = RefLists.partition(tensors.addRef(), evenBatchSize);
            PointSample temp_36_0001 = RefUtil.get(collection.stream()
                .map(RefUtil.wrapInterface((Function<RefList<Tensor[]>, PointSample>) trainingData -> {
                  if (batchSize < trainingData.size()) {
                    trainingData.freeRef();
                    throw new RuntimeException();
                  }
                  assert inner != null;
                  RefUtil.freeRef(inner.setData(trainingData.addRef()));
                  trainingData.freeRef();
                  PointSample measure = super.measure(monitor).addRef();
                  RefUtil.freeRef(inner.setData(new RefArrayList<>()));
                  return measure;
                }, inner == null ? null : inner.addRef())).reduce((a, b) -> {
                  PointSample temp_36_0002 = a.add(b == null ? null : b.addRef());
                  if (null != b)
                    b.freeRef();
                  a.freeRef();
                  return temp_36_0002;
                }));
            collection.freeRef();
            if (null != inner)
              inner.freeRef();
            return temp_36_0001;
          } else {
            assert inner != null;
            RefUtil.freeRef(inner.setData(tensors.addRef()));
            PointSample measure = super.measure(monitor).addRef();
            RefUtil.freeRef(inner.setData(new RefArrayList<>()));
            inner.freeRef();
            return measure;
          }
        }, tensors.addRef()));
    if (null != monitor && isVerbose()) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", tensors.size(), timedResult.timeNanos / 1e9,
          timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
    tensors.freeRef();
    return timedResult.result;
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  BatchedTrainable addRef() {
    return (BatchedTrainable) super.addRef();
  }
}
