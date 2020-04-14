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

package com.simiacryptus.mindseye.network;

import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

/**
 * The interface Dag node.
 */
public interface DAGNode extends Serializable, ReferenceCounting {
  /**
   * Gets id.
   *
   * @return the id
   */
  UUID getId();

  /**
   * Get inputs dag node [ ].
   *
   * @return the dag node [ ]
   */
  @Nonnull
  default DAGNode[] getInputs() {
    return new DAGNode[]{};
  }

  /**
   * Gets layer.
   *
   * @param <T> the type parameter
   * @return the layer
   */
  @Nullable <T extends Layer> T getLayer();

  /**
   * Sets layer.
   *
   * @param layer the layer
   */
  void setLayer(Layer layer);

  /**
   * Get result.
   *
   * @param buildExeCtx the build exe ctx
   * @param consumer    the consumer
   * @return the result
   */
  @Nullable
  Result get(GraphEvaluationContext buildExeCtx, Layer consumer);

  /**
   * Free.
   */
  void _free();

  @Nonnull
  DAGNode addRef();

}