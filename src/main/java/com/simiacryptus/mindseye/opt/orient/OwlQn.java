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

package com.simiacryptus.mindseye.opt.orient;

import com.simiacryptus.mindseye.eval.Trainable;
import com.simiacryptus.mindseye.lang.Delta;
import com.simiacryptus.mindseye.lang.DeltaSet;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.PointSample;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchPoint;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class OwlQn extends OrientationStrategyBase<LineSearchCursor> {
  public final OrientationStrategy<?> inner;
  private double factor_L1 = 0.000;
  private double zeroTol = 1e-20;

  public OwlQn() {
    this(new LBFGS());
  }

  protected OwlQn(final OrientationStrategy<?> inner) {
    OrientationStrategy<?> temp_29_0001 = inner == null ? null : inner.addRef();
    this.inner = temp_29_0001 == null ? null : temp_29_0001.addRef();
    if (null != temp_29_0001)
      temp_29_0001.freeRef();
    if (null != inner)
      inner.freeRef();
  }

  public double getFactor_L1() {
    return factor_L1;
  }

  @Nonnull
  public OwlQn setFactor_L1(final double factor_L1) {
    this.factor_L1 = factor_L1;
    return this.addRef();
  }

  public double getZeroTol() {
    return zeroTol;
  }

  @Nonnull
  public OwlQn setZeroTol(final double zeroTol) {
    this.zeroTol = zeroTol;
    return this.addRef();
  }

  public static @SuppressWarnings("unused") OwlQn[] addRefs(OwlQn[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(OwlQn::addRef).toArray((x) -> new OwlQn[x]);
  }

  public static @SuppressWarnings("unused") OwlQn[][] addRefs(OwlQn[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(OwlQn::addRefs).toArray((x) -> new OwlQn[x][]);
  }

  public RefCollection<Layer> getLayers(@Nonnull final RefCollection<Layer> layers) {
    RefList<Layer> temp_29_0004 = layers.stream()
        //        .filter(layer -> layer instanceof FullyConnectedLayer)
        .collect(RefCollectors.toList());
    layers.freeRef();
    return temp_29_0004;
  }

  @Nonnull
  @Override
  public LineSearchCursor orient(final Trainable subject, @Nonnull final PointSample measurement,
      final TrainingMonitor monitor) {
    @Nonnull
    final SimpleLineSearchCursor gradient = (SimpleLineSearchCursor) inner
        .orient(subject == null ? null : subject.addRef(), measurement == null ? null : measurement.addRef(), monitor);
    @Nonnull
    final DeltaSet<UUID> searchDirection = gradient.direction.copy();
    @Nonnull
    final DeltaSet<UUID> orthant = new DeltaSet<UUID>();
    RefMap<UUID, Delta<UUID>> temp_29_0006 = gradient.direction.getMap();
    RefSet<UUID> keySet = temp_29_0006.keySet();
    if (null != temp_29_0006)
      temp_29_0006.freeRef();
    RefList<Layer> layerSet = keySet.stream()
        .map(RefUtil.wrapInterface((Function<? super UUID, ? extends Layer>) id -> {
          DAGNetwork layer = (DAGNetwork) subject.getLayer();
          RefMap<UUID, Layer> layersById = layer.getLayersById();
          Layer id_layer = layersById.get(id);
          layersById.freeRef();
          layer.freeRef();
          return id_layer;
        }, subject == null ? null : subject.addRef())).collect(RefCollectors.toList());
    if (null != keySet)
      keySet.freeRef();
    RefCollection<Layer> layers = getLayers(layerSet == null ? null : layerSet.addRef());
    for (@Nonnull final Layer layer : layers) {
      RefMap<UUID, Delta<UUID>> temp_29_0007 = gradient.direction.getMap();
      temp_29_0007
          .forEach(RefUtil.wrapInterface((BiConsumer<? super UUID, ? super Delta<UUID>>) (layerId, layerDelta) -> {
            final double[] weights = layerDelta.target;
            @Nullable
            final double[] delta = layerDelta.getDelta();
            if (null != layerDelta)
              layerDelta.freeRef();
            Delta<UUID> layerDelta1 = searchDirection.get(layerId, weights);
            @Nullable
            final double[] searchDir = layerDelta1.getDelta();
            if (null != layerDelta1)
              layerDelta1.freeRef();
            Delta<UUID> layerDelta2 = orthant.get(layerId, weights);
            @Nullable
            final double[] suborthant = layerDelta2.getDelta();
            if (null != layerDelta2)
              layerDelta2.freeRef();
            for (int i = 0; i < searchDir.length; i++) {
              final int positionSign = sign(weights[i]);
              final int directionSign = sign(delta[i]);
              suborthant[i] = 0 == positionSign ? directionSign : positionSign;
              searchDir[i] += factor_L1 * (weights[i] < 0 ? -1.0 : 1.0);
              if (sign(searchDir[i]) != directionSign) {
                searchDir[i] = delta[i];
              }
            }
            assert null != searchDir;
          }, searchDirection == null ? null : searchDirection.addRef(), orthant == null ? null : orthant.addRef()));
      if (null != temp_29_0007)
        temp_29_0007.freeRef();
    }
    layers.freeRef();
    if (null != layerSet)
      layerSet.freeRef();
    orthant.freeRef();
    gradient.freeRef();
    SimpleLineSearchCursor temp_29_0005 = new SimpleLineSearchCursor(subject, measurement, searchDirection) {
      {
      }

      @Nonnull
      @Override
      public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
        origin.weights.stream().forEach(d -> {
          RefUtil.freeRef(d.restore());
          if (null != d)
            d.freeRef();
        });
        @Nonnull
        final DeltaSet<UUID> currentDirection = direction.copy();
        RefMap<UUID, Delta<UUID>> temp_29_0008 = direction.getMap();
        temp_29_0008.forEach(RefUtil.wrapInterface((BiConsumer<? super UUID, ? super Delta<UUID>>) (layer, buffer) -> {
          if (null == buffer.getDelta()) {
            if (null != buffer)
              buffer.freeRef();
            return;
          }
          Delta<UUID> layerDelta = currentDirection.get(layer, buffer.target);
          @Nullable
          final double[] currentDelta = layerDelta.getDelta();
          if (null != layerDelta)
            layerDelta.freeRef();
          for (int i = 0; i < buffer.getDelta().length; i++) {
            final double prevValue = buffer.target[i];
            final double newValue = prevValue + buffer.getDelta()[i] * alpha;
            if (sign(prevValue) != 0 && sign(prevValue) != sign(newValue)) {
              currentDelta[i] = 0;
              buffer.target[i] = 0;
            } else {
              buffer.target[i] = newValue;
            }
          }
          if (null != buffer)
            buffer.freeRef();
        }, currentDirection == null ? null : currentDirection.addRef()));
        if (null != temp_29_0008)
          temp_29_0008.freeRef();
        PointSample temp_29_0009 = subject.measure(monitor);
        @Nonnull
        final PointSample measure = afterStep(temp_29_0009.setRate(alpha));
        if (null != temp_29_0009)
          temp_29_0009.freeRef();
        double dot = currentDirection.dot(measure.delta.addRef());
        if (null != currentDirection)
          currentDirection.freeRef();
        LineSearchPoint temp_29_0003 = new LineSearchPoint(measure == null ? null : measure.addRef(), dot);
        if (null != measure)
          measure.freeRef();
        return temp_29_0003;
      }

      public @SuppressWarnings("unused") void _free() {
      }
    };
    SimpleLineSearchCursor temp_29_0002 = temp_29_0005.setDirectionType("OWL/QN");
    if (null != temp_29_0005)
      temp_29_0005.freeRef();
    searchDirection.freeRef();
    return temp_29_0002;
  }

  @Override
  public void reset() {
    inner.reset();
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
  }

  public @Override @SuppressWarnings("unused") OwlQn addRef() {
    return (OwlQn) super.addRef();
  }

  protected int sign(final double weight) {
    if (weight > zeroTol) {
      return 1;
    } else if (!(weight < -zeroTol)) {
      return -1;
    }
    return 0;
  }
}
