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
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.network.DAGNetwork;
import com.simiacryptus.mindseye.opt.TrainingMonitor;
import com.simiacryptus.mindseye.opt.line.LineSearchCursor;
import com.simiacryptus.mindseye.opt.line.LineSearchPoint;
import com.simiacryptus.mindseye.opt.line.SimpleLineSearchCursor;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class OwlQn extends OrientationStrategyBase<LineSearchCursor> {
  @Nullable
  public final OrientationStrategy<?> inner;
  private double factor_L1 = 0.000;
  private double zeroTol = 1e-20;

  public OwlQn() {
    this(new LBFGS());
  }

  protected OwlQn(@Nullable final OrientationStrategy<?> inner) {
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

  public void setFactor_L1(double factor_L1) {
    this.factor_L1 = factor_L1;
  }

  public double getZeroTol() {
    return zeroTol;
  }

  public void setZeroTol(double zeroTol) {
    this.zeroTol = zeroTol;
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
  public LineSearchCursor orient(@Nullable final Trainable subject, @Nonnull final PointSample measurement,
                                 final TrainingMonitor monitor) {
    assert inner != null;
    @Nonnull final SimpleLineSearchCursor gradient = (SimpleLineSearchCursor) inner
        .orient(subject == null ? null : subject.addRef(), measurement.addRef(), monitor);
    assert gradient.direction != null;
    @Nonnull final DeltaSet<UUID> searchDirection = gradient.direction.copy();
    @Nonnull final DeltaSet<UUID> orthant = new DeltaSet<UUID>();
    RefMap<UUID, Delta<UUID>> temp_29_0006 = gradient.direction.getMap();
    RefSet<UUID> keySet = temp_29_0006.keySet();
    temp_29_0006.freeRef();
    RefList<Layer> layerSet = keySet.stream()
        .map(RefUtil.wrapInterface((Function<? super UUID, ? extends Layer>) id -> {
          assert subject != null;
          DAGNetwork layer = (DAGNetwork) subject.getLayer();
          RefMap<UUID, Layer> layersById = layer.getLayersById();
          Layer id_layer = layersById.get(id);
          layersById.freeRef();
          layer.freeRef();
          return id_layer;
        }, subject == null ? null : subject.addRef())).collect(RefCollectors.toList());
    keySet.freeRef();
    //RefCollection<Layer> layers = getLayers(layerSet == null ? null : layerSet.addRef());
    //for (@Nonnull final Layer layer : layers)
    {
      RefMap<UUID, Delta<UUID>> temp_29_0007 = gradient.direction.getMap();
      temp_29_0007
          .forEach(RefUtil.wrapInterface((layerId, layerDelta) -> {
            final double[] weights = layerDelta.target;
            @Nullable final double[] delta = layerDelta.getDelta();
            layerDelta.freeRef();
            Delta<UUID> layerDelta1 = searchDirection.get(layerId, weights);
            assert layerDelta1 != null;
            @Nullable final double[] searchDir = layerDelta1.getDelta();
            layerDelta1.freeRef();
            Delta<UUID> layerDelta2 = orthant.get(layerId, weights);
            assert layerDelta2 != null;
            @Nullable final double[] suborthant = layerDelta2.getDelta();
            layerDelta2.freeRef();
            assert searchDir != null;
            for (int i = 0; i < searchDir.length; i++) {
              final int positionSign = sign(weights[i]);
              assert delta != null;
              final int directionSign = sign(delta[i]);
              assert suborthant != null;
              suborthant[i] = 0 == positionSign ? directionSign : positionSign;
              searchDir[i] += factor_L1 * (weights[i] < 0 ? -1.0 : 1.0);
              if (sign(searchDir[i]) != directionSign) {
                searchDir[i] = delta[i];
              }
            }
          }, searchDirection.addRef(), orthant.addRef()));
      temp_29_0007.freeRef();
    }
    //layers.freeRef();
    if (null != layerSet)
      layerSet.freeRef();
    orthant.freeRef();
    gradient.freeRef();
    SimpleLineSearchCursor temp_29_0005 = new SimpleLineSearchCursor(subject, measurement, searchDirection) {

      @Nonnull
      @Override
      public LineSearchPoint step(final double alpha, final TrainingMonitor monitor) {
        origin.weights.stream().forEach(d -> {
          d.restore();
          d.freeRef();
        });
        assert direction != null;
        @Nonnull final DeltaSet<UUID> currentDirection = direction.copy();
        RefMap<UUID, Delta<UUID>> temp_29_0008 = direction.getMap();
        temp_29_0008.forEach(RefUtil.wrapInterface((BiConsumer<? super UUID, ? super Delta<UUID>>) (layer, buffer) -> {
          if (null == buffer.getDelta()) {
            buffer.freeRef();
            return;
          }
          Delta<UUID> layerDelta = currentDirection.get(layer, buffer.target);
          assert layerDelta != null;
          @Nullable final double[] currentDelta = layerDelta.getDelta();
          layerDelta.freeRef();
          for (int i = 0; i < buffer.getDelta().length; i++) {
            final double prevValue = buffer.target[i];
            final double newValue = prevValue + buffer.getDelta()[i] * alpha;
            if (sign(prevValue) != 0 && sign(prevValue) != sign(newValue)) {
              assert currentDelta != null;
              currentDelta[i] = 0;
              buffer.target[i] = 0;
            } else {
              buffer.target[i] = newValue;
            }
          }
          buffer.freeRef();
        }, currentDirection.addRef()));
        temp_29_0008.freeRef();
        assert subject != null;
        PointSample temp_29_0009 = subject.measure(monitor);
        temp_29_0009.setRate(alpha);
        @Nonnull final PointSample measure = afterStep(temp_29_0009.addRef());
        temp_29_0009.freeRef();
        double dot = currentDirection.dot(measure.delta.addRef());
        currentDirection.freeRef();
        LineSearchPoint temp_29_0003 = new LineSearchPoint(measure.addRef(), dot);
        measure.freeRef();
        return temp_29_0003;
      }

      public @SuppressWarnings("unused")
      void _free() {
      }
    };
    temp_29_0005.setDirectionType("OWL/QN");
    SimpleLineSearchCursor temp_29_0002 = temp_29_0005.addRef();
    temp_29_0005.freeRef();
    return temp_29_0002;
  }

  @Override
  public void reset() {
    assert inner != null;
    inner.reset();
  }

  public void _free() {
    if (null != inner)
      inner.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  OwlQn addRef() {
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
