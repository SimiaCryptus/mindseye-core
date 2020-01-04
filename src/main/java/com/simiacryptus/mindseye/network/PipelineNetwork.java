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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@SuppressWarnings("serial")
public @com.simiacryptus.ref.lang.RefAware
class PipelineNetwork extends DAGNetwork {
  @Nullable
  private DAGNode head;

  public PipelineNetwork() {
    this(1);
  }

  public PipelineNetwork(final int inputs, @Nonnull final Layer... layers) {
    super(inputs);
    for (final Layer layer : layers) {
      add(layer);
    }
  }

  protected PipelineNetwork(@Nonnull final JsonObject json,
                            com.simiacryptus.ref.wrappers.RefMap<CharSequence, byte[]> rs) {
    super(json, rs);
    @Nonnull final UUID headId = UUID.fromString(json.get("head").getAsString());
    if (!inputHandles.contains(headId)) {
      assert null != headId;
      DAGNode node = getNodeById(headId);
      setHead(node);
    }
  }

  public PipelineNetwork(final Layer... layers) {
    this();
    addAll(layers);
  }

  public PipelineNetwork(int inputs, UUID id, String name) {
    super(inputs, id, name);
  }

  @Nullable
  @Override
  public final DAGNode getHead() {
    assertAlive();
    if (null == head) {
      return getInput(0);
    } else {
      return head;
    }
  }

  @Nonnull
  public void setHead(final DAGNode obj) {
    if (obj != head) {
      head = obj;
    }
  }

  @SuppressWarnings("unused")
  public static PipelineNetwork fromJson(@Nonnull final JsonObject json,
                                         com.simiacryptus.ref.wrappers.RefMap<CharSequence, byte[]> rs) {
    return new PipelineNetwork(json, rs);
  }

  public static PipelineNetwork build(final int inputs, final Layer... layers) {
    assert com.simiacryptus.ref.wrappers.RefArrays.stream(layers).allMatch(ReferenceCounting::assertAlive);
    PipelineNetwork pipelineNetwork = new PipelineNetwork(inputs);
    for (final Layer layer : layers) {
      pipelineNetwork.add(layer);
    }
    return pipelineNetwork;
  }

  public static InnerNode transferNode(PipelineNetwork pipelineNetwork, DAGNode node) {
    {
      final DAGNode[] dagNodes = com.simiacryptus.ref.wrappers.RefArrays.stream(node.getInputs())
          .map((DAGNode input) -> {
            final PipelineNetwork inputNetwork = (PipelineNetwork) input.getNetwork();
            final UUID inputId = input.getId();
            if (inputNetwork.inputNodes.containsKey(inputId)) {
              return pipelineNetwork.getInput(inputNetwork.inputHandles.indexOf(inputId));
            } else {
              Layer inputLayer = input.getLayer();
              if (inputLayer == null)
                throw new IllegalArgumentException(input.getClass().toString());
              return pipelineNetwork.getNodes().stream().filter(dagNode -> {
                Layer layer = dagNode.getLayer();
                if (null == layer)
                  return false;
                return layer.getId().equals(inputLayer.getId());
              }).findFirst().orElseGet(() -> {
                int inputNumber = inputNetwork.inputNodes.keySet().stream()
                    .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).indexOf(inputId);
                if (-1 == inputNumber) {
                  return transferNode(pipelineNetwork, input);
                } else {
                  return pipelineNetwork.getInput(inputNumber);
                }
              });
            }
          }).toArray(i -> new DAGNode[i]);
      return pipelineNetwork.add(node.getLayer(), dagNodes);
    }
  }

  public static PipelineNetwork combine(Layer combiner, PipelineNetwork... networks) {
    com.simiacryptus.ref.wrappers.RefArrays.stream(networks).forEach(ReferenceCountingBase::assertAlive);
    if (1 == networks.length)
      return networks[0];
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1);
    pipelineNetwork.add(combiner, com.simiacryptus.ref.wrappers.RefArrays.stream(networks).map(network -> {
      return transferNode(pipelineNetwork, network.getHead());
    }).toArray(i -> new DAGNode[i]));
    return pipelineNetwork;
  }

  public static PipelineNetwork sequence(PipelineNetwork... networks) {
    com.simiacryptus.ref.wrappers.RefArrays.stream(networks).forEach(ReferenceCountingBase::assertAlive);
    if (1 == networks.length)
      return networks[0];
    if (1 != networks[0].inputHandles.size())
      throw new IllegalArgumentException();
    if (1 != com.simiacryptus.ref.wrappers.RefArrays.stream(networks).mapToInt(x -> x.inputHandles.size()).distinct()
        .count())
      throw new IllegalArgumentException();
    PipelineNetwork pipelineNetwork = new PipelineNetwork(networks[0].inputHandles.size());
    for (PipelineNetwork network : networks) {
      transferNode(pipelineNetwork, network.getHead());
    }
    return pipelineNetwork;
  }

  public static @SuppressWarnings("unused")
  PipelineNetwork[] addRefs(PipelineNetwork[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(PipelineNetwork::addRef)
        .toArray((x) -> new PipelineNetwork[x]);
  }

  public static @SuppressWarnings("unused")
  PipelineNetwork[][] addRefs(PipelineNetwork[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(PipelineNetwork::addRefs)
        .toArray((x) -> new PipelineNetwork[x][]);
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
    if (null == nextHead)
      return null;
    return add(nextHead, getHead());
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

  public void addAll(InnerNode node, @Nonnull final Layer... layers) {
    for (final Layer l : layers) {
      node = add(l, node);
    }
  }

  public void addAll(final Layer... layers) {
    addAll((InnerNode) getHead(), layers);
  }

  @Nullable
  public DAGNode constValue(final Tensor tensor) {
    return add(new ValueLayer(tensor), new DAGNode[]{});
  }

  @Nullable
  public DAGNode constValueWrap(final Tensor tensor) {
    return constValue(tensor);
  }

  @Override
  public JsonObject getJson(com.simiacryptus.ref.wrappers.RefMap<CharSequence, byte[]> resources,
                            DataSerializer dataSerializer) {
    assertConsistent();
    final JsonObject json = super.getJson(resources, dataSerializer);
    json.addProperty("head", getHeadId().toString());
    return json;
  }

  @Override
  public PipelineNetwork andThenWrap(Layer append) {
    assert append.assertAlive();
    assert assertAlive();
    add(append);
    return this;
  }

  public PipelineNetwork copyPipeline() {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1, getId(), getName());
    if (!this.internalNodes.isEmpty())
      pipelineNetwork.setHead(transferNode(pipelineNetwork, getHead()));
    return pipelineNetwork;
  }

  public void _free() {
    super._free();
    if (null != head) {
      head = null;
    }
  }

  public @Override
  @SuppressWarnings("unused")
  PipelineNetwork addRef() {
    return (PipelineNetwork) super.addRef();
  }

}
