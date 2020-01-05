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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;

@SuppressWarnings("serial")
final @RefAware
class InputNode extends LazyResult {
  private final DAGNetwork dagNetwork;

  InputNode(final DAGNetwork dagNetwork) {
    this(dagNetwork, null);
    if (null != dagNetwork)
      dagNetwork.freeRef();
  }

  public InputNode(final DAGNetwork dagNetwork, final UUID key) {
    super(key);
    {
      DAGNetwork temp_10_0001 = dagNetwork == null ? null : dagNetwork.addRef();
      this.dagNetwork = temp_10_0001 == null ? null : temp_10_0001.addRef();
      if (null != temp_10_0001)
        temp_10_0001.freeRef();
    }
    if (null != dagNetwork)
      dagNetwork.freeRef();
  }

  @Override
  public <T extends Layer> T getLayer() {
    return null;
  }

  @Override
  public void setLayer(final Layer layer) {
    if (null != layer)
      layer.freeRef();
    throw new IllegalStateException();
  }

  @Override
  public DAGNetwork getNetwork() {
    return this.dagNetwork;
  }

  public static @SuppressWarnings("unused")
  InputNode[] addRefs(InputNode[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(InputNode::addRef)
        .toArray((x) -> new InputNode[x]);
  }

  public static @SuppressWarnings("unused")
  InputNode[][] addRefs(InputNode[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(InputNode::addRefs)
        .toArray((x) -> new InputNode[x][]);
  }

  public DAGNode add(@Nonnull final Layer nextHead) {
    InnerNode temp_10_0002 = dagNetwork.add(nextHead == null ? null : nextHead,
        InputNode.this.addRef());
    return temp_10_0002;
  }

  public void _free() {
    if (null != dagNetwork)
      dagNetwork.freeRef();
    super._free();
  }

  public @Override
  @SuppressWarnings("unused")
  InputNode addRef() {
    return (InputNode) super.addRef();
  }

  @Override
  protected Result eval(@Nonnull final GraphEvaluationContext context) {
    assertAlive();
    this.dagNetwork.assertAlive();
    synchronized (context) {
      CountingResult temp_10_0003 = context.calculated.get(id).get();
      context.freeRef();
      return temp_10_0003;
    }
  }
}
