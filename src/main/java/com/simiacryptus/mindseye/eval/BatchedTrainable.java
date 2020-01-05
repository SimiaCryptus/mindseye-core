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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefLists;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Function;

public abstract @RefAware
class BatchedTrainable extends TrainableWrapper<DataTrainable> implements DataTrainable {

  protected final int batchSize;
  private boolean verbose = false;

  public BatchedTrainable(final DataTrainable inner, final int batchSize) {
    super(inner);
    if (null != inner)
      inner.freeRef();
    this.batchSize = batchSize;
  }

  public BatchedTrainable(final Layer network, final int batchSize) {
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

  public BatchedTrainable setVerbose(final boolean verbose) {
    this.verbose = verbose;
    return this.addRef();
  }

  public static @SuppressWarnings("unused")
  BatchedTrainable[] addRefs(BatchedTrainable[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BatchedTrainable::addRef)
        .toArray((x) -> new BatchedTrainable[x]);
  }

  public static @SuppressWarnings("unused")
  BatchedTrainable[][] addRefs(BatchedTrainable[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(BatchedTrainable::addRefs)
        .toArray((x) -> new BatchedTrainable[x][]);
  }

  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    @Nonnull final RefList<Tensor[]> tensors = RefArrays.asList(getData());
    TimedResult<PointSample> timedResult = TimedResult.time(RefUtil
        .wrapInterface((UncheckedSupplier<PointSample>) () -> {
          DataTrainable inner = getInner();
          if (batchSize < tensors.size()) {
            final int batches = (int) Math.ceil(tensors.size() * 1.0 / batchSize);
            final int evenBatchSize = (int) Math.ceil(tensors.size() * 1.0 / batches);
            @Nonnull final RefList<RefList<Tensor[]>> collection = RefLists.partition(tensors == null ? null : tensors.addRef(),
                evenBatchSize);
            PointSample temp_36_0001 = collection.stream()
                .map(RefUtil.wrapInterface(
                    (Function<? super RefList<Tensor[]>, ? extends PointSample>) trainingData -> {
                      if (batchSize < trainingData.size()) {
                        if (null != trainingData)
                          trainingData.freeRef();
                        throw new RuntimeException();
                      }
                      RefUtil
                          .freeRef(inner.setData(trainingData == null ? null : trainingData.addRef()));
                      if (null != trainingData)
                        trainingData.freeRef();
                      PointSample measure = super.measure(monitor).addRef();
                      RefUtil.freeRef(inner.setData(new RefArrayList<>()));
                      return measure;
                    }, inner == null ? null : inner.addRef()))
                .reduce((a, b) -> {
                  PointSample temp_36_0002 = a.add(b == null ? null : b.addRef());
                  if (null != b)
                    b.freeRef();
                  if (null != a)
                    a.freeRef();
                  return temp_36_0002;
                }).get();
            collection.freeRef();
            if (null != inner)
              inner.freeRef();
            return temp_36_0001;
          } else {
            RefUtil.freeRef(inner.setData(tensors == null ? null : tensors.addRef()));
            PointSample measure = super.measure(monitor).addRef();
            RefUtil.freeRef(inner.setData(new RefArrayList<>()));
            if (null != inner)
              inner.freeRef();
            return measure;
          }
        }, tensors == null ? null : tensors.addRef()));
    if (null != monitor && isVerbose()) {
      monitor.log(String.format("Evaluated %s items in %.4fs (%s/%s)", tensors.size(), timedResult.timeNanos / 1e9,
          timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
    tensors.freeRef();
    return timedResult.result;
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  BatchedTrainable addRef() {
    return (BatchedTrainable) super.addRef();
  }
}
