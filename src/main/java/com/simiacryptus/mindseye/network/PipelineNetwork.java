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
 * This class represents a pipeline network, which is a directed acyclic graph (DAG) of nodes.
 * The head node is the starting point of the pipeline and can be null.
 *
 * @docgenVersion 9
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

  /**
   * @return the head layer, or null if there is no head layer
   * @docgenVersion 9
   */
  @Nullable
  public Layer headLayer() {
    DAGNode head = getHead();
    freeRef();
    Layer layer = head.getLayer();
    head.freeRef();
    return layer;
  }

  /**
   * @NotNull
   * @Override public final DAGNode getHead() {
   * assertAlive();
   * if (null == head) {
   * return getInput(0);
   * } else {
   * return head.addRef();
   * }
   * }
   * @docgenVersion 9
   */
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
   * Sets the head of the DAG.
   *
   * @param obj the new head of the DAG
   * @docgenVersion 9
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
   * Creates a new {@link PipelineNetwork} from the given JSON object.
   *
   * @param json the JSON object to create the network from
   * @param rs   the map of character sequences to byte arrays
   * @return the newly created network
   * @docgenVersion 9
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static PipelineNetwork fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new PipelineNetwork(json, rs);
  }

  /**
   * Builds a pipeline network with the given inputs and layers.
   *
   * @param inputs the number of inputs
   * @param layers the layers
   * @return the pipeline network
   * @docgenVersion 9
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
   * Combines the given networks using the specified combiner layer.
   *
   * @param combiner the layer to use for combining
   * @param networks the networks to combine
   * @return the combined network
   * @docgenVersion 9
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
   * Sequences the given networks.
   *
   * @param networks the networks to sequence
   * @return the sequenced network
   * @docgenVersion 9
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
   * Returns a copy of the given pipeline network. If the network is null,
   * returns null.
   *
   * @docgenVersion 9
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

  /**
   * Returns a copy of this pipeline network with the specified precision.
   *
   * @param precision the precision of the copy
   * @return the copy of this pipeline network
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public PipelineNetwork copy(final SerialPrecision precision) {
    return (PipelineNetwork) super.copy(precision);
  }

  /**
   * Returns a copy of this pipeline network.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public PipelineNetwork copy() {
    return (PipelineNetwork) super.copy();
  }

  /**
   * Adds the given node to the head of the list.
   *
   * @param nextHead the node to add
   * @return the added node
   * @docgenVersion 9
   */
  @Nonnull
  public InnerNode add(@Nullable final Layer nextHead) {
    assert nextHead != null;
    assert nextHead.assertAlive();
    return add(nextHead, getHead());
  }

  /**
   * Adds the next head to the DAG.
   *
   * @param nextHead the next head to add
   * @param head     the current head of the DAG
   * @return the new head of the DAG
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public InnerNode add(@Nullable final Layer nextHead, @Nonnull final DAGNode... head) {
    assert nextHead != null;
    @Nullable final InnerNode node = super.add(nextHead, head);
    setHead(node.addRef());
    return node;
  }

  /**
   * Adds a node with the given label and layer, and makes it the head node.
   *
   * @param label the label for the new node
   * @param layer the layer for the new node
   * @param head  the head node(s) for the new node
   * @return the new node
   * @docgenVersion 9
   */
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
   * Adds all the given layers to the given node.
   *
   * @param node   the node to add the layers to
   * @param layers the layers to add
   * @docgenVersion 9
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
   * Adds all the given layers to this layer.
   *
   * @param layers the layers to add
   * @docgenVersion 9
   */
  public void addAll(@Nullable final Layer... layers) {
    addAll(getHead(), RefUtil.addRef(layers));
    if (null != layers)
      RefUtil.freeRef(layers);
  }

  /**
   * @param tensor the tensor to use as a value, or null to use a default tensor
   * @return the resulting DAGNode
   * @docgenVersion 9
   */
  @Nullable
  public DAGNode constValue(@Nullable final Tensor tensor) {
    return add(new ValueLayer(tensor), new DAGNode[]{});
  }

  /**
   * @param tensor the tensor to use for the value layer
   * @return the DAGNode for the value layer
   * @docgenVersion 9
   */
  @Nullable
  public DAGNode mutableValue(@Nullable final Tensor tensor) {
    ValueLayer valueLayer = new ValueLayer(tensor);
    valueLayer.setFrozen(false);
    return add(valueLayer, new DAGNode[]{});
  }

  /**
   * @param tensor the tensor to wrap
   * @return the DAGNode representing the given tensor, or null if the tensor is null
   * @docgenVersion 9
   */
  @Nullable
  public DAGNode constValueWrap(@Nullable final Tensor tensor) {
    // ??? Why does this method exist?

//    if (tensor == null) {
//      return constValue(null);
//    }
//    else {
//    }

    if (null == tensor) {
      RefUtil.freeRef(tensor);
      throw new IllegalArgumentException();
    }
    return constValue(tensor);
  }

  /**
   * Overrides the getJson method to assert that the object is in a consistent state
   * and to add a "head" property to the JSON object with the value of the headId.
   *
   * @docgenVersion 9
   */
  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    assertConsistent();
    final JsonObject json = super.getJson(resources, dataSerializer);
    assert json != null;
    json.addProperty("head", getHeadId().toString());
    return json;
  }

  /**
   * Adds the given layer to the end of this network and returns the resulting network.
   *
   * @param append the layer to add
   * @return the resulting network
   * @docgenVersion 9
   */
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
   * Returns a copy of the pipeline network.
   *
   * @return a copy of the pipeline network
   * @docgenVersion 9
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

  /**
   * Frees this object and all of its children.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
    if (null != head) {
      head.freeRef();
      head = null;
    }
  }

  /**
   * Adds a reference to this object.
   *
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  PipelineNetwork addRef() {
    return (PipelineNetwork) super.addRef();
  }

}
