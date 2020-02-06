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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefSet;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("serial")
public class PipelineNetwork extends DAGNetwork {
  @Nullable
  private DAGNode head;

  public PipelineNetwork() {
    this(1);
  }

  public PipelineNetwork(final int inputs, @Nonnull final Layer... layers) {
    super(inputs);
    RefArrays.stream(layers).forEach(layer -> RefUtil.freeRef(add(layer)));
  }

  protected PipelineNetwork(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
    @Nonnull final UUID headId = UUID.fromString(json.get("head").getAsString());
    if (!inputHandles.contains(headId)) {
      DAGNode node = getNodeById(headId);
      setHead(node == null ? null : node.addRef());
      if (null != node)
        node.freeRef();
    }
  }

  public PipelineNetwork(@Nullable final Layer... layers) {
    this();
    addAll(RefUtil.addRefs(layers));
    if (null != layers)
      RefUtil.freeRef(layers);
  }

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

  @Nonnull
  public void setHead(@Nullable final DAGNode obj) {
    if (obj != head) {
      DAGNode temp_06_0001 = obj == null ? null : obj.addRef();
      if (null != head)
        head.freeRef();
      head = temp_06_0001 == null ? null : temp_06_0001.addRef();
      if (null != temp_06_0001)
        temp_06_0001.freeRef();
    }
    if (null != obj)
      obj.freeRef();
  }

  @Nonnull
  @SuppressWarnings("unused")
  public static PipelineNetwork fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new PipelineNetwork(json, rs);
  }

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

  @NotNull
  public static InnerNode transferNode(@Nonnull PipelineNetwork destinationNetwork, @Nonnull DAGNode node, PipelineNetwork sourceNetwork) {
    try {
      final DAGNode[] dagNodes = RefArrays.stream(node.getInputs())
          .map(RefUtil.wrapInterface((Function<DAGNode, ? extends DAGNode>) (DAGNode input) -> {
            final UUID inputId = input.getId();
            assert sourceNetwork != null;
            if (sourceNetwork.inputNodes.containsKey(inputId)) {
              DAGNode temp_06_0003 = destinationNetwork.getInput(sourceNetwork.inputHandles.indexOf(inputId));
              input.freeRef();
              return temp_06_0003;
            } else {
              Layer inputLayer = input.getLayer();
              if (inputLayer == null) {
                IllegalArgumentException temp_06_0009 = new IllegalArgumentException(input.getClass().toString());
                input.freeRef();
                throw temp_06_0009;
              }
              RefList<DAGNode> temp_06_0015 = destinationNetwork.getNodes();
              Optional<DAGNode> temp_06_0016 = temp_06_0015.stream()
                  .filter(RefUtil.wrapInterface((Predicate<? super DAGNode>) dagNode -> {
                    Layer layer = dagNode.getLayer();
                    dagNode.freeRef();
                    if (null == layer) {
                      return false;
                    }
                    boolean temp_06_0005 = layer.getId().equals(inputLayer.getId());
                    layer.freeRef();
                    return temp_06_0005;
                  }, inputLayer)).findFirst();
              DAGNode temp_06_0004 = RefUtil.orElseGet(temp_06_0016, RefUtil.wrapInterface(() -> {
                    RefSet<UUID> temp_06_0017 = sourceNetwork.inputNodes.keySet();
                    RefList<UUID> temp_06_0018 = temp_06_0017.stream().collect(RefCollectors.toList());
                    int inputNumber = temp_06_0018.indexOf(inputId);
                    temp_06_0018.freeRef();
                    temp_06_0017.freeRef();
                    if (-1 == inputNumber) {
                      return transferNode(destinationNetwork.addRef(), input.addRef(), sourceNetwork.addRef());
                    } else {
                      return destinationNetwork.getInput(inputNumber);
                    }
                  }, input, destinationNetwork.addRef(), sourceNetwork.addRef()));
              temp_06_0015.freeRef();
              return temp_06_0004;
            }
          }, destinationNetwork.addRef())).toArray(DAGNode[]::new);
      InnerNode temp_06_0006 = destinationNetwork.add(node.getLayer(), RefUtil.addRefs(dagNodes));
      RefUtil.freeRef(dagNodes);
      destinationNetwork.freeRef();
      node.freeRef();
      return temp_06_0006;
    } finally {
      sourceNetwork.freeRef();
    }
  }

  public static PipelineNetwork combine(@Nullable Layer combiner, @Nonnull PipelineNetwork... networks) {
    assert RefArrays.stream(RefUtil.addRefs(networks)).allMatch(pipelineNetwork1 -> {
      boolean alive = pipelineNetwork1.assertAlive();
      pipelineNetwork1.freeRef();
      return alive;
    });
    if (1 == networks.length) {
      if (null != combiner)
        combiner.freeRef();
      PipelineNetwork temp_06_0010 = networks[0].addRef();
      RefUtil.freeRef(networks);
      return temp_06_0010;
    }
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1);
    RefUtil.freeRef(pipelineNetwork.add(combiner == null ? null : combiner.addRef(),
        RefArrays.stream(RefUtil.addRefs(networks))
            .map(RefUtil.wrapInterface((Function<? super PipelineNetwork, ? extends InnerNode>) network -> {
              InnerNode temp_06_0007 = transferNode(pipelineNetwork.addRef(),
                  network.getHead(), network.addRef());
              network.freeRef();
              return temp_06_0007;
            }, pipelineNetwork.addRef())).toArray(DAGNode[]::new)));
    RefUtil.freeRef(networks);
    if (null != combiner)
      combiner.freeRef();
    return pipelineNetwork;
  }

  public static PipelineNetwork sequence(@Nonnull PipelineNetwork... networks) {
    assert RefArrays.stream(RefUtil.addRefs(networks)).allMatch(pipelineNetwork1 -> {
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
    if (1 != RefArrays.stream(RefUtil.addRefs(networks)).mapToInt(x -> {
      int temp_06_0008 = x.inputHandles.size();
      x.freeRef();
      return temp_06_0008;
    }).distinct().count()) {
      RefUtil.freeRef(networks);
      throw new IllegalArgumentException();
    }
    PipelineNetwork pipelineNetwork = new PipelineNetwork(networks[0].inputHandles.size());
    for (PipelineNetwork network : networks) {
      RefUtil.freeRef(transferNode(pipelineNetwork.addRef(), network.getHead(), network.addRef()));
    }
    RefUtil.freeRef(networks);
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

  @Nonnull
  public InnerNode add(@Nullable final Layer nextHead) {
    assert nextHead != null;
    assert nextHead.assertAlive();
    InnerNode temp_06_0012 = add(nextHead.addRef(), getHead());
    nextHead.freeRef();
    return temp_06_0012;
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

  public void addAll(InnerNode node, @Nonnull final Layer... layers) {
    for (final Layer l : layers) {
      node = add(l == null ? null : l.addRef(), node);
    }
    RefUtil.freeRef(layers);
    if (null != node)
      node.freeRef();
  }

  public void addAll(@Nullable final Layer... layers) {
    addAll((InnerNode) getHead(), RefUtil.addRefs(layers));
    if (null != layers)
      RefUtil.freeRef(layers);
  }

  @Nullable
  public DAGNode constValue(@Nullable final Tensor tensor) {
    InnerNode temp_06_0013 = add(new ValueLayer(tensor == null ? null : tensor.addRef()), new DAGNode[]{});
    if (null != tensor)
      tensor.freeRef();
    return temp_06_0013;
  }

  @Nullable
  public DAGNode constValueWrap(@Nullable final Tensor tensor) {
    DAGNode temp_06_0014 = constValue(tensor == null ? null : tensor.addRef());
    if (null != tensor)
      tensor.freeRef();
    return temp_06_0014;
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

  @Nullable
  public PipelineNetwork copyPipeline() {
    PipelineNetwork pipelineNetwork = new PipelineNetwork(1, getId(), getName());
    if (!this.internalNodes.isEmpty())
      pipelineNetwork.setHead(transferNode(pipelineNetwork.addRef(), getHead(), this.addRef()));
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
