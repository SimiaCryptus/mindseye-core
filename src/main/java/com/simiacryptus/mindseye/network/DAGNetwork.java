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
public abstract @RefAware
class DAGNetwork extends LayerBase {

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
      InputNode replaced = inputNodes.put(key, new InputNode(this, key));
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
      source_layersByLayerId.put(UUID.fromString(e.getKey()), value);
    }
    for (@Nonnull final Entry<String, JsonElement> e : jsonNodes.entrySet()) {
      @Nonnull final UUID nodeId = UUID.fromString(e.getKey());
      @Nonnull final UUID layerId = UUID.fromString(e.getValue().getAsString());
      final Layer layer = source_layersByLayerId.get(layerId);
      assert null != layer;
      source_layersByNodeId.put(nodeId, layer);
    }
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
      deserializedLinks.put(UUID.fromString(e.getKey()), linkList);
    }
    for (final UUID key : labels.values()) {
      initLinks(deserializedLinks, source_layersByNodeId, key);
    }
    for (final UUID key : source_layersByNodeId.keySet()) {
      initLinks(deserializedLinks, source_layersByNodeId, key);
    }
    @Nonnull final UUID head = UUID.fromString(json.getAsJsonPrimitive("head").getAsString());
    initLinks(deserializedLinks, source_layersByNodeId, head);
    this.labels.putAll(labels);
    assertConsistent();
  }

  @Override
  public RefList<Layer> getChildren() {
    return getLayersById().values().stream().flatMap(l -> l.getChildren().stream()).distinct()
        .sorted(RefComparator.comparing(l -> l.getId().toString()))
        .collect(RefCollectors.toList());
  }

  @Nullable
  public abstract DAGNode getHead();

  public UUID getHeadId() {
    DAGNode head = getHead();
    return head.getId();
  }

  @Nonnull
  public Layer getLayer() {
    return this;
  }

  public RefMap<UUID, Layer> getLayersById() {
    RefLinkedHashMap<UUID, Layer> map = new RefLinkedHashMap<>();
    visitLayers(layer -> {
      UUID id = layer.getId();
      Layer previous = map.put(id, layer);
      if (null != previous && previous != layer)
        throw new RuntimeException(String.format("Duplicated key found: %s (%s)", previous, id));
    });
    return RefCollections.unmodifiableMap(map);
  }

  public RefList<DAGNode> getNodes() {
    return RefStream
        .concat(this.internalNodes.values().stream(), inputHandles.stream().map(inputNodes::get))
        .collect(RefCollectors.toList());
  }

  @Nonnull
  @Override
  public DAGNetwork setFrozen(final boolean frozen) {
    super.setFrozen(frozen);
    visitLayers(layer -> layer.setFrozen(frozen));
    return this;
  }

  public static @SuppressWarnings("unused")
  DAGNetwork[] addRefs(DAGNetwork[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DAGNetwork::addRef)
        .toArray((x) -> new DAGNetwork[x]);
  }

  public static @SuppressWarnings("unused")
  DAGNetwork[][] addRefs(DAGNetwork[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(DAGNetwork::addRefs)
        .toArray((x) -> new DAGNetwork[x][]);
  }

  public void shuffle(long seed) {
    visitLayers(layer -> {
      if (layer instanceof StochasticComponent)
        ((StochasticComponent) layer).shuffle(seed);
    });
  }

  public void clearNoise() {
    visitLayers(layer -> {
      if (layer instanceof StochasticComponent)
        ((StochasticComponent) layer).clearNoise();
    });
  }

  @Nullable
  public InnerNode add(@Nonnull final Layer nextHead, final DAGNode... head) {
    return add(null, nextHead, head);
  }

  public InnerNode add(@Nullable final CharSequence label, @Nonnull final Layer layer, final DAGNode... head) {
    if (null == layer && head.length > 0)
      throw new IllegalArgumentException();
    if (null == layer)
      return null;
    assert RefArrays.stream(head)
        .allMatch(x -> x == null || internalNodes.containsKey(x.getId()) || inputNodes.containsKey(x.getId()));
    assert layer.assertAlive();
    if (null == layer)
      throw new IllegalArgumentException();
    assert layer.assertAlive();
    assertAlive();
    assertConsistent();
    assert null != inputHandles;
    @Nonnull final InnerNode node = new InnerNode(this, layer, head);
    DAGNode replaced = internalNodes.put(node.getId(), node);
    if (null != label)
      labels.put(label, node.getId());
    assertConsistent();
    return node;
  }

  @Nonnull
  public void addInput() {
    @Nonnull final UUID key = UUID.randomUUID();
    inputHandles.add(key);
    InputNode replaced = inputNodes.put(key, new InputNode(this, key));
    if (null != replaced)
      throw new RuntimeException("UUID Conflict: " + key);
  }

  public void attach(@Nonnull final MonitoredObject obj) {
    visitLayers(layer -> {
      if (layer instanceof MonitoredItem) {
        obj.addObj(layer.getName(), (MonitoredItem) layer);
      }
    });
  }

  @Nonnull
  public GraphEvaluationContext buildExeCtx(@Nonnull final Result... inputs) {
    assert inputs.length == inputHandles.size() : inputs.length + " != " + inputHandles.size();
    @Nonnull final GraphEvaluationContext context = new GraphEvaluationContext();
    for (int i = 0; i < inputs.length; i++) {
      UUID key = inputHandles.get(i);
      Result input = inputs[i];
      if (!context.calculated.containsKey(key)) {
        input.getData();
        context.calculated.put(key, new Singleton<CountingResult>().set(new CountingResult(input)));
      }
    }
    context.expectedCounts.putAll(getNodes().stream().flatMap(t -> {
      return RefArrays.stream(t.getInputs()).map(n -> n.getId());
    }).filter(x -> !inputHandles.contains(x)).collect(RefCollectors.groupingBy(x -> x,
        RefCollectors.counting())));
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
    GraphEvaluationContext buildExeCtx = buildExeCtx(input);
    DAGNode head = getHead();
    return head.get(buildExeCtx);
  }

  public DAGNode getNodeById(final UUID k) {
    DAGNode dagNode = internalNodes.get(k);
    return null == dagNode ? null : dagNode;
  }

  public final DAGNode getInput(final int index) {
    assertAlive();
    UUID key = inputHandles.get(index);
    final DAGNode input = inputNodes.get(key);
    if (null == input) {
      throw new IllegalStateException(String.format("No Input: %d: %s", index, key));
    }
    return input;
  }

  @Override
  public JsonObject getJson(Map<CharSequence, byte[]> resources,
                            DataSerializer dataSerializer) {
    assertAlive();
    @Nonnull final JsonObject json = super.getJsonStub();
    @Nonnull final JsonArray inputs = new JsonArray();
    json.add("inputs", inputs);
    inputHandles.forEach(uuid -> inputs.add(new JsonPrimitive(uuid.toString())));
    @Nonnull final JsonObject layerMap = new JsonObject();
    @Nonnull final JsonObject nodeMap = new JsonObject();
    @Nonnull final JsonObject links = new JsonObject();
    this.internalNodes.values().forEach(node -> {
      @Nonnull final JsonArray linkArray = new JsonArray();
      RefArrays.stream(node.getInputs())
          .forEach((@Nonnull final DAGNode input) -> linkArray.add(new JsonPrimitive(input.getId().toString())));
      @Nullable final Layer layer = node.getLayer();
      @Nonnull final String nodeId = node.getId().toString();
      final String layerId = layer.getId().toString();
      nodeMap.addProperty(nodeId, layerId);
      layerMap.add(layerId, layer.getJson(resources, dataSerializer));
      links.add(nodeId, linkArray);
    });
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
    return getChildren().stream().filter(x -> !x.isFrozen()).flatMap(l -> l.state().stream()).distinct()
        .collect(RefCollectors.toList());
  }

  public void visitLayers(@Nonnull final RefConsumer<Layer> visitor) {
    visitNodes(node -> {
      Layer layer = node.getLayer();
      Layer unwrapped = layer;
      while (unwrapped instanceof WrapperLayer) {
        unwrapped = ((WrapperLayer) unwrapped).getInner();
      }
      if (unwrapped instanceof DAGNetwork) {
        ((DAGNetwork) unwrapped).visitLayers(visitor);
      }
      while (layer instanceof WrapperLayer) {
        visitor.accept(layer);
        layer = ((WrapperLayer) layer).getInner();
      }
      visitor.accept(layer);
    });
  }

  public void visitNodes(@Nonnull final RefConsumer<DAGNode> visitor) {
    visitNodes(true, visitor);
  }

  public void visitNodes(boolean recurse, @Nonnull final RefConsumer<DAGNode> visitor) {
    assertAlive();
    this.internalNodes.values().forEach(node -> {
      node.assertAlive();
      Layer layer = node.getLayer();
      layer.assertAlive();
      while (layer instanceof WrapperLayer) {
        layer = ((WrapperLayer) layer).getInner();
      }
      if (recurse && layer instanceof DAGNetwork) {
        ((DAGNetwork) layer).visitNodes(visitor);
      }
      visitor.accept(node);
    });
  }

  public void _free() {
    super._free();
    this.inputNodes.clear();
  }

  public @Override
  @SuppressWarnings("unused")
  DAGNetwork addRef() {
    return (DAGNetwork) super.addRef();
  }

  protected boolean assertConsistent() {
    assertAlive();
    assert null != inputHandles;
    for (@Nonnull final Entry<CharSequence, UUID> e : labels.entrySet()) {
      assert internalNodes.containsKey(e.getValue());
    }
    return true;
  }

  private DAGNode[] getDependencies(
      @Nonnull final RefMap<UUID, RefList<UUID>> deserializedLinks,
      final UUID e) {
    final RefList<UUID> links = deserializedLinks.get(e);
    if (null == links)
      return new DAGNode[]{};
    return links.stream().map(id -> getNode(id)).toArray(i -> new DAGNode[i]);
  }

  private DAGNode getNode(final UUID id) {
    DAGNode returnValue = getNodeById(id);
    if (null == returnValue) {
      returnValue = inputNodes.get(id);
    }
    return returnValue;
  }

  private synchronized void initLinks(
      @Nonnull final RefMap<UUID, RefList<UUID>> nodeLinks,
      @Nonnull final RefMap<UUID, Layer> layersByNodeId, final UUID newNodeId) {
    RefMap<UUID, Layer> layersById = getLayersById();
    if (layersById.containsKey(newNodeId))
      return;
    if (inputNodes.containsKey(newNodeId))
      return;
    final Layer layer = layersByNodeId.get(newNodeId);
    if (layer == null) {
      throw new IllegalArgumentException(String.format("%s is linked to but not defined", newNodeId));
    }
    final RefList<UUID> links = nodeLinks.get(newNodeId);
    if (null != links) {
      for (final UUID link : links) {
        initLinks(nodeLinks, layersByNodeId, link);
      }
    }
    assertConsistent();
    @Nonnull final InnerNode node = new InnerNode(this, layer, newNodeId, getDependencies(nodeLinks, newNodeId));
    DAGNode replaced = internalNodes.put(node.getId(), node);
    assertConsistent();
  }
}
