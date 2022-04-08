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
 * This is the DAGNode interface.
 *
 * @docgenVersion 9
 */
public interface DAGNode extends Serializable, ReferenceCounting {
  /**
   * Returns the UUID of this object.
   *
   * @docgenVersion 9
   */
  UUID getId();

  /**
   * Returns an array of DAGNode objects that represent the inputs
   * to this node.
   *
   * @docgenVersion 9
   */
  @Nonnull
  default DAGNode[] getInputs() {
    return new DAGNode[]{};
  }

  /**
   * Returns the layer.
   *
   * @docgenVersion 9
   */
  @Nullable <T extends Layer> T getLayer();

  /**
   * Sets the layer.
   *
   * @docgenVersion 9
   */
  void setLayer(Layer layer);

  /**
   * Returns the result of the computation.
   *
   * @docgenVersion 9
   */
  @Nullable
  Result get(GraphEvaluationContext buildExeCtx, Layer consumer);

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * Adds a reference to the DAGNode and returns the DAGNode.
   *
   * @docgenVersion 9
   */
  @Nonnull
  DAGNode addRef();

  /**
   * Evaluates the result of the code.
   *
   * @docgenVersion 9
   */
  Result eval(GraphEvaluationContext t);

}