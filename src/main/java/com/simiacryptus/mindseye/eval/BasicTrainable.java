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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefDoubleStream;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.DoubleSummaryStatistics;
import java.util.UUID;

/**
 * This class represents a trainable object with a nullable network layer and nullable data.
 * It also has a nullable boolean mask, an input size, and an array of input proxies.
 *
 * @docgenVersion 9
 */
public class BasicTrainable extends ReferenceCountingBase implements DataTrainable, TrainableDataMask {

  @Nullable
  private final Layer network;
  @Nullable
  private RefList<Tensor[]> data;

  @Nullable
  private boolean[] mask = null;
  private int verbosity = 0;
  private int inputSize;
  private Result[] inputProxies;

  /**
   * Instantiates a new Basic trainable.
   *
   * @param network the network
   */
  public BasicTrainable(@Nullable final Layer network) {
    this.network = network;
    data = null;
    inputSize = 0;
  }

  /**
   * @return the data
   * @throws NullPointerException if data is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Tensor[][] getData() {
    assert data != null;
    return data.toArray(new Tensor[][]{});
  }

  /**
   * Sets the data for this layer.
   *
   * @param data The data to set.
   * @docgenVersion 9
   */
  public synchronized void setData(@Nonnull final RefList<Tensor[]> data) {
    if (null != this.data)
      this.data.freeRef();
    inputSize = getInputs(data);
    this.data = data;
    RefUtil.freeRef(this.inputProxies);
    this.inputProxies = getInputProxies();
  }

  /**
   * Returns an array of non-null Results.
   *
   * @docgenVersion 9
   */
  @Nonnull
  protected Result[] getInputProxies() {
    if (null == data) {
      throw new IllegalArgumentException();
    }
    if (0 > data.size()) {
      throw new IllegalArgumentException();
    }
    return RefIntStream.range(0, inputSize)
        .mapToObj(inputIndex -> {
          final Tensor[] tensors = select(inputIndex);
          if (mask(inputIndex)) {
            return new ConstantResult(new TensorArray(tensors));
          } else {
            return new MutableResult(tensors);
          }
        }).toArray(Result[]::new);
  }

  /**
   * Returns the layer associated with this object, or null if there is no such layer.
   *
   * @docgenVersion 9
   */
  @Override
  public Layer getLayer() {
    return network == null ? null : network.addRef();
  }

  /**
   * @return the mask or null if none exists
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public boolean[] getMask() {
    return mask;
  }

  /**
   * Sets the mask to the given boolean values.
   *
   * @param mask the new mask
   * @docgenVersion 9
   */
  @Override
  public void setMask(final boolean... mask) {
    this.mask = mask;
  }

  /**
   * Sets the verbosity of the output.
   *
   * @param verbose the new verbosity of the output
   * @docgenVersion 9
   */
  public void setVerbosity(int verbose) {
    verbosity = verbose;
  }

  /**
   * @param data
   * @return
   * @docgenVersion 9
   */
  @RefIgnore
  private static int getInputs(@NotNull @RefIgnore RefList<Tensor[]> data) {
    if (null == data) return 0;
    if (data.isEmpty()) return 0;
    Tensor[] tensors = data.get(0);
    int length = tensors.length;
    RefUtil.freeRef(tensors);
    return length;
  }

  /**
   * @Override public PointSample measure(@Nullable final TrainingMonitor monitor);
   * @docgenVersion 9
   */
  @Override
  public PointSample measure(@Nullable final TrainingMonitor monitor) {
    assert data != null;
    assert !data.isEmpty();
    @Nonnull final TimedResult<PointSample> timedResult = TimedResult.time(() -> eval());
    //          log.info(String.format("Evaluated to %s evalInputDelta arrays", DeltaSet<LayerBase>.apply.size()));
    PointSample result = timedResult.getResult();
    if (null != monitor && verbosity() > 1) {
      monitor.log(RefString.format("Evaluated %s items in %.4fs (%s/%s)", data.size(), timedResult.timeNanos / 1e9,
          result.getMean(), result.delta.getMagnitude()));
    }
    timedResult.freeRef();
    return result;
  }

  /**
   * Returns the verbosity level.
   *
   * @docgenVersion 9
   */
  public int verbosity() {
    return verbosity;
  }

  /**
   * Frees this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != inputProxies) {
      RefUtil.freeRef(inputProxies);
      inputProxies = null;
    }
    if (null != data) {
      data.freeRef();
      data = null;
    }
    inputSize = 0;
    if (null != network) {
      network.freeRef();
    }
  }

  /**
   * @return a new reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  BasicTrainable addRef() {
    return (BasicTrainable) super.addRef();
  }

  /**
   * @return the result of the evaluation as a PointSample
   * @throws NullPointerException if the result is null
   * @docgenVersion 9
   */
  @Nonnull
  protected PointSample eval() {
    assert network != null;
    final Result result = network.eval(RefUtil.addRef(this.inputProxies));
    assert result != null;
    final TensorList resultData = result.getData();
    @Nonnull final DeltaSet<UUID> deltaSet = new DeltaSet<UUID>();
    final DoubleSummaryStatistics statistics = resultData.stream().flatMapToDouble(x -> {
      RefDoubleStream doubleStream = x.doubleStream();
      x.freeRef();
      return doubleStream;
    }).summaryStatistics();
    final double sum = statistics.getSum();
    result.accumulate(deltaSet.addRef());
    result.freeRef();
    StateSet<UUID> stateSet = new StateSet<>(deltaSet.addRef());
    resultData.freeRef();
    //log.info(String.format("Evaluated to %s evalInputDelta buffers, %s mag", DeltaSet<LayerBase>.getMap().size(), DeltaSet<LayerBase>.getMagnitude()));
    return normalize(new PointSample(deltaSet, stateSet, sum, 0.0, data.size()));
  }

  /**
   * Returns true if the given input index should be masked, false otherwise.
   * A null mask indicates that all inputs should be masked.
   *
   * @docgenVersion 9
   */
  private boolean mask(int inputIndex) {
    return null == mask || inputIndex >= mask.length || !mask[inputIndex];
  }

  /**
   * Selects the inputIndex'th input from each batch in data.
   *
   * @param inputIndex the input index to select
   * @return an array of Tensors containing the selected inputs
   * @docgenVersion 9
   */
  @NotNull
  private Tensor[] select(int inputIndex) {
    return data.stream().map(batchData -> {
      try {
        return batchData[inputIndex].addRef();
      } finally {
        RefUtil.freeRef(batchData);
      }
    }).toArray(Tensor[]::new);
  }

  /**
   * Normalizes the given point sample.
   *
   * @param pointSample the point sample to normalize
   * @return the normalized point sample
   * @docgenVersion 9
   */
  @NotNull
  private PointSample normalize(PointSample pointSample) {
    PointSample normalize = pointSample.normalize();
    pointSample.freeRef();
    return normalize;
  }

}
