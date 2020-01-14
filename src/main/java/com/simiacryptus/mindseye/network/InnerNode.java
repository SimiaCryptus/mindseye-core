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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings("serial")
public final class InnerNode extends LazyResult {
  @SuppressWarnings("unused")
  public final CharSequence[] createdBy = Util.currentStack();
  @Nullable
  private final DAGNetwork dagNetwork;
  @Nonnull
  private final DAGNode[] inputNodes;
  @Nullable
  private volatile Layer layer;
  private boolean parallel = true;

  @SafeVarargs
  InnerNode(final DAGNetwork dagNetwork, @Nonnull final Layer layer, final DAGNode... inputNodes) {
    this(dagNetwork, layer, UUID.randomUUID(), inputNodes);
  }

  @SafeVarargs
  InnerNode(@Nullable final DAGNetwork dagNetwork, @Nonnull final Layer layer, final UUID key,
            @Nonnull final DAGNode... inputNodes) {
    super(key);
    DAGNetwork temp_12_0001 = dagNetwork == null ? null : dagNetwork.addRef();
    this.dagNetwork = temp_12_0001 == null ? null : temp_12_0001.addRef();
    if (null != temp_12_0001)
      temp_12_0001.freeRef();
    if (null != dagNetwork)
      dagNetwork.freeRef();
    setLayer(layer);
    if (0 == inputNodes.length) {
      DAGNode[] temp_12_0002 = new DAGNode[]{};
      this.inputNodes = DAGNode.addRefs(temp_12_0002);
      ReferenceCounting.freeRefs(temp_12_0002);
    } else {
      DAGNode[] temp_12_0003 = RefArrays.copyOf(DAGNode.addRefs(inputNodes), inputNodes.length);
      this.inputNodes = DAGNode.addRefs(temp_12_0003);
      ReferenceCounting.freeRefs(temp_12_0003);
      assert RefArrays.stream(DAGNode.addRefs(inputNodes)).parallel().allMatch(x -> {
        boolean temp_12_0006 = x != null;
        if (null != x)
          x.freeRef();
        return temp_12_0006;
      });
      assert RefArrays.stream(DAGNode.addRefs(inputNodes)).parallel().allMatch(x -> {
        boolean temp_12_0007 = x.assertAlive();
        x.freeRef();
        return temp_12_0007;
      });
    }
    ReferenceCounting.freeRefs(inputNodes);
  }

  @Nonnull
  @Override
  public DAGNode[] getInputs() {
    return DAGNode.addRefs(inputNodes);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  @Override
  public <T extends Layer> T getLayer() {
    return (T) layer.addRef();
  }

  @Override
  public synchronized void setLayer(@Nonnull final Layer newLayer) {
    assertAlive();
    assert dagNetwork != null;
    dagNetwork.assertAlive();
    newLayer.assertAlive();
    Layer prevLayer = this.layer.addRef();
    if (newLayer != prevLayer) {
      Layer temp_12_0004 = newLayer.addRef();
      if (null != this.layer)
        this.layer.freeRef();
      this.layer = temp_12_0004 == null ? null : temp_12_0004.addRef();
      if (null != temp_12_0004)
        temp_12_0004.freeRef();
      dagNetwork.assertConsistent();
    }
    newLayer.freeRef();
    if (null != prevLayer)
      prevLayer.freeRef();
  }

  @Nullable
  @Override
  public DAGNetwork getNetwork() {
    return dagNetwork == null ? null : dagNetwork.addRef();
  }

  public boolean isParallel() {
    return parallel;
  }

  @Nonnull
  public InnerNode setParallel(boolean parallel) {
    this.parallel = parallel;
    return this.addRef();
  }

  @Nullable
  public static @SuppressWarnings("unused")
  InnerNode[] addRefs(@Nullable InnerNode[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(InnerNode::addRef).toArray((x) -> new InnerNode[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  InnerNode[][] addRefs(@Nullable InnerNode[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(InnerNode::addRefs).toArray((x) -> new InnerNode[x][]);
  }

  public void _free() {
    if (null != layer) {
      layer.freeRef();
      layer = null;
    }
    ReferenceCounting.freeRefs(inputNodes);
    if (null != dagNetwork)
      dagNetwork.freeRef();
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  InnerNode addRef() {
    return (InnerNode) super.addRef();
  }

  @Nullable
  @Override
  protected Result eval(@Nullable final GraphEvaluationContext ctx) {
    assertAlive();
    @Nonnull final Layer innerLayer = getLayer();
    assert RefArrays.stream(DAGNode.addRefs(inputNodes)).allMatch(x -> {
      boolean temp_12_0009 = x != null;
      if (null != x)
        x.freeRef();
      return temp_12_0009;
    });
    @Nonnull
    RefStream<DAGNode> stream = RefArrays.stream(DAGNode.addRefs(inputNodes));
    if (!CoreSettings.INSTANCE().isSingleThreaded() && parallel)
      stream = stream.parallel();
    final Result[] in = stream.map(RefUtil.wrapInterface((Function<? super DAGNode, ? extends Result>) x -> {
      Result temp_12_0010 = x == null ? null : x.get(ctx == null ? null : ctx.addRef());
      if (null != x)
        x.freeRef();
      return temp_12_0010;
    }, ctx == null ? null : ctx.addRef())).toArray(i -> new Result[i]);
    if (null != ctx)
      ctx.freeRef();
    assert RefArrays.stream(Result.addRefs(in)).allMatch(x -> {
      boolean temp_12_0011 = x != null;
      if (null != x)
        x.freeRef();
      return temp_12_0011;
    });
    Result temp_12_0008 = innerLayer.eval(Result.addRefs(in));
    ReferenceCounting.freeRefs(in);
    innerLayer.freeRef();
    return temp_12_0008;
  }
}
