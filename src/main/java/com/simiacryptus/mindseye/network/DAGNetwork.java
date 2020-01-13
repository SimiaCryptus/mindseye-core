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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.MonitoredItem;
import com.simiacryptus.util.MonitoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
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
    for (@Nonnull
    final JsonElement item : json.getAsJsonArray("inputs")) {
      @Nonnull
      final UUID key = UUID.fromString(item.getAsString());
      inputHandles.add(key);
      InputNode replaced = inputNodes.put(key, new InputNode(this.addRef(), key));
      if (null != replaced)
        replaced.freeRef();
    }
    final JsonObject jsonNodes = json.getAsJsonObject("nodes");
    final JsonObject jsonLayers = json.getAsJsonObject("layers");
    final JsonObject jsonLinks = json.getAsJsonObject("links");
    final JsonObject jsonLabels = json.getAsJsonObject("labels");
    @Nonnull
    final RefMap<UUID, Layer> source_layersByNodeId = new RefHashMap<>();
    @Nonnull
    final RefMap<UUID, Layer> source_layersByLayerId = new RefHashMap<>();
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonLayers.entrySet()) {
      @Nonnull
      Layer value = Layer.fromJson(e.getValue().getAsJsonObject(), rs);
      RefUtil.freeRef(source_layersByLayerId.put(UUID.fromString(e.getKey()), value == null ? null : value));
    }
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonNodes.entrySet()) {
      @Nonnull
      final UUID nodeId = UUID.fromString(e.getKey());
      @Nonnull
      final UUID layerId = UUID.fromString(e.getValue().getAsString());
      final Layer layer = source_layersByLayerId.get(layerId);
      assert null != layer;
      RefUtil.freeRef(source_layersByNodeId.put(nodeId, layer == null ? null : layer.addRef()));
      if (null != layer)
        layer.freeRef();
    }
    source_layersByLayerId.freeRef();
    @Nonnull
    final RefLinkedHashMap<CharSequence, UUID> labels = new RefLinkedHashMap<>();
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonLabels.entrySet()) {
      labels.put(e.getKey(), UUID.fromString(e.getValue().getAsString()));
    }
    @Nonnull
    final RefMap<UUID, RefList<UUID>> deserializedLinks = new RefHashMap<>();
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonLinks.entrySet()) {
      @Nonnull
      final RefArrayList<UUID> linkList = new RefArrayList<>();
      for (@Nonnull
      final JsonElement linkItem : e.getValue().getAsJsonArray()) {
        linkList.add(UUID.fromString(linkItem.getAsString()));
      }
      RefUtil.freeRef(deserializedLinks.put(UUID.fromString(e.getKey()), linkList == null ? null : linkList));
    }
    RefHashSet<UUID> values = labels.values();
    for (final UUID key : values) {
      initLinks(deserializedLinks == null ? null : deserializedLinks.addRef(),
          source_layersByNodeId == null ? null : source_layersByNodeId.addRef(), key);
    }
    values.freeRef();
    RefSet<UUID> keySet = source_layersByNodeId.keySet();
    for (final UUID key : keySet) {
      initLinks(deserializedLinks == null ? null : deserializedLinks.addRef(),
          source_layersByNodeId == null ? null : source_layersByNodeId.addRef(), key);
    }
    keySet.freeRef();
    @Nonnull
    final UUID head = UUID.fromString(json.getAsJsonPrimitive("head").getAsString());
    initLinks(deserializedLinks == null ? null : deserializedLinks,
        source_layersByNodeId == null ? null : source_layersByNodeId, head);
    this.labels.putAll(labels);
    assertConsistent();
  }

  @Override
  public RefList<Layer> getChildren() {
    RefMap<UUID, Layer> temp_38_0016 = getLayersById();
    RefCollection<Layer> values = temp_38_0016.values();
    if (null != temp_38_0016)
      temp_38_0016.freeRef();
    RefList<Layer> temp_38_0015 = values.stream().flatMap(l -> {
      RefList<Layer> children = l.getChildren();
      RefStream<Layer> temp_38_0001 = children.stream();
      if (null != l)
        l.freeRef();
      children.freeRef();
      return temp_38_0001;
    }).distinct().sorted(RefComparator.comparing(l -> {
      String temp_38_0002 = l.getId().toString();
      if (null != l)
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
    UUID temp_38_0003 = head.getId();
    if (null != head)
      head.freeRef();
    return temp_38_0003;
  }

  @Nonnull
  public Layer getLayer() {
    return this.addRef();
  }

  public RefMap<UUID, Layer> getLayersById() {
    RefLinkedHashMap<UUID, Layer> map = new RefLinkedHashMap<>();
    visitLayers(RefUtil.wrapInterface(layer -> {
      UUID id = layer.getId();
      Layer previous = map.put(id, layer == null ? null : layer.addRef());
      if (null != previous && previous != layer) {
        if (null != layer)
          layer.freeRef();
        RuntimeException temp_38_0005 = new RuntimeException(
            RefString.format("Duplicated key found: %s (%s)", previous, id));
        if (null != previous)
          previous.freeRef();
        throw temp_38_0005;
      }
      if (null != previous)
        previous.freeRef();
      if (null != layer)
        layer.freeRef();
    }, RefUtil.addRef(map)));
    RefMap<UUID, Layer> temp_38_0004 = RefCollections.unmodifiableMap(RefUtil.addRef(map));
    if (null != map)
      map.freeRef();
    return temp_38_0004;
  }

  public RefList<DAGNode> getNodes() {
    RefHashSet<DAGNode> temp_38_0019 = this.internalNodes.values();
    RefList<DAGNode> temp_38_0018 = RefStream.concat(temp_38_0019.stream(), inputHandles.stream().map(inputNodes::get))
        .collect(RefCollectors.toList());
    if (null != temp_38_0019)
      temp_38_0019.freeRef();
    return temp_38_0018;
  }

  @Nonnull
  @Override
  public DAGNetwork setFrozen(final boolean frozen) {
    super.setFrozen(frozen);
    visitLayers(layer -> {
      RefUtil.freeRef(layer.setFrozen(frozen));
      if (null != layer)
        layer.freeRef();
    });
    return this.addRef();
  }

  public static @SuppressWarnings("unused") DAGNetwork[] addRefs(DAGNetwork[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DAGNetwork::addRef).toArray((x) -> new DAGNetwork[x]);
  }

  public static @SuppressWarnings("unused") DAGNetwork[][] addRefs(DAGNetwork[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DAGNetwork::addRefs).toArray((x) -> new DAGNetwork[x][]);
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

  @Nullable
  public InnerNode add(@Nonnull final Layer nextHead, final DAGNode... head) {
    InnerNode temp_38_0014 = add(null, nextHead == null ? null : nextHead, DAGNode.addRefs(head));
    if (null != head)
      ReferenceCounting.freeRefs(head);
    return temp_38_0014;
  }

  public InnerNode add(@Nullable final CharSequence label, @Nonnull final Layer layer, final DAGNode... head) {
    if (null == layer && head.length > 0) {
      layer.freeRef();
      if (null != head)
        ReferenceCounting.freeRefs(head);
      throw new IllegalArgumentException();
    }
    if (null == layer) {
      layer.freeRef();
      if (null != head)
        ReferenceCounting.freeRefs(head);
      return null;
    }
    assert RefArrays.stream(DAGNode.addRefs(head)).allMatch(x -> {
      boolean temp_38_0006 = x == null || internalNodes.containsKey(x.getId()) || inputNodes.containsKey(x.getId());
      if (null != x)
        x.freeRef();
      return temp_38_0006;
    });
    assert layer.assertAlive();
    if (null == layer) {
      layer.freeRef();
      if (null != head)
        ReferenceCounting.freeRefs(head);
      throw new IllegalArgumentException();
    }
    assert layer.assertAlive();
    assertAlive();
    assertConsistent();
    assert null != inputHandles;
    @Nonnull
    final InnerNode node = new InnerNode(this.addRef(), layer == null ? null : layer, DAGNode.addRefs(head));
    if (null != head)
      ReferenceCounting.freeRefs(head);
    DAGNode replaced = internalNodes.put(node.getId(), node == null ? null : node.addRef());
    if (null != replaced)
      replaced.freeRef();
    if (null != label)
      labels.put(label, node.getId());
    assertConsistent();
    return node;
  }

  @Nonnull
  public void addInput() {
    @Nonnull
    final UUID key = UUID.randomUUID();
    inputHandles.add(key);
    InputNode replaced = inputNodes.put(key, new InputNode(this.addRef(), key));
    if (null != replaced) {
      if (null != replaced)
        replaced.freeRef();
      throw new RuntimeException("UUID Conflict: " + key);
    }
    if (null != replaced)
      replaced.freeRef();
  }

  public void attach(@Nonnull final MonitoredObject obj) {
    visitLayers(RefUtil.wrapInterface(layer -> {
      if (layer instanceof MonitoredItem) {
        RefUtil.freeRef(obj.addObj(layer.getName(), (MonitoredItem) layer));
      }
      if (null != layer)
        layer.freeRef();
    }, obj == null ? null : obj));
  }

  @Nonnull
  public GraphEvaluationContext buildExeCtx(@Nonnull final Result... inputs) {
    assert inputs.length == inputHandles.size() : inputs.length + " != " + inputHandles.size();
    @Nonnull
    final GraphEvaluationContext context = new GraphEvaluationContext();
    for (int i = 0; i < inputs.length; i++) {
      UUID key = inputHandles.get(i);
      Result input = inputs[i].addRef();
      if (!context.calculated.containsKey(key)) {
        RefUtil.freeRef(input.getData());
        context.calculated.put(key,
            new Singleton<CountingResult>().set(new CountingResult(input == null ? null : input.addRef())));
      }
      if (null != input)
        input.freeRef();
    }
    ReferenceCounting.freeRefs(inputs);
    RefList<DAGNode> temp_38_0020 = getNodes();
    context.expectedCounts.putAll(temp_38_0020.stream().flatMap(t -> {
      RefStream<UUID> temp_38_0007 = RefArrays.stream(t.getInputs()).map(n -> {
        UUID temp_38_0008 = n.getId();
        if (null != n)
          n.freeRef();
        return temp_38_0008;
      });
      if (null != t)
        t.freeRef();
      return temp_38_0007;
    }).filter(x -> !inputHandles.contains(x)).collect(RefCollectors.groupingBy(x -> x, RefCollectors.counting())));
    if (null != temp_38_0020)
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
  public Result eval(final Result... input) {
    assertAlive();
    @Nonnull
    GraphEvaluationContext buildExeCtx = buildExeCtx(Result.addRefs(input));
    if (null != input)
      ReferenceCounting.freeRefs(input);
    DAGNode head = getHead();
    Result temp_38_0009 = head.get(buildExeCtx == null ? null : buildExeCtx);
    if (null != head)
      head.freeRef();
    return temp_38_0009;
  }

  public DAGNode getNodeById(final UUID k) {
    DAGNode dagNode = internalNodes.get(k);
    DAGNode temp_38_0010 = null == dagNode ? null : dagNode;
    if (null != dagNode)
      dagNode.freeRef();
    return temp_38_0010;
  }

  public final DAGNode getInput(final int index) {
    assertAlive();
    UUID key = inputHandles.get(index);
    final DAGNode input = inputNodes.get(key);
    if (null == input) {
      if (null != input)
        input.freeRef();
      throw new IllegalStateException(RefString.format("No Input: %d: %s", index, key));
    }
    return input;
  }

  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    assertAlive();
    @Nonnull
    final JsonObject json = super.getJsonStub();
    @Nonnull
    final JsonArray inputs = new JsonArray();
    json.add("inputs", inputs);
    inputHandles.forEach(uuid -> inputs.add(new JsonPrimitive(uuid.toString())));
    @Nonnull
    final JsonObject layerMap = new JsonObject();
    @Nonnull
    final JsonObject nodeMap = new JsonObject();
    @Nonnull
    final JsonObject links = new JsonObject();
    RefHashSet<DAGNode> temp_38_0021 = this.internalNodes.values();
    temp_38_0021.forEach(node -> {
      @Nonnull
      final JsonArray linkArray = new JsonArray();
      RefArrays.stream(node.getInputs()).forEach((@Nonnull final DAGNode input) -> {
        linkArray.add(new JsonPrimitive(input.getId().toString()));
        input.freeRef();
      });
      @Nullable
      final Layer layer = node.getLayer();
      @Nonnull
      final String nodeId = node.getId().toString();
      if (null != node)
        node.freeRef();
      final String layerId = layer.getId().toString();
      nodeMap.addProperty(nodeId, layerId);
      layerMap.add(layerId, layer.getJson(resources, dataSerializer));
      if (null != layer)
        layer.freeRef();
      links.add(nodeId, linkArray);
    });
    if (null != temp_38_0021)
      temp_38_0021.freeRef();
    json.add("nodes", nodeMap);
    json.add("layers", layerMap);
    json.add("links", links);
    @Nonnull
    final JsonObject labels = new JsonObject();
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
      if (null != x)
        x.freeRef();
      return temp_38_0011;
    }).flatMap(l -> {
      RefList<double[]> state = l.state();
      RefStream<double[]> temp_38_0012 = state.stream();
      state.freeRef();
      if (null != l) l.freeRef();
      return temp_38_0012;
    }).distinct().collect(RefCollectors.toList());
    if (null != temp_38_0023)
      temp_38_0023.freeRef();
    return temp_38_0022;
  }

  public void visitLayers(@Nonnull final RefConsumer<Layer> visitor) {
    visitNodes(node -> {
      Layer layer = node.getLayer();
      if (null != node)
        node.freeRef();
      Layer unwrapped = layer == null ? null : layer.addRef();
      while (unwrapped instanceof WrapperLayer) {
        unwrapped = ((WrapperLayer) unwrapped).getInner();
      }
      if (unwrapped instanceof DAGNetwork) {
        ((DAGNetwork) unwrapped).visitLayers(visitor);
      }
      if (null != unwrapped)
        unwrapped.freeRef();
      while (layer instanceof WrapperLayer) {
        visitor.accept(layer.addRef());
        Layer inner = ((WrapperLayer) layer).getInner();
        layer.freeRef();
        layer = inner;
      }
      visitor.accept(layer);
    });
  }

  public void visitNodes(@Nonnull final RefConsumer<DAGNode> visitor) {
    visitNodes(true, visitor);
  }

  public void visitNodes(boolean recurse, @Nonnull final RefConsumer<DAGNode> visitor) {
    assertAlive();
    RefHashSet<DAGNode> temp_38_0025 = this.internalNodes.values();
    temp_38_0025.forEach(node -> {
      node.assertAlive();
      Layer layer = node.getLayer();
      layer.assertAlive();
      while (layer instanceof WrapperLayer) {
        layer = ((WrapperLayer) layer).getInner();
      }
      if (recurse && layer instanceof DAGNetwork) {
        ((DAGNetwork) layer).visitNodes(visitor);
      }
      if (null != layer)
        layer.freeRef();
      visitor.accept(node == null ? null : node.addRef());
      if (null != node)
        node.freeRef();
    });
    if (null != temp_38_0025)
      temp_38_0025.freeRef();
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
    this.inputNodes.clear();
  }

  public @Override @SuppressWarnings("unused") DAGNetwork addRef() {
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

  private DAGNode[] getDependencies(@Nonnull final RefMap<UUID, RefList<UUID>> deserializedLinks, final UUID e) {
    final RefList<UUID> links = deserializedLinks.get(e);
    deserializedLinks.freeRef();
    if (null == links) {
      if (null != links)
        links.freeRef();
      return new DAGNode[] {};
    }
    DAGNode[] temp_38_0013 = links.stream().map(id -> getNode(id)).toArray(i -> new DAGNode[i]);
    if (null != links)
      links.freeRef();
    return temp_38_0013;
  }

  private DAGNode getNode(final UUID id) {
    DAGNode returnValue = getNodeById(id);
    if (null == returnValue) {
      returnValue = inputNodes.get(id);
    }
    return returnValue;
  }

  private synchronized void initLinks(@Nonnull final RefMap<UUID, RefList<UUID>> nodeLinks,
      @Nonnull final RefMap<UUID, Layer> layersByNodeId, final UUID newNodeId) {
    RefMap<UUID, Layer> layersById = getLayersById();
    if (layersById.containsKey(newNodeId)) {
      if (null != layersById)
        layersById.freeRef();
      nodeLinks.freeRef();
      layersByNodeId.freeRef();
      return;
    }
    if (null != layersById)
      layersById.freeRef();
    if (inputNodes.containsKey(newNodeId)) {
      nodeLinks.freeRef();
      layersByNodeId.freeRef();
      return;
    }
    final Layer layer = layersByNodeId.get(newNodeId);
    if (layer == null) {
      if (null != layer)
        layer.freeRef();
      nodeLinks.freeRef();
      layersByNodeId.freeRef();
      throw new IllegalArgumentException(RefString.format("%s is linked to but not defined", newNodeId));
    }
    final RefList<UUID> links = nodeLinks.get(newNodeId);
    if (null != links) {
      for (final UUID link : links) {
        initLinks(nodeLinks == null ? null : nodeLinks.addRef(),
            layersByNodeId == null ? null : layersByNodeId.addRef(), link);
      }
    }
    layersByNodeId.freeRef();
    if (null != links)
      links.freeRef();
    assertConsistent();
    @Nonnull
    final InnerNode node = new InnerNode(this.addRef(), layer == null ? null : layer.addRef(), newNodeId,
        getDependencies(nodeLinks == null ? null : nodeLinks, newNodeId));
    if (null != layer)
      layer.freeRef();
    DAGNode replaced = internalNodes.put(node.getId(), node == null ? null : node);
    if (null != replaced)
      replaced.freeRef();
    assertConsistent();
  }
}
