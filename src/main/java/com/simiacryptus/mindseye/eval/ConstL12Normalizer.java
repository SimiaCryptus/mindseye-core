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
 * This class is responsible for normalizing values according to the L1 and L2 norms.
 * The factor_L1 and factor_L2 fields represent the constants used in the normalization process.
 *
 * @docgenVersion 9
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
   * Returns the value of factor_L1.
   *
   * @docgenVersion 9
   */
  public double getFactor_L1() {
    return factor_L1;
  }

  /**
   * Sets the L1 regularization factor.
   *
   * @param factor_L1 the new L1 regularization factor
   * @docgenVersion 9
   */
  public void setFactor_L1(double factor_L1) {
    this.factor_L1 = factor_L1;
  }

  /**
   * Returns the value of the 'factor_L2' field.
   *
   * @docgenVersion 9
   */
  public double getFactor_L2() {
    return factor_L2;
  }

  /**
   * Sets the L2 regularization factor.
   *
   * @param factor_L2 the new L2 regularization factor
   * @docgenVersion 9
   */
  public void setFactor_L2(double factor_L2) {
    this.factor_L2 = factor_L2;
  }

  /**
   * @return the layer
   * @docgenVersion 9
   */
  @Override
  public Layer getLayer() {
    assert inner != null;
    return inner.getLayer();
  }

  /**
   * @return the mask, or null if there is no inner TrainableDataMask
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public boolean[] getMask() {
    assert inner != null;
    return ((TrainableDataMask) inner).getMask();
  }

  /**
   * Sets the mask for this object.
   *
   * @param mask the new mask
   * @docgenVersion 9
   */
  @Override
  public void setMask(final boolean... mask) {
    assert inner != null;
    ((TrainableDataMask) inner).setMask(mask);
  }

  /**
   * @return the number of training examples
   * @docgenVersion 9
   */
  @Override
  public int getTrainingSize() {
    assert inner != null;
    return ((SampledTrainable) inner).getTrainingSize();
  }

  /**
   * Sets the training size.
   *
   * @param trainingSize the training size
   * @docgenVersion 9
   */
  @Override
  public void setTrainingSize(final int trainingSize) {
    assert inner != null;
    ((SampledTrainable) inner).setTrainingSize(trainingSize);
  }

  /**
   * Returns a new SampledCachedTrainable that is a cached version of this SampledTrainable.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this.addRef());
  }

  /**
   * This method is unused.
   *
   * @docgenVersion 9
   */
  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  /**
   * @return the ConstL12Normalizer object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  ConstL12Normalizer addRef() {
    return (ConstL12Normalizer) super.addRef();
  }

  /**
   * @param layer
   * @return
   * @docgenVersion 9
   */
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

  /**
   * @param layer
   * @return
   * @docgenVersion 9
   */
  @Override
  protected double getL2(@Nullable final Layer layer) {
    if (null != layer)
      layer.freeRef();
    return factor_L2;
  }

  /**
   * Returns false if the given layer is not null and not an instance of BiasLayer or ImgBandBiasLayer.
   * Otherwise, the layer's reference is freed and true is returned.
   *
   * @docgenVersion 9
   */
  private boolean supress(@Nullable final Layer layer) {
    if (null != layer)
      layer.freeRef();
    //    if (layer instanceof BiasLayer) return false;
    //    if (layer instanceof ImgBandBiasLayer) return false;
    return false;
  }
}
