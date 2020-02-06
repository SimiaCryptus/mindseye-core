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
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings("serial")
public final class InnerNode extends LazyResult {
  @SuppressWarnings("unused")
  //public final CharSequence[] createdBy = Util.currentStack();
  @Nonnull
  private final DAGNode[] inputNodes;
  @Nullable
  private volatile Layer layer;
  private boolean parallel = true;

  @SafeVarargs
  InnerNode(@Nonnull final Layer layer, final DAGNode... inputNodes) {
    this(layer, UUID.randomUUID(), inputNodes);
  }

  @SafeVarargs
  InnerNode(@Nonnull final Layer layer, final UUID key,
            @Nonnull final DAGNode... inputNodes) {
    super(key);
    setLayer(layer);
    if (0 == inputNodes.length) {
      this.inputNodes = new DAGNode[]{};
      RefUtil.freeRef(inputNodes);
    } else {
      assert RefUtil.assertAlive(inputNodes);
      this.inputNodes = RefArrays.copyOf(inputNodes, inputNodes.length);
    }
  }

  @Nonnull
  @Override
  public DAGNode[] getInputs() {
    return RefUtil.addRefs(inputNodes);
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
    newLayer.assertAlive();
    Layer prevLayer = null == this.layer ? null : this.layer.addRef();
    if (newLayer != prevLayer) {
      Layer temp_12_0004 = newLayer.addRef();
      if (null != this.layer)
        this.layer.freeRef();
      this.layer = temp_12_0004 == null ? null : temp_12_0004.addRef();
      if (null != temp_12_0004)
        temp_12_0004.freeRef();
    }
    newLayer.freeRef();
    if (null != prevLayer)
      prevLayer.freeRef();
  }

  public boolean isParallel() {
    return parallel;
  }

  public void setParallel(boolean parallel) {
    this.parallel = parallel;
  }


  public void _free() {
    if (null != layer) {
      layer.freeRef();
      layer = null;
    }
    RefUtil.freeRef(inputNodes);
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
    @Nonnull
    RefStream<DAGNode> stream = RefArrays.stream(RefUtil.addRefs(inputNodes));
    if (!CoreSettings.INSTANCE().isSingleThreaded() && parallel)
      stream = stream.parallel();
    final Result[] in = stream.map(RefUtil.wrapInterface((Function<? super DAGNode, ? extends Result>) x -> {
      Result temp_12_0010 = x == null ? null : x.get(ctx == null ? null : ctx.addRef());
      if (null != x)
        x.freeRef();
      return temp_12_0010;
    }, ctx)).toArray(Result[]::new);
    for (Result result : in) {
      assert result != null;
    }
    Result temp_12_0008 = innerLayer.eval(in);
    innerLayer.freeRef();
    return temp_12_0008;
  }
}
