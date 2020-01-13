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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public interface DAGNode extends Serializable, ReferenceCounting {
  UUID getId();

  @Nonnull
  default DAGNode[] getInputs() {
    return new DAGNode[] {};
  }

  @Nullable
  <T extends Layer> T getLayer();

  void setLayer(Layer layer);

  DAGNetwork getNetwork();

  public static @SuppressWarnings("unused") DAGNode[] addRefs(DAGNode[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DAGNode::addRef).toArray((x) -> new DAGNode[x]);
  }

  public static @SuppressWarnings("unused") DAGNode[][] addRefs(DAGNode[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DAGNode::addRefs).toArray((x) -> new DAGNode[x][]);
  }

  @Nullable
  Result get(GraphEvaluationContext buildExeCtx);

  public void _free();

  public DAGNode addRef();

}