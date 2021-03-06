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
import java.util.function.Function;

/**
 * The type Batched trainable.
 */
public abstract class BatchedTrainable extends TrainableWrapper<DataTrainable> implements DataTrainable {

  /**
   * The Batch size.
   */
  protected final int batchSize;
  private boolean verbose = false;

  /**
   * Instantiates a new Batched trainable.
   *
   * @param inner     the inner
   * @param batchSize the batch size
   */
  public BatchedTrainable(final DataTrainable inner, final int batchSize) {
    super(inner);
    this.batchSize = batchSize;
  }

  /**
   * Instantiates a new Batched trainable.
   *
   * @param network   the network
   * @param batchSize the batch size
   */
  public BatchedTrainable(@Nullable final Layer network, final int batchSize) {
    this(new BasicTrainable(network), batchSize);
  }

  /**
   * Gets batch size.
   *
   * @return the batch size
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Is verbose boolean.
   *
   * @return the boolean
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets verbose.
   *
   * @param verbose the verbose
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    @Nonnull final RefList<Tensor[]> tensors = RefArrays.asList(getData());
    int size = tensors.size();
    TimedResult<PointSample> timedResult = TimedResult
        .time(RefUtil.wrapInterface((UncheckedSupplier<PointSample>) () -> {
          DataTrainable inner = getInner();
          if (batchSize < size) {
            final int batches = (int) Math.ceil(size * 1.0 / batchSize);
            final int evenBatchSize = (int) Math.ceil(size * 1.0 / batches);
            @Nonnull final RefList<RefList<Tensor[]>> collection = RefLists.partition(tensors.addRef(), evenBatchSize);
            PointSample temp_36_0001 = RefUtil.get(collection.stream()
                .map(RefUtil.wrapInterface((Function<RefList<Tensor[]>, PointSample>) trainingData -> {
                  if (batchSize < trainingData.size()) {
                    trainingData.freeRef();
                    throw new RuntimeException();
                  }
                  assert inner != null;
                  inner.setData(trainingData.addRef());
                  trainingData.freeRef();
                  PointSample measure = super.measure(monitor);
                  inner.setData(new RefArrayList<>());
                  return measure;
                }, inner)).reduce((a, b) -> {
                  PointSample temp_36_0002 = a.add(b == null ? null : b.addRef());
                  if (null != b)
                    b.freeRef();
                  a.freeRef();
                  return temp_36_0002;
                }));
            collection.freeRef();
            return temp_36_0001;
          } else {
            assert inner != null;
            inner.setData(tensors.addRef());
            PointSample measure = super.measure(monitor);
            inner.setData(new RefArrayList<>());
            inner.freeRef();
            return measure;
          }
        }, tensors));
    PointSample result = timedResult.getResult();
    if (null != monitor && isVerbose()) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", size, timedResult.timeNanos / 1e9,
          result.getMean(), result.delta.getMagnitude()));
    }
    timedResult.freeRef();
    return result;
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  BatchedTrainable addRef() {
    return (BatchedTrainable) super.addRef();
  }
}
