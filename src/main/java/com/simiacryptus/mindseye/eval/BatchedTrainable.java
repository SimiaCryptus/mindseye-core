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
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.mindseye.opt.TrainingMonitor;

import javax.annotation.Nonnull;

public abstract @com.simiacryptus.ref.lang.RefAware
class BatchedTrainable extends TrainableWrapper<DataTrainable>
    implements DataTrainable {

  protected final int batchSize;
  private boolean verbose = false;

  public BatchedTrainable(final DataTrainable inner, final int batchSize) {
    super(inner);
    this.batchSize = batchSize;
  }

  public BatchedTrainable(final Layer network, final int batchSize) {
    this(new BasicTrainable(network), batchSize);
    getInner();
  }

  public int getBatchSize() {
    return batchSize;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public BatchedTrainable setVerbose(final boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  public static @SuppressWarnings("unused")
  BatchedTrainable[] addRefs(BatchedTrainable[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BatchedTrainable::addRef)
        .toArray((x) -> new BatchedTrainable[x]);
  }

  public static @SuppressWarnings("unused")
  BatchedTrainable[][] addRefs(BatchedTrainable[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BatchedTrainable::addRefs)
        .toArray((x) -> new BatchedTrainable[x][]);
  }

  @Override
  public PointSample measure(final TrainingMonitor monitor) {
    @Nonnull final com.simiacryptus.ref.wrappers.RefList<Tensor[]> tensors = com.simiacryptus.ref.wrappers.RefArrays
        .asList(getData());
    TimedResult<PointSample> timedResult = TimedResult.time(() -> {
      DataTrainable inner = getInner();
      if (batchSize < tensors.size()) {
        final int batches = (int) Math.ceil(tensors.size() * 1.0 / batchSize);
        final int evenBatchSize = (int) Math.ceil(tensors.size() * 1.0 / batches);
        @Nonnull final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.ref.wrappers.RefList<Tensor[]>> collection = com.simiacryptus.ref.wrappers.RefLists
            .partition(tensors, evenBatchSize);
        return collection.stream().map(trainingData -> {
          if (batchSize < trainingData.size()) {
            throw new RuntimeException();
          }
          inner.setData(trainingData);
          PointSample measure = super.measure(monitor);
          inner.setData(new com.simiacryptus.ref.wrappers.RefArrayList<>());
          return measure;
        }).reduce((a, b) -> {
          return a.add(b);
        }).get();
      } else {
        inner.setData(tensors);
        PointSample measure = super.measure(monitor);
        inner.setData(new com.simiacryptus.ref.wrappers.RefArrayList<>());
        return measure;
      }
    });
    if (null != monitor && isVerbose()) {
      monitor.log(String.format("Evaluated %s items in %.4fs (%s/%s)", tensors.size(), timedResult.timeNanos / 1e9,
          timedResult.result.getMean(), timedResult.result.delta.getMagnitude()));
    }
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
