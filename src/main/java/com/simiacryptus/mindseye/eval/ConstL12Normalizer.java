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

public class ConstL12Normalizer extends L12Normalizer implements SampledTrainable, TrainableDataMask {
  private double factor_L1 = 0.0;
  private double factor_L2 = 0.0;

  public ConstL12Normalizer(final Trainable inner) {
    super(inner);
  }

  @Nonnull
  @Override
  public SampledCachedTrainable<? extends SampledTrainable> cached() {
    return new SampledCachedTrainable<>(this);
  }

  @Override
  public Layer getLayer() {
    return inner.getLayer();
  }

  public double getFactor_L1() {
    return factor_L1;
  }

  @Nonnull
  public ConstL12Normalizer setFactor_L1(final double factor_L1) {
    this.factor_L1 = factor_L1;
    return this;
  }

  public double getFactor_L2() {
    return factor_L2;
  }

  @Nonnull
  public ConstL12Normalizer setFactor_L2(final double factor_L2) {
    this.factor_L2 = factor_L2;
    return this;
  }

  @Override
  protected double getL1(final Layer layer) {
    if (supress(layer)) return 0;
    return factor_L1;
  }

  @Override
  protected double getL2(final Layer layer) {
    return factor_L2;
  }

  @Nullable
  @Override
  public boolean[] getMask() {
    return ((TrainableDataMask) inner).getMask();
  }

  @Override
  public int getTrainingSize() {
    return ((SampledTrainable) inner).getTrainingSize();
  }

  @Nonnull
  @Override
  public ConstL12Normalizer setTrainingSize(final int trainingSize) {
    ((SampledTrainable) inner).setTrainingSize(trainingSize);
    return this;
  }

  @Nonnull
  @Override
  public TrainableDataMask setMask(final boolean... mask) {
    ((TrainableDataMask) inner).setMask(mask);
    return this;
  }

  private boolean supress(final Layer layer) {
//    if (layer instanceof BiasLayer) return false;
//    if (layer instanceof ImgBandBiasLayer) return false;
    return false;
  }
}
