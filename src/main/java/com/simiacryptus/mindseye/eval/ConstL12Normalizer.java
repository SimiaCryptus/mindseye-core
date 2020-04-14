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

import com.simiacryptus.mindseye.lang.Layer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The type Const l 12 normalizer.
 */
public class ConstL12Normalizer extends L12Normalizer implements SampledTrainable, TrainableDataMask {
  private double factor_L1 = 0.0;
  private double factor_L2 = 0.0;

  /**
   * Instantiates a new Const l 12 normalizer.
   *
   * @param inner the inner
   */
  public ConstL12Normalizer(final Trainable inner) {
    super(inner);
  }

  /**
   * Gets factor l 1.
   *
   * @return the factor l 1
   */
  public double getFactor_L1() {
    return factor_L1;
  }

  /**
   * Sets factor l 1.
   *
   * @param factor_L1 the factor l 1
   */
  public void setFactor_L1(double factor_L1) {
    this.factor_L1 = factor_L1;
  }

  /**
   * Gets factor l 2.
   *
   * @return the factor l 2
   */
  public double getFactor_L2() {
    return factor_L2;
  }

  /**
   * Sets factor l 2.
   *
   * @param factor_L2 the factor l 2
   */
  public void setFactor_L2(double factor_L2) {
    this.factor_L2 = factor_L2;
  }

  @Override
  public Layer getLayer() {
    assert inner != null;
    return inner.getLayer();
  }

  @Nullable
  @Override
  public boolean[] getMask() {
    assert inner != null;
    return ((TrainableDataMask) inner).getMask();
  }

  @Override
  public void setMask(final boolean... mask) {
    assert inner != null;
    ((TrainableDataMask) inner).setMask(mask);
  }

  @Override
  public int getTrainingSize() {
    assert inner != null;
    return ((SampledTrainable) inner).getTrainingSize();
  }

  @Override
  public void setTrainingSize(final int trainingSize) {
    assert inner != null;
    ((SampledTrainable) inner).setTrainingSize(trainingSize);
  }

  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this.addRef());
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ConstL12Normalizer addRef() {
    return (ConstL12Normalizer) super.addRef();
  }

  @Override
  protected double getL1(@Nullable final Layer layer) {
    if (supress(layer == null ? null : layer.addRef())) {
      layer.freeRef();
      return 0;
    }
    if (null != layer)
      layer.freeRef();
    return factor_L1;
  }

  @Override
  protected double getL2(@Nullable final Layer layer) {
    if (null != layer)
      layer.freeRef();
    return factor_L2;
  }

  private boolean supress(@Nullable final Layer layer) {
    if (null != layer)
      layer.freeRef();
    //    if (layer instanceof BiasLayer) return false;
    //    if (layer instanceof ImgBandBiasLayer) return false;
    return false;
  }
}
