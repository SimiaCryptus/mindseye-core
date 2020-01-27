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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.layers.StochasticComponent;
import com.simiacryptus.mindseye.layers.WrapperLayer;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.MonitoredItem;
import com.simiacryptus.util.MonitoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

@SuppressWarnings("serial")
public abstract class DAGNetwork extends LayerBase {

  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DAGNetwork.class);
  public final RefList<UUID> inputHandles = new RefArrayList<>();
  public final RefLinkedHashMap<UUID, InputNode> inputNodes = new RefLinkedHashMap<>();
  protected final RefLinkedHashMap<CharSequence, UUID> labels = new RefLinkedHashMap<>();
  protected final RefLinkedHashMap<UUID, DAGNode> internalNodes = new RefLinkedHashMap<>();

  public DAGNetwork(final int inputs, UUID id, String name) {
    super(id, name);
    assert 0 < inputs;
    for (int i = 0; i < inputs; i++) {
      addInput();
    }
  }

  public DAGNetwork(final int inputs) {
    super();
    //assert 0 < inputs;
    for (int i = 0; i < inputs; i++) {
      addInput();
    }
  }

  protected DAGNetwork(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json);
    for (@Nonnull final JsonElement item : json.getAsJsonArray("inputs")) {
      @Nonnull final UUID key = UUID.fromString(item.getAsString());
      inputHandles.add(key);
      InputNode replaced = inputNodes.put(key, new InputNode(this.addRef(), key));
      if (null != replaced)
        replaced.freeRef();
    }
    final JsonObject jsonNodes = json.getAsJsonObject("nodes");
    final JsonObject jsonLayers = json.getAsJsonObject("layers");
    final JsonObject jsonLinks = json.getAsJsonObject("links");
    final JsonObject jsonLabels = json.getAsJsonObject("labels");
    @Nonnull final RefMap<UUID, Layer> source_layersByNodeId = new RefHashMap<>();
    @Nonnull final RefMap<UUID, Layer> source_layersByLayerId = new RefHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLayers.entrySet()) {
      @Nonnull
      Layer value = Layer.fromJson(e.getValue().getAsJsonObject(), rs);
      RefUtil.freeRef(source_layersByLayerId.put(UUID.fromString(e.getKey()), value));
    }
    for (@Nonnull final Entry<String, JsonElement> e : jsonNodes.entrySet()) {
      @Nonnull final UUID nodeId = UUID.fromString(e.getKey());
      @Nonnull final UUID layerId = UUID.fromString(e.getValue().getAsString());
      final Layer layer = source_layersByLayerId.get(layerId);
      assert null != layer;
      RefUtil.freeRef(source_layersByNodeId.put(nodeId, layer.addRef()));
      layer.freeRef();
    }
    source_layersByLayerId.freeRef();
    @Nonnull final RefLinkedHashMap<CharSequence, UUID> labels = new RefLinkedHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLabels.entrySet()) {
      labels.put(e.getKey(), UUID.fromString(e.getValue().getAsString()));
    }
    @Nonnull final RefMap<UUID, RefList<UUID>> deserializedLinks = new RefHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLinks.entrySet()) {
      @Nonnull final RefArrayList<UUID> linkList = new RefArrayList<>();
      for (@Nonnull final JsonElement linkItem : e.getValue().getAsJsonArray()) {
        linkList.add(UUID.fromString(linkItem.getAsString()));
      }
      RefUtil.freeRef(deserializedLinks.put(UUID.fromString(e.getKey()), linkList));
    }
    RefHashSet<UUID> values = labels.values();
    values.forEach(key -> initLinks(deserializedLinks.addRef(), source_layersByNodeId.addRef(), key));
    values.freeRef();
    RefSet<UUID> keySet = source_layersByNodeId.keySet();
    keySet.forEach(key -> initLinks(deserializedLinks.addRef(), source_layersByNodeId.addRef(), key));
    keySet.freeRef();
    @Nonnull final UUID head = UUID.fromString(json.getAsJsonPrimitive("head").getAsString());
    initLinks(deserializedLinks, source_layersByNodeId, head);
    this.labels.putAll(labels);
    assertConsistent();
  }

  @Override
  public RefList<Layer> getChildren() {
    assertAlive();
    RefMap<UUID, Layer> temp_38_0016 = getLayersById();
    RefCollection<Layer> values = temp_38_0016.values();
    temp_38_0016.freeRef();
    RefList<Layer> temp_38_0015 = values.stream().flatMap(l -> {
      RefList<Layer> children = l.getChildren();
      RefStream<Layer> temp_38_0001 = children.stream();
      l.freeRef();
      children.freeRef();
      return temp_38_0001;
    }).distinct().sorted(RefComparator.comparing(l -> {
      String temp_38_0002 = l.getId().toString();
      l.freeRef();
      return temp_38_0002;
    })).collect(RefCollectors.toList());
    values.freeRef();
    return temp_38_0015;
  }

  @Nullable
  public abstract DAGNode getHead();

  public UUID getHeadId() {
    DAGNode head = getHead();
    assert head != null;
    UUID temp_38_0003 = head.getId();
    head.freeRef();
    return temp_38_0003;
  }

  @Nonnull
  public RefMap<UUID, Layer> getLayersById() {
    RefLinkedHashMap<UUID, Layer> map = new RefLinkedHashMap<>();
    visitLayers(RefUtil.wrapInterface(layer -> {
      UUID id = layer.getId();
      Layer previous = map.put(id, layer.addRef());
      if (null != previous && previous != layer) {
        layer.freeRef();
        RuntimeException temp_38_0005 = new RuntimeException(
            RefString.format("Duplicated key found: %s (%s)", previous, id));
        throw temp_38_0005;
      }
      if (null != previous)
        previous.freeRef();
      layer.freeRef();
    }, RefUtil.addRef(map)));
    return RefCollections.unmodifiableMap(map);
  }

  public RefList<DAGNode> getNodes() {
    RefHashSet<DAGNode> temp_38_0019 = this.internalNodes.values();
    RefList<DAGNode> temp_38_0018 = RefStream.concat(temp_38_0019.stream(), inputHandles.stream().map(inputNodes::get))
        .collect(RefCollectors.toList());
    temp_38_0019.freeRef();
    return temp_38_0018;
  }

  @Nonnull
  @Override
  public void setFrozen(final boolean frozen) {
    super.setFrozen(frozen);
    visitLayers(layer -> {
      layer.setFrozen(frozen);
      layer.freeRef();
    });
  }


  public void shuffle(long seed) {
    visitLayers(layer -> {
      if (layer instanceof StochasticComponent)
        ((StochasticComponent) layer).shuffle(seed);
      if (null != layer)
        layer.freeRef();
    });
  }

  public void clearNoise() {
    visitLayers(layer -> {
      if (layer instanceof StochasticComponent)
        ((StochasticComponent) layer).clearNoise();
      if (null != layer)
        layer.freeRef();
    });
  }

  @Nonnull
  public InnerNode add(@Nonnull final Layer nextHead, @Nullable final DAGNode... head) {
    InnerNode temp_38_0014 = add(null, nextHead, RefUtil.addRefs(head));
    if (null != head)
      RefUtil.freeRefs(head);
    return temp_38_0014;
  }

  @Nonnull
  public InnerNode add(@Nullable final CharSequence label, @Nonnull final Layer layer, @Nonnull final DAGNode... head) {
    assert RefArrays.stream(RefUtil.addRefs(head)).allMatch(x -> {
      boolean temp_38_0006 = x == null || internalNodes.containsKey(x.getId()) || inputNodes.containsKey(x.getId());
      if (null != x)
        x.freeRef();
      return temp_38_0006;
    });
    assert layer.assertAlive();
    assert layer.assertAlive();
    assertAlive();
    assertConsistent();
    assert null != inputHandles;
    @Nonnull final InnerNode node = new InnerNode(this.addRef(), layer, RefUtil.addRefs(head));
    RefUtil.freeRefs(head);
    DAGNode replaced = internalNodes.put(node.getId(), node.addRef());
    if (null != replaced)
      replaced.freeRef();
    if (null != label)
      labels.put(label, node.getId());
    assertConsistent();
    return node;
  }

  @Nonnull
  public void addInput() {
    @Nonnull final UUID key = UUID.randomUUID();
    inputHandles.add(key);
    InputNode replaced = inputNodes.put(key, new InputNode(this.addRef(), key));
    try {
      if (null != replaced) {
        throw new RuntimeException("UUID Conflict: " + key);
      }
    } finally {
      if (null != replaced) replaced.freeRef();
    }
  }

  public void attach(@Nonnull final MonitoredObject obj) {
    visitLayers(RefUtil.wrapInterface(layer -> {
      if (layer instanceof MonitoredItem) {
        obj.addObj(layer.getName(), (MonitoredItem) layer);
      } else {
        if (null != layer) layer.freeRef();
      }
    }, obj));
  }

  @Nonnull
  public GraphEvaluationContext buildExeCtx(@Nonnull final Result... inputs) {
    assert inputs.length == inputHandles.size() : inputs.length + " != " + inputHandles.size();
    @Nonnull final GraphEvaluationContext context = new GraphEvaluationContext();
    for (int i = 0; i < inputs.length; i++) {
      UUID key = inputHandles.get(i);
      Result input = inputs[i].addRef();
      if (!context.calculated.containsKey(key)) {
        Singleton<CountingResult> countingResultSingleton = new Singleton<CountingResult>();
        countingResultSingleton.set(new CountingResult(input.addRef()));
        context.calculated.put(key, countingResultSingleton);
      }
      input.freeRef();
    }
    RefUtil.freeRefs(inputs);
    RefList<DAGNode> temp_38_0020 = getNodes();
    context.expectedCounts.putAll(temp_38_0020.stream().flatMap(t -> {
      RefStream<UUID> temp_38_0007 = RefArrays.stream(t.getInputs()).map(n -> {
        UUID temp_38_0008 = n.getId();
        n.freeRef();
        return temp_38_0008;
      });
      if (null != t)
        t.freeRef();
      return temp_38_0007;
    }).filter(x -> !inputHandles.contains(x)).collect(RefCollectors.groupingBy(x -> x, RefCollectors.counting())));
    temp_38_0020.freeRef();
    return context;
  }

  @Nonnull
  @Override
  public DAGNetwork copy(SerialPrecision precision) {
    return (DAGNetwork) super.copy(precision);
  }

  @Nullable
  @Override
  public Result eval(@Nullable final Result... input) {
    assertAlive();
    @Nonnull
    GraphEvaluationContext buildExeCtx = buildExeCtx(input);
    DAGNode head = getHead();
    assert head != null;
    Result temp_38_0009 = head.get(buildExeCtx);
    head.freeRef();
    return temp_38_0009;
  }

  @Nullable
  public DAGNode getNodeById(final UUID k) {
    return internalNodes.get(k);
  }

  @Nonnull
  public final DAGNode getInput(final int index) {
    assertAlive();
    UUID key = inputHandles.get(index);
    final DAGNode input = inputNodes.get(key);
    if (null == input) {
      throw new IllegalStateException(RefString.format("No Input: %d: %s", index, key));
    }
    return input;
  }

  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    assertAlive();
    @Nonnull final JsonObject json = super.getJsonStub();
    @Nonnull final JsonArray inputs = new JsonArray();
    json.add("inputs", inputs);
    inputHandles.forEach(uuid -> inputs.add(new JsonPrimitive(uuid.toString())));
    @Nonnull final JsonObject layerMap = new JsonObject();
    @Nonnull final JsonObject nodeMap = new JsonObject();
    @Nonnull final JsonObject links = new JsonObject();
    RefHashSet<DAGNode> temp_38_0021 = this.internalNodes.values();
    temp_38_0021.forEach(node -> {
      @Nonnull final JsonArray linkArray = new JsonArray();
      RefArrays.stream(node.getInputs()).forEach((@Nonnull final DAGNode input) -> {
        linkArray.add(new JsonPrimitive(input.getId().toString()));
        input.freeRef();
      });
      @Nullable final Layer layer = node.getLayer();
      @Nonnull final String nodeId = node.getId().toString();
      node.freeRef();
      assert layer != null;
      final String layerId = layer.getId().toString();
      nodeMap.addProperty(nodeId, layerId);
      layerMap.add(layerId, layer.getJson(resources, dataSerializer));
      layer.freeRef();
      links.add(nodeId, linkArray);
    });
    temp_38_0021.freeRef();
    json.add("nodes", nodeMap);
    json.add("layers", layerMap);
    json.add("links", links);
    @Nonnull final JsonObject labels = new JsonObject();
    this.labels.forEach((k, v) -> {
      labels.addProperty(k.toString(), v.toString());
    });
    json.add("labels", labels);
    json.addProperty("head", getHeadId().toString());
    return json;
  }

  public synchronized void reset() {
    this.internalNodes.clear();
    labels.clear();
  }

  @Override
  public RefList<double[]> state() {
    RefList<Layer> temp_38_0023 = getChildren();
    RefList<double[]> temp_38_0022 = temp_38_0023.stream().filter(x -> {
      boolean temp_38_0011 = !x.isFrozen();
      x.freeRef();
      return temp_38_0011;
    }).flatMap(l -> {
      RefList<double[]> state = l.state();
      assert state != null;
      RefStream<double[]> temp_38_0012 = state.stream();
      state.freeRef();
      l.freeRef();
      return temp_38_0012;
    }).distinct().collect(RefCollectors.toList());
    temp_38_0023.freeRef();
    return temp_38_0022;
  }

  public void visitLayers(@Nonnull @RefAware final RefConsumer<Layer> visitor) {
    assertAlive();
    visitNodes(RefUtil.wrapInterface(node -> {
      Layer layer = node.getLayer();
      node.freeRef();
      while (layer instanceof WrapperLayer) {
        Layer unwrapped = ((WrapperLayer) layer).getInner();
        try {
          if (unwrapped instanceof DAGNetwork) {
            ((DAGNetwork) unwrapped).visitLayers(RefUtil.addRef(visitor));
          }
        } finally {
          if (null != unwrapped)
            unwrapped.freeRef();
        }
      }
      while (layer instanceof WrapperLayer) {
        visitor.accept(layer.addRef());
        Layer inner = ((WrapperLayer) layer).getInner();
        assert null != inner;
        layer.freeRef();
        layer = inner;
      }
      visitor.accept(layer);
    }, visitor));
  }

  public void visitNodes(@Nonnull @RefAware final RefConsumer<DAGNode> visitor) {
    visitNodes(true, visitor);
  }

  public void visitNodes(boolean recurse, @Nonnull @RefAware final RefConsumer<DAGNode> visitor) {
    assertAlive();
    RefHashSet<DAGNode> temp_38_0025 = this.internalNodes.values();
    try {
      temp_38_0025.forEach(RefUtil.wrapInterface(node -> {
        node.assertAlive();
        Layer layer = node.getLayer();
        assert layer != null;
        layer.assertAlive();
        while (layer instanceof WrapperLayer) {
          Layer inner = ((WrapperLayer) layer).getInner();
          layer.freeRef();
          layer = inner;
        }
        if (recurse) {
          if (layer instanceof DAGNetwork) {
            ((DAGNetwork) layer).visitNodes(RefUtil.addRef(visitor));
          }
        }
        if (null != layer)
          layer.freeRef();
        visitor.accept(node);
      }, visitor));
    } finally {
      temp_38_0025.freeRef();
    }
  }

  public void _free() {
    if (null != internalNodes)
      internalNodes.freeRef();
    if (null != labels)
      labels.freeRef();
    if (null != inputNodes)
      inputNodes.freeRef();
    if (null != inputHandles)
      inputHandles.freeRef();
    super._free();
  }

  public @Override
  @SuppressWarnings("unused")
  DAGNetwork addRef() {
    return (DAGNetwork) super.addRef();
  }

  protected boolean assertConsistent() {
    assertAlive();
    assert null != inputHandles;
    RefHashSet<Entry<CharSequence, UUID>> entries = labels.entrySet();
    entries.forEach(e -> {
      assert internalNodes.containsKey(e.getValue());
    });
    entries.freeRef();
    return true;
  }

  @Nonnull
  private DAGNode[] getDependencies(@Nonnull final RefMap<UUID, RefList<UUID>> deserializedLinks, final UUID e) {
    final RefList<UUID> links = deserializedLinks.get(e);
    deserializedLinks.freeRef();
    if (null == links) {
      return new DAGNode[]{};
    }
    DAGNode[] temp_38_0013 = links.stream().map(id -> getNode(id)).toArray(i -> new DAGNode[i]);
    links.freeRef();
    return temp_38_0013;
  }

  @Nullable
  private DAGNode getNode(final UUID id) {
    DAGNode returnValue = getNodeById(id);
    if (null == returnValue) {
      RefUtil.freeRef(returnValue);
      returnValue = inputNodes.get(id);
    }
    return returnValue;
  }

  private synchronized void initLinks(@Nonnull final RefMap<UUID, RefList<UUID>> nodeLinks,
                                      @Nonnull final RefMap<UUID, Layer> layersByNodeId, final UUID newNodeId) {
    RefMap<UUID, Layer> layersById = getLayersById();
    if (layersById.containsKey(newNodeId)) {
      layersById.freeRef();
      nodeLinks.freeRef();
      layersByNodeId.freeRef();
      return;
    }
    layersById.freeRef();
    if (inputNodes.containsKey(newNodeId)) {
      nodeLinks.freeRef();
      layersByNodeId.freeRef();
      return;
    }
    final Layer layer = layersByNodeId.get(newNodeId);
    if (layer == null) {
      nodeLinks.freeRef();
      layersByNodeId.freeRef();
      throw new IllegalArgumentException(RefString.format("%s is linked to but not defined", newNodeId));
    }
    final RefList<UUID> links = nodeLinks.get(newNodeId);
    if (null != links) {
      links.forEach(link -> initLinks(nodeLinks.addRef(), layersByNodeId.addRef(), link));
      links.freeRef();
    }
    layersByNodeId.freeRef();
    assertConsistent();
    @Nonnull final InnerNode node = new InnerNode(this.addRef(), layer, newNodeId,
        getDependencies(nodeLinks, newNodeId));
    RefUtil.freeRef(internalNodes.put(node.getId(), node));
    assertConsistent();
  }
}
