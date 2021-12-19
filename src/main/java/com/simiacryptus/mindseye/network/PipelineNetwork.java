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

import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.lang.DataSerializer;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.SerialPrecision;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.mindseye.layers.ValueLayer;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefFunction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * The type Pipeline network.
 */
@SuppressWarnings("serial")
public class PipelineNetwork extends DAGNetwork {
  @Nullable
  private DAGNode head;

  /**
   * Instantiates a new Pipeline network.
   */
  public PipelineNetwork() {
    this(1);
  }

  /**
   * Instantiates a new Pipeline network.
   *
   * @param inputs the inputs
   * @param layers the layers
   */
  public PipelineNetwork(final int inputs, @Nonnull final Layer... layers) {
    super(inputs);
    RefArrays.stream(layers).forEach(layer -> RefUtil.freeRef(add(layer)));
  }

  /**
   * Instantiates a new Pipeline network.
   *
   * @param json the json
   * @param rs   the rs
   */
  protected PipelineNetwork(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
    @Nonnull final UUID headId = UUID.fromString(json.get("head").getAsString());
    if (!inputHandles.contains(headId)) {
      setHead(getNodeById(headId));
    }
  }

  /**
   * Instantiates a new Pipeline network.
   *
   * @param layers the layers
   */
  public PipelineNetwork(@Nullable final Layer... layers) {
    this();
    addAll(RefUtil.addRef(layers));
    if (null != layers)
      RefUtil.freeRef(layers);
  }

  /**
   * Instantiates a new Pipeline network.
   *
   * @param inputs the inputs
   * @param id     the id
   * @param name   the name
   */
  public PipelineNetwork(int inputs, UUID id, String name) {
    super(inputs, id, name);
  }

  @NotNull
  @Override
  public final DAGNode getHead() {
    assertAlive();
    if (null == head) {
      return getInput(0);
    } else {
      return head.addRef();
    }
  }

  /**
   * Sets head.
   *
   * @param obj the obj
   */
  public synchronized void setHead(@Nullable final DAGNode obj) {
    if (obj != head) {
      if (null != head)
        head.freeRef();
      head = obj;
    } else {
      if (null != obj)
        obj.freeRef();
    }
  }

  /**
   * From json pipeline network.
   *
   * @param json the json
   * @param rs   the rs
   * @return the pipeline network
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static PipelineNetwork fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new PipelineNetwork(json, rs);
  }

  /**
   * Build pipeline network.
   *
   * @param inputs the inputs
   * @param layers the layers
   * @return the pipeline network
   */
  @Nonnull
  public static PipelineNetwork build(final int inputs, @Nonnull final Layer... layers) {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(inputs);
    for (final Layer layer : layers) {
      layer.assertAlive();
      RefUtil.freeRef(pipelineNetwork.add(layer == null ? null : layer.addRef()));
    }
    RefUtil.freeRef(layers);
    return pipelineNetwork;
  }

  /**
   * Combine pipeline network.
   *
   * @param combiner the combiner
   * @param networks the networks
   * @return the pipeline network
   */
  public static PipelineNetwork combine(@Nullable Layer combiner, @Nonnull PipelineNetwork... networks) {
    assert RefArrays.stream(RefUtil.addRef(networks)).allMatch(pipelineNetwork1 -> {
      boolean alive = pipelineNetwork1.assertAlive();
      pipelineNetwork1.freeRef();
      return alive;
    });
    if (1 == networks.length) {
      if (null != combiner)
        combiner.freeRef();
      PipelineNetwork net0 = networks[0].addRef();
      RefUtil.freeRef(networks);
      return net0;
    }
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1);
    RefUtil.freeRef(pipelineNetwork.add(combiner,
        RefArrays.stream(networks).map(RefUtil.wrapInterface((RefFunction<PipelineNetwork, DAGNode>) network -> {
          return pipelineNetwork.transferNode(network, network.getHead());
        })).toArray(DAGNode[]::new)));
    return pipelineNetwork;
  }

  /**
   * Sequence pipeline network.
   *
   * @param networks the networks
   * @return the pipeline network
   */
  public static PipelineNetwork sequence(@Nonnull PipelineNetwork... networks) {
    assert RefArrays.stream(RefUtil.addRef(networks)).allMatch(pipelineNetwork1 -> {
      boolean alive = pipelineNetwork1.assertAlive();
      pipelineNetwork1.freeRef();
      return alive;
    });
    if (1 == networks.length) {
      PipelineNetwork temp_06_0011 = networks[0].addRef();
      RefUtil.freeRef(networks);
      return temp_06_0011;
    }
    if (1 != networks[0].inputHandles.size()) {
      RefUtil.freeRef(networks);
      throw new IllegalArgumentException();
    }
    if (1 != RefArrays.stream(RefUtil.addRef(networks)).mapToInt(x -> {
      int temp_06_0008 = x.inputHandles.size();
      x.freeRef();
      return temp_06_0008;
    }).distinct().count()) {
      RefUtil.freeRef(networks);
      throw new IllegalArgumentException();
    }
    PipelineNetwork pipelineNetwork = new PipelineNetwork(networks[0].inputHandles.size());
    for (PipelineNetwork network : networks) {
      RefUtil.freeRef(pipelineNetwork.transferNode(network.addRef(), network.getHead()));
    }
    RefUtil.freeRef(networks);
    return pipelineNetwork;
  }

  /**
   * Gets copy.
   *
   * @param network the network
   * @return the copy
   */
  public static PipelineNetwork getCopy(PipelineNetwork network) {
    if (null == network) {
      return null;
    }
    try {
      return network.copyPipeline();
    } finally {
      network.freeRef();
    }
  }

  @Nonnull
  @Override
  public PipelineNetwork copy(final SerialPrecision precision) {
    return (PipelineNetwork) super.copy(precision);
  }

  @Nonnull
  @Override
  public PipelineNetwork copy() {
    return (PipelineNetwork) super.copy();
  }

  /**
   * Add inner node.
   *
   * @param nextHead the next head
   * @return the inner node
   */
  @Nonnull
  public InnerNode add(@Nullable final Layer nextHead) {
    assert nextHead != null;
    assert nextHead.assertAlive();
    return add(nextHead, getHead());
  }

  @Nonnull
  @Override
  public InnerNode add(@Nullable final Layer nextHead, @Nonnull final DAGNode... head) {
    assert nextHead != null;
    @Nullable final InnerNode node = super.add(nextHead, head);
    setHead(node.addRef());
    return node;
  }

  @Nonnull
  @SafeVarargs
  @Override
  public final InnerNode add(final CharSequence label, @Nullable final Layer layer, final DAGNode... head) {
    assert layer != null;
    final InnerNode node = super.add(label, layer, head);
    setHead(node.addRef());
    return node;
  }

  /**
   * Add all.
   *
   * @param node   the node
   * @param layers the layers
   */
  public void addAll(DAGNode node, @Nonnull final Layer... layers) {
    for (final Layer l : layers) {
      node = add(l == null ? null : l.addRef(), node);
    }
    RefUtil.freeRef(layers);
    if (null != node)
      node.freeRef();
  }

  /**
   * Add all.
   *
   * @param layers the layers
   */
  public void addAll(@Nullable final Layer... layers) {
    addAll(getHead(), RefUtil.addRef(layers));
    if (null != layers)
      RefUtil.freeRef(layers);
  }

  /**
   * Const value dag node.
   *
   * @param tensor the tensor
   * @return the dag node
   */
  @Nullable
  public DAGNode constValue(@Nullable final Tensor tensor) {
    return add(new ValueLayer(tensor), new DAGNode[]{});
  }

  /**
   * Const value wrap dag node.
   *
   * @param tensor the tensor
   * @return the dag node
   */
  @Nullable
  public DAGNode constValueWrap(@Nullable final Tensor tensor) {
    if (tensor == null) {
      return constValue(null);
    }
    else {
      return constValue(tensor);
    }
  }

  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    assertConsistent();
    final JsonObject json = super.getJson(resources, dataSerializer);
    assert json != null;
    json.addProperty("head", getHeadId().toString());
    return json;
  }

  @Nonnull
  @Override
  public PipelineNetwork andThen(@Nonnull Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    RefUtil.freeRef(add(append.addRef()));
    append.freeRef();
    return this.addRef();
  }

  /**
   * Copy pipeline pipeline network.
   *
   * @return the pipeline network
   */
  @Nullable
  public final PipelineNetwork copyPipeline() {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1, getId(), getName());
    if (!this.internalNodes.isEmpty()) {
      InnerNode node = pipelineNetwork.transferNode(this.addRef(), getHead());
      pipelineNetwork.setHead(node);
    }
    return pipelineNetwork;
  }

  public void _free() {
    super._free();
    if (null != head) {
      head.freeRef();
      head = null;
    }
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  PipelineNetwork addRef() {
    return (PipelineNetwork) super.addRef();
  }

}
