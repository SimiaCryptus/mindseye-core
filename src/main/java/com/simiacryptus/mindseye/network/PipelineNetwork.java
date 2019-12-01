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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.mindseye.lang.DataSerializer;
import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.SerialPrecision;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.mindseye.layers.ValueLayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class PipelineNetwork extends DAGNetwork {
  @Nullable
  private DAGNode head;

  public PipelineNetwork() {
    this(1);
  }

  public PipelineNetwork(final int inputs, @Nonnull final Layer... layers) {
    super(inputs);
    for (final Layer layer : layers) {
      add(layer).freeRef();
    }
  }

  protected PipelineNetwork(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
    @Nonnull final UUID headId = UUID.fromString(json.get("head").getAsString());
    if (!inputHandles.contains(headId)) {
      assert null != headId;
      DAGNode node = getNodeById(headId);
      setHead(node);
      node.freeRef();
    }
  }

  public PipelineNetwork(final Layer... layers) {
    this();
    addAll(layers);
  }

  public PipelineNetwork(int inputs, UUID id, String name) {
    super(inputs, id, name);
  }

  public static PipelineNetwork fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new PipelineNetwork(json, rs);
  }

  public static PipelineNetwork build(final int inputs, final Layer... layers) {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(inputs);
    for (final Layer layer : layers) {
      pipelineNetwork.add(layer).freeRef();
    }
    return pipelineNetwork;
  }

  public static PipelineNetwork wrap(final int inputs, final Layer... layers) {
    assert Arrays.stream(layers).allMatch(ReferenceCounting::assertAlive);
    PipelineNetwork pipelineNetwork = new PipelineNetwork(inputs);
    for (final Layer layer : layers) {
      pipelineNetwork.wrap(layer).freeRef();
    }
    return pipelineNetwork;
  }

  public static InnerNode transferNode(PipelineNetwork pipelineNetwork, DAGNode node) {
    try {
      final DAGNode[] dagNodes = Arrays.stream(node.getInputs()).map((DAGNode input) -> {
        final PipelineNetwork inputNetwork = (PipelineNetwork) input.getNetwork();
        final UUID inputId = input.getId();
        if (inputNetwork.inputNodes.containsKey(inputId)) {
          return pipelineNetwork.getInput(inputNetwork.inputHandles.indexOf(inputId));
        } else {
          Layer inputLayer = input.getLayer();
          if (inputLayer == null) throw new IllegalArgumentException(input.getClass().toString());
          return pipelineNetwork.getNodes().stream().filter(dagNode -> {
            Layer layer = dagNode.getLayer();
            if (null == layer) return false;
            return layer.getId().equals(inputLayer.getId());
          }).findFirst().map(DAGNode::addRef).orElseGet(() -> {
            int inputNumber = inputNetwork.inputNodes.keySet().stream().collect(Collectors.toList()).indexOf(inputId);
            if (-1 == inputNumber) {
              return transferNode(pipelineNetwork, input.addRef());
            } else {
              return pipelineNetwork.getInput(inputNumber);
            }
          });
        }
      }).toArray(i -> new DAGNode[i]);
      return pipelineNetwork.add(node.getLayer(), dagNodes);
    } finally {
      node.freeRef();
    }
  }

  public static PipelineNetwork combine(Layer combiner, PipelineNetwork... networks) {
    Arrays.stream(networks).forEach(ReferenceCountingBase::assertAlive);
    if (1 == networks.length) return networks[0];
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1);
    pipelineNetwork.wrap(combiner, Arrays.stream(networks).map(network -> {
      InnerNode node = transferNode(pipelineNetwork, network.getHead());
      network.freeRef();
      return node;
    }).toArray(i -> new DAGNode[i])).freeRef();
    return pipelineNetwork;
  }

  public static PipelineNetwork sequence(PipelineNetwork... networks) {
    Arrays.stream(networks).forEach(ReferenceCountingBase::assertAlive);
    if (1 == networks.length) return networks[0];
    if (1 != networks[0].inputHandles.size()) throw new IllegalArgumentException();
    if (1 != Arrays.stream(networks).mapToInt(x -> x.inputHandles.size()).distinct().count()) throw new IllegalArgumentException();
    PipelineNetwork pipelineNetwork = new PipelineNetwork(networks[0].inputHandles.size());
    for (PipelineNetwork network : networks) {
      transferNode(pipelineNetwork, network.getHead()).freeRef();
    }
    return pipelineNetwork;
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

  @Nullable
  public InnerNode add(@Nullable final Layer nextHead) {
    assert nextHead.assertAlive();
    if (null == nextHead) return null;
    return add(nextHead, getHead());
  }

  @Nullable
  public InnerNode wrap(@Nullable final Layer nextHead) {
    @Nullable InnerNode add = add(nextHead);
    nextHead.freeRef();
    return add;
  }

  @Nullable
  public InnerNode wrap(@Nullable final Layer nextHead, @Nonnull final DAGNode... head) {
    @Nullable InnerNode add = add(nextHead, head);
    nextHead.freeRef();
    return add;
  }

  @Nullable
  public DAGNode wrap(final CharSequence label, @Nullable final Layer nextHead, @Nonnull final DAGNode... head) {
    DAGNode add = add(label, nextHead, head);
    nextHead.freeRef();
    return add;
  }

  @Nullable
  @Override
  public InnerNode add(@Nullable final Layer nextHead, @Nonnull final DAGNode... head) {
    @Nullable final InnerNode node = super.add(nextHead, head);
    setHead(node);
    return node;
  }

  @SafeVarargs
  @Override
  public final InnerNode add(final CharSequence label, @Nullable final Layer layer, final DAGNode... head) {
    final InnerNode node = super.add(label, layer, head);
    setHead(node);
    return node;
  }

  public InnerNode addAll(InnerNode node, @Nonnull final Layer... layers) {
    for (final Layer l : layers) {
      node = add(l, node);
    }
    return node;
  }

  public InnerNode addAll(final Layer... layers) {
    return addAll((InnerNode) getHead(), layers);
  }

  @Nullable
  public DAGNode constValue(final Tensor tensor) {
    return super.wrap(new ValueLayer(tensor));
  }

  @Nullable
  public DAGNode constValueWrap(final Tensor tensor) {
    DAGNode node = constValue(tensor);
    tensor.freeRef();
    return node;
  }

  @Override
  protected void _free() {
    super._free();
    if (null != head) {
      head.freeRef();
      head = null;
    }
  }

  @Nullable
  @Override
  public final DAGNode getHead() {
    assertAlive();
    if (null == head) {
      return getInput(0);
    } else {
      head.addRef();
      return head;
    }
  }

  @Nonnull
  public DAGNode setHead(final DAGNode obj) {
    if (obj != head) {
      if (null != head) head.freeRef();
      head = obj;
      if (null != head) head.addRef();
    }
    return obj;
  }

  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    assertConsistent();
    final JsonObject json = super.getJson(resources, dataSerializer);
    json.addProperty("head", getHeadId().toString());
    return json;
  }

  @Override
  public PipelineNetwork andThenWrap(Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    wrap(append).freeRef();
    return this;
  }

  @Override
  public PipelineNetwork addRef() {
    return (PipelineNetwork) super.addRef();
  }

  public PipelineNetwork copyPipeline() {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1, getId(), getName());
    if (!this.internalNodes.isEmpty()) pipelineNetwork.setHead(transferNode(pipelineNetwork, getHead())).freeRef();
    return pipelineNetwork;
  }

}
