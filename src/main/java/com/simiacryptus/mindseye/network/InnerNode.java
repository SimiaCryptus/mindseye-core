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

import com.simiacryptus.mindseye.lang.CoreSettings;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.Result;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

@SuppressWarnings("serial")
public final @RefAware
class InnerNode extends LazyResult {
  @SuppressWarnings("unused")
  public final CharSequence[] createdBy = Util.currentStack();
  private final DAGNetwork dagNetwork;
  @Nonnull
  private final DAGNode[] inputNodes;
  private volatile Layer layer;
  private boolean parallel = true;

  @SafeVarargs
  InnerNode(final DAGNetwork dagNetwork, @Nonnull final Layer layer, final DAGNode... inputNodes) {
    this(dagNetwork, layer, UUID.randomUUID(), inputNodes);
  }

  @SafeVarargs
  InnerNode(final DAGNetwork dagNetwork, @Nonnull final Layer layer, final UUID key,
            @Nonnull final DAGNode... inputNodes) {
    super(key);
    this.dagNetwork = dagNetwork;
    assert null != inputNodes;
    setLayer(layer);
    if (0 == inputNodes.length) {
      this.inputNodes = new DAGNode[]{};
    } else {
      this.inputNodes = RefArrays.copyOf(inputNodes, inputNodes.length);
      assert RefArrays.stream(inputNodes).parallel().allMatch(x -> x != null);
      assert RefArrays.stream(inputNodes).parallel().allMatch(x -> x.assertAlive());
    }
  }

  @Nonnull
  @Override
  public DAGNode[] getInputs() {
    return inputNodes;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  @Override
  public <T extends Layer> T getLayer() {
    return (T) layer;
  }

  @Override
  public synchronized void setLayer(@Nonnull final Layer newLayer) {
    assertAlive();
    dagNetwork.assertAlive();
    newLayer.assertAlive();
    Layer prevLayer = this.layer;
    if (newLayer != prevLayer) {
      this.layer = newLayer;
      dagNetwork.assertConsistent();
    }
  }

  @Override
  public DAGNetwork getNetwork() {
    return dagNetwork;
  }

  public boolean isParallel() {
    return parallel;
  }

  public InnerNode setParallel(boolean parallel) {
    this.parallel = parallel;
    return this;
  }

  public static @SuppressWarnings("unused")
  InnerNode[] addRefs(InnerNode[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(InnerNode::addRef)
        .toArray((x) -> new InnerNode[x]);
  }

  public static @SuppressWarnings("unused")
  InnerNode[][] addRefs(InnerNode[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(InnerNode::addRefs)
        .toArray((x) -> new InnerNode[x][]);
  }

  public void _free() {
    super._free();
    if (null != this.inputNodes) {
      RefArrays.fill(this.inputNodes, null);
    }
    this.layer = null;
  }

  public @Override
  @SuppressWarnings("unused")
  InnerNode addRef() {
    return (InnerNode) super.addRef();
  }

  @Nullable
  @Override
  protected Result eval(final GraphEvaluationContext ctx) {
    assertAlive();
    @Nonnull final Layer innerLayer = getLayer();
    assert RefArrays.stream(inputNodes).allMatch(x -> x != null);
    @Nonnull
    RefStream<DAGNode> stream = RefArrays
        .stream(inputNodes);
    if (!CoreSettings.INSTANCE().isSingleThreaded() && parallel)
      stream = stream.parallel();
    final Result[] in = stream.map(x -> x == null ? null : x.get(ctx)).toArray(i -> new Result[i]);
    assert RefArrays.stream(in).allMatch(x -> x != null);
    return innerLayer.eval(in);
  }
}
