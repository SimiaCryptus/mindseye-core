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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("serial")
public @RefAware
class PipelineNetwork extends DAGNetwork {
  @Nullable
  private DAGNode head;

  public PipelineNetwork() {
    this(1);
  }

  public PipelineNetwork(final int inputs, @Nonnull final Layer... layers) {
    super(inputs);
    for (final Layer layer : layers) {
      RefUtil.freeRef(add(layer == null ? null : layer.addRef()));
    }
    ReferenceCounting.freeRefs(layers);
  }

  protected PipelineNetwork(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
    @Nonnull final UUID headId = UUID.fromString(json.get("head").getAsString());
    if (!inputHandles.contains(headId)) {
      assert null != headId;
      DAGNode node = getNodeById(headId);
      setHead(node == null ? null : node.addRef());
      if (null != node)
        node.freeRef();
    }
  }

  public PipelineNetwork(final Layer... layers) {
    this();
    addAll(Layer.addRefs(layers));
    if (null != layers)
      ReferenceCounting.freeRefs(layers);
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
      return head == null ? null : head.addRef();
    }
  }

  @Nonnull
  public void setHead(final DAGNode obj) {
    if (obj != head) {
      {
        DAGNode temp_06_0001 = obj == null ? null : obj.addRef();
        if (null != head)
          head.freeRef();
        head = temp_06_0001 == null ? null : temp_06_0001.addRef();
        if (null != temp_06_0001)
          temp_06_0001.freeRef();
      }
    }
    if (null != obj)
      obj.freeRef();
  }

  @SuppressWarnings("unused")
  public static PipelineNetwork fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new PipelineNetwork(json, rs);
  }

  public static PipelineNetwork build(final int inputs, final Layer... layers) {
    assert RefArrays.stream(Layer.addRefs(layers))
        .allMatch(ReferenceCounting::assertAlive);
    PipelineNetwork pipelineNetwork = new PipelineNetwork(inputs);
    for (final Layer layer : layers) {
      RefUtil.freeRef(pipelineNetwork.add(layer == null ? null : layer.addRef()));
    }
    if (null != layers)
      ReferenceCounting.freeRefs(layers);
    return pipelineNetwork;
  }

  public static InnerNode transferNode(PipelineNetwork pipelineNetwork, DAGNode node) {
    {
      final DAGNode[] dagNodes = RefArrays.stream(node.getInputs()).map(RefUtil.wrapInterface(
          (Function<DAGNode, ? extends DAGNode>) (
              DAGNode input) -> {
            final PipelineNetwork inputNetwork = (PipelineNetwork) input.getNetwork();
            final UUID inputId = input.getId();
            if (inputNetwork.inputNodes.containsKey(inputId)) {
              DAGNode temp_06_0003 = pipelineNetwork
                  .getInput(inputNetwork.inputHandles.indexOf(inputId));
              if (null != inputNetwork)
                inputNetwork.freeRef();
              if (null != input)
                input.freeRef();
              return temp_06_0003;
            } else {
              Layer inputLayer = input.getLayer();
              if (inputLayer == null) {
                if (null != inputNetwork)
                  inputNetwork.freeRef();
                if (null != inputLayer)
                  inputLayer.freeRef();
                IllegalArgumentException temp_06_0009 = new IllegalArgumentException(
                    input.getClass().toString());
                if (null != input)
                  input.freeRef();
                throw temp_06_0009;
              }
              RefList<DAGNode> temp_06_0015 = pipelineNetwork
                  .getNodes();
              Optional<DAGNode> temp_06_0016 = temp_06_0015.stream()
                  .filter(RefUtil.wrapInterface(
                      (Predicate<? super DAGNode>) dagNode -> {
                        Layer layer = dagNode.getLayer();
                        if (null != dagNode)
                          dagNode.freeRef();
                        if (null == layer) {
                          if (null != layer)
                            layer.freeRef();
                          return false;
                        }
                        boolean temp_06_0005 = layer.getId().equals(inputLayer.getId());
                        if (null != layer)
                          layer.freeRef();
                        return temp_06_0005;
                      }, inputLayer == null ? null : inputLayer.addRef()))
                  .findFirst();
              DAGNode temp_06_0004 = temp_06_0016
                  .orElseGet(RefUtil.wrapInterface(
                      (Supplier<? extends DAGNode>) () -> {
                        RefSet<UUID> temp_06_0017 = inputNetwork.inputNodes
                            .keySet();
                        RefList<UUID> temp_06_0018 = temp_06_0017.stream()
                            .collect(RefCollectors.toList());
                        int inputNumber = temp_06_0018.indexOf(inputId);
                        if (null != temp_06_0018)
                          temp_06_0018.freeRef();
                        if (null != temp_06_0017)
                          temp_06_0017.freeRef();
                        if (-1 == inputNumber) {
                          return transferNode(pipelineNetwork == null ? null : pipelineNetwork.addRef(),
                              input == null ? null : input.addRef());
                        } else {
                          return pipelineNetwork.getInput(inputNumber);
                        }
                      }, input == null ? null : input.addRef(),
                      pipelineNetwork == null ? null : pipelineNetwork.addRef(),
                      inputNetwork == null ? null : inputNetwork.addRef()));
              if (null != temp_06_0016)
                RefUtil.freeRef(temp_06_0016);
              if (null != temp_06_0015)
                temp_06_0015.freeRef();
              if (null != inputLayer)
                inputLayer.freeRef();
              if (null != inputNetwork)
                inputNetwork.freeRef();
              if (null != input)
                input.freeRef();
              return temp_06_0004;
            }
          }, pipelineNetwork == null ? null : pipelineNetwork.addRef())).toArray(i -> new DAGNode[i]);
      InnerNode temp_06_0006 = pipelineNetwork.add(node.getLayer(),
          DAGNode.addRefs(dagNodes));
      if (null != dagNodes)
        ReferenceCounting.freeRefs(dagNodes);
      if (null != pipelineNetwork)
        pipelineNetwork.freeRef();
      if (null != node)
        node.freeRef();
      return temp_06_0006;
    }
  }

  public static PipelineNetwork combine(Layer combiner, PipelineNetwork... networks) {
    RefArrays.stream(PipelineNetwork.addRefs(networks))
        .forEach(ReferenceCountingBase::assertAlive);
    if (1 == networks.length) {
      if (null != combiner)
        combiner.freeRef();
      PipelineNetwork temp_06_0010 = networks[0];
      if (null != networks)
        ReferenceCounting.freeRefs(networks);
      return temp_06_0010;
    }
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1);
    RefUtil.freeRef(pipelineNetwork.add(combiner == null ? null : combiner.addRef(), RefArrays
        .stream(PipelineNetwork.addRefs(networks))
        .map(RefUtil.wrapInterface(
            (Function<? super PipelineNetwork, ? extends InnerNode>) network -> {
              InnerNode temp_06_0007 = transferNode(
                  pipelineNetwork == null ? null : pipelineNetwork.addRef(), network.getHead());
              if (null != network)
                network.freeRef();
              return temp_06_0007;
            }, pipelineNetwork == null ? null : pipelineNetwork.addRef()))
        .toArray(i -> new DAGNode[i])));
    if (null != networks)
      ReferenceCounting.freeRefs(networks);
    if (null != combiner)
      combiner.freeRef();
    return pipelineNetwork;
  }

  public static PipelineNetwork sequence(PipelineNetwork... networks) {
    RefArrays.stream(PipelineNetwork.addRefs(networks))
        .forEach(ReferenceCountingBase::assertAlive);
    if (1 == networks.length) {
      PipelineNetwork temp_06_0011 = networks[0];
      if (null != networks)
        ReferenceCounting.freeRefs(networks);
      return temp_06_0011;
    }
    if (1 != networks[0].inputHandles.size()) {
      if (null != networks)
        ReferenceCounting.freeRefs(networks);
      throw new IllegalArgumentException();
    }
    if (1 != RefArrays.stream(PipelineNetwork.addRefs(networks)).mapToInt(x -> {
      int temp_06_0008 = x.inputHandles.size();
      if (null != x)
        x.freeRef();
      return temp_06_0008;
    }).distinct().count()) {
      if (null != networks)
        ReferenceCounting.freeRefs(networks);
      throw new IllegalArgumentException();
    }
    PipelineNetwork pipelineNetwork = new PipelineNetwork(networks[0].inputHandles.size());
    for (PipelineNetwork network : networks) {
      RefUtil
          .freeRef(transferNode(pipelineNetwork == null ? null : pipelineNetwork.addRef(), network.getHead()));
    }
    if (null != networks)
      ReferenceCounting.freeRefs(networks);
    return pipelineNetwork;
  }

  public static @SuppressWarnings("unused")
  PipelineNetwork[] addRefs(PipelineNetwork[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(PipelineNetwork::addRef)
        .toArray((x) -> new PipelineNetwork[x]);
  }

  public static @SuppressWarnings("unused")
  PipelineNetwork[][] addRefs(PipelineNetwork[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(PipelineNetwork::addRefs)
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
    if (null == nextHead) {
      if (null != nextHead)
        nextHead.freeRef();
      return null;
    }
    InnerNode temp_06_0012 = add(nextHead == null ? null : nextHead.addRef(),
        getHead());
    if (null != nextHead)
      nextHead.freeRef();
    return temp_06_0012;
  }

  @Nullable
  @Override
  public InnerNode add(@Nullable final Layer nextHead, @Nonnull final DAGNode... head) {
    @Nullable final InnerNode node = super.add(nextHead, head).addRef();
    ReferenceCounting.freeRefs(head);
    if (null != nextHead)
      nextHead.freeRef();
    setHead(node == null ? null : node.addRef());
    return node;
  }

  @SafeVarargs
  @Override
  public final InnerNode add(final CharSequence label, @Nullable final Layer layer, final DAGNode... head) {
    final InnerNode node = super.add(label, layer, head).addRef();
    if (null != head)
      ReferenceCounting.freeRefs(head);
    if (null != layer)
      layer.freeRef();
    setHead(node == null ? null : node.addRef());
    return node;
  }

  public void addAll(InnerNode node, @Nonnull final Layer... layers) {
    for (final Layer l : layers) {
      node = add(l == null ? null : l.addRef(), node == null ? null : node.addRef());
    }
    ReferenceCounting.freeRefs(layers);
    if (null != node)
      node.freeRef();
  }

  public void addAll(final Layer... layers) {
    addAll((InnerNode) getHead(), Layer.addRefs(layers));
    if (null != layers)
      ReferenceCounting.freeRefs(layers);
  }

  @Nullable
  public DAGNode constValue(final Tensor tensor) {
    InnerNode temp_06_0013 = add(
        new ValueLayer(tensor == null ? null : tensor.addRef()), new DAGNode[]{});
    if (null != tensor)
      tensor.freeRef();
    return temp_06_0013;
  }

  @Nullable
  public DAGNode constValueWrap(final Tensor tensor) {
    DAGNode temp_06_0014 = constValue(tensor == null ? null : tensor.addRef());
    if (null != tensor)
      tensor.freeRef();
    return temp_06_0014;
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
    RefUtil.freeRef(add(append == null ? null : append.addRef()));
    if (null != append)
      append.freeRef();
    return this.addRef();
  }

  public PipelineNetwork copyPipeline() {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1, getId(), getName());
    if (!this.internalNodes.isEmpty())
      pipelineNetwork.setHead(transferNode(pipelineNetwork == null ? null : pipelineNetwork.addRef(), getHead()));
    return pipelineNetwork;
  }

  public void _free() {
    if (null != head)
      head.freeRef();
    head = null;
    super._free();
    if (null != head) {
      {
        DAGNode temp_06_0002 = null;
        if (null != head)
          head.freeRef();
        head = temp_06_0002 == null ? null : temp_06_0002.addRef();
        if (null != temp_06_0002)
          temp_06_0002.freeRef();
      }
    }
  }

  public @Override
  @SuppressWarnings("unused")
  PipelineNetwork addRef() {
    return (PipelineNetwork) super.addRef();
  }

}
