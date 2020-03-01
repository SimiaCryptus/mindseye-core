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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public abstract class DAGNetwork extends LayerBase {

  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DAGNetwork.class);
  public final RefList<UUID> inputHandles = new RefArrayList<>();
  public final RefLinkedHashMap<UUID, InputNode> inputNodes = new RefLinkedHashMap<>();
  protected final LinkedHashMap<CharSequence, UUID> labels = new LinkedHashMap<>();
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
      RefUtil.freeRef(inputNodes.put(key, new InputNode(key)));
    }
    @Nonnull final RefMap<UUID, Layer> layersByLayerId = getLayersById(json.getAsJsonObject("layers"), rs);
    @Nonnull final RefMap<UUID, Layer> layersByNodeId = getLayersByNodeId(json.getAsJsonObject("nodes"), layersByLayerId);
    @Nonnull final LinkedHashMap<CharSequence, UUID> nodeLabels = getLabels(json.getAsJsonObject("labels"));
    @Nonnull final Map<UUID, List<UUID>> links = getLinks(json.getAsJsonObject("links"));
    Collection<UUID> values = nodeLabels.values();
    values.forEach(key -> initLinks(links, layersByNodeId.addRef(), key));
    RefSet<UUID> nodeIds = layersByNodeId.keySet();
    nodeIds.forEach(key -> initLinks(links, layersByNodeId.addRef(), key));
    nodeIds.freeRef();
    @Nonnull final UUID head = UUID.fromString(json.getAsJsonPrimitive("head").getAsString());
    initLinks(links, layersByNodeId, head);
    this.labels.putAll(nodeLabels);
    assertConsistent();
  }

  protected RefMap<UUID, DAGNode> getNodesByLayerId() {
    RefHashMap<UUID, DAGNode> map = new RefHashMap<>();
    internalNodes.forEach((nodeId,node)->{
      Layer layer = node.getLayer();
      if(layer != null) {
        RefUtil.freeRef(map.put(layer.getId(), node));
        layer.freeRef();
      } else {
        node.freeRef();
      }
    });
    return map;
  }

  @NotNull
  public static InnerNode transferNode(@Nonnull DAGNetwork destinationNetwork, @Nonnull DAGNode node, DAGNetwork sourceNetwork) {
    try {
      RefMap<UUID, DAGNode> nodesByLayerId = destinationNetwork.getNodesByLayerId();
      final DAGNode[] dagNodes = RefArrays.stream(node.getInputs())
          .map((DAGNode input) -> {
            final UUID inputId = input.getId();
            assert sourceNetwork != null;
            if (sourceNetwork.inputNodes.containsKey(inputId)) {
              input.freeRef();
              return destinationNetwork.getInput(sourceNetwork.inputHandles.indexOf(inputId));
            } else {
              Layer inputLayer = input.getLayer();
              if (inputLayer == null) {
                Class<? extends DAGNode> inputClass = input.getClass();
                input.freeRef();
                throw new IllegalArgumentException(inputClass.toString());
              }
              UUID inputLayerId = inputLayer.getId();
              inputLayer.freeRef();
              DAGNode dagNode = nodesByLayerId.get(inputLayerId);
              if(dagNode != null) {
                input.freeRef();
                return dagNode;
              } else {
                return transferNode(destinationNetwork.addRef(), input, sourceNetwork.addRef());
              }
            }
          }).toArray(DAGNode[]::new);
      nodesByLayerId.freeRef();
      return destinationNetwork.add(node.getLayer(), dagNodes);
    } finally {
      destinationNetwork.freeRef();
      node.freeRef();
      sourceNetwork.freeRef();
    }
  }

  @NotNull
  private RefMap<UUID, Layer> getLayersByNodeId(JsonObject jsonNodes, RefMap<UUID, Layer> source_layersByLayerId) {
    @Nonnull final RefMap<UUID, Layer> source_layersByNodeId = new RefHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonNodes.entrySet()) {
      @Nonnull final UUID nodeId = UUID.fromString(e.getKey());
      @Nonnull final UUID layerId = UUID.fromString(e.getValue().getAsString());
      final Layer layer = source_layersByLayerId.get(layerId);
      assert null != layer;
      RefUtil.freeRef(source_layersByNodeId.put(nodeId, layer.addRef()));
      layer.freeRef();
    }
    source_layersByLayerId.freeRef();
    return source_layersByNodeId;
  }

  @NotNull
  private static RefMap<UUID, Layer> getLayersById(JsonObject jsonLayers, Map<CharSequence, byte[]> rs) {
    @Nonnull final RefMap<UUID, Layer> source_layersByLayerId = new RefHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLayers.entrySet()) {
      @Nonnull
      Layer value = Layer.fromJson(e.getValue().getAsJsonObject(), rs);
      RefUtil.freeRef(source_layersByLayerId.put(UUID.fromString(e.getKey()), value));
    }
    return source_layersByLayerId;
  }

  @NotNull
  private static LinkedHashMap<CharSequence, UUID> getLabels(JsonObject jsonLabels) {
    @Nonnull final LinkedHashMap<CharSequence, UUID> labels = new LinkedHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLabels.entrySet()) {
      labels.put(e.getKey(), UUID.fromString(e.getValue().getAsString()));
    }
    return labels;
  }

  @NotNull
  private static Map<UUID, List<UUID>> getLinks(JsonObject jsonLinks) {
    @Nonnull final Map<UUID, List<UUID>> links = new HashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLinks.entrySet()) {
      @Nonnull final ArrayList<UUID> linkList = new ArrayList<>();
      for (@Nonnull final JsonElement linkItem : e.getValue().getAsJsonArray()) {
        linkList.add(UUID.fromString(linkItem.getAsString()));
      }
      links.put(UUID.fromString(e.getKey()), linkList);
    }
    return links;
  }

  @Override
  public RefList<Layer> getChildren() {
    assertAlive();
    RefCollection<Layer> values = getLayers();
    RefStream<Layer> stream = values.stream();
    values.freeRef();
    return stream.flatMap(l -> {
      RefList<Layer> children = l.getChildren();
      RefStream<Layer> temp_38_0001 = children.stream();
      l.freeRef();
      children.freeRef();
      return temp_38_0001;
    }).distinct().sorted(RefComparator.comparing(layer -> {
      String string = layer.getId().toString();
      layer.freeRef();
      return string;
    })).collect(RefCollectors.toList());
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
  public RefList<Layer> getLayers() {
    RefList<Layer> list = new RefArrayList<>();
    visitLayers(RefUtil.wrapInterface(layer -> {
      list.add(layer);
    }, RefUtil.addRef(list)));
    return RefCollections.unmodifiableList(list);
  }

  @Nonnull
  public RefMap<UUID, Layer> getLayersById() {
    RefLinkedHashMap<UUID, Layer> map = new RefLinkedHashMap<>();
    visitLayers(RefUtil.wrapInterface(layer -> {
      RefUtil.freeRef(map.put(layer.getId(), layer));
    }, RefUtil.addRef(map)));
    return RefCollections.unmodifiableMap(map);
  }

  public RefList<DAGNode> getNodes() {
    RefList<DAGNode> allNodes = new RefArrayList<>();
    this.internalNodes.forEach((k,v)-> {
      allNodes.add(v);
    });
    this.inputNodes.forEach((k,v)->{
      allNodes.add(v);
    });
    return allNodes;
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
      try {
        if (layer instanceof StochasticComponent)
          ((StochasticComponent) layer).shuffle(seed);
      } finally {
        if (null != layer)
          layer.freeRef();
      }
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
    return add(null, nextHead, head);
  }

  @Nonnull
  public InnerNode add(@Nullable final CharSequence label, @Nonnull final Layer layer, @Nonnull final DAGNode... head) {
    for (DAGNode dagNode : head) {
      assert dagNode == null || internalNodes.containsKey(dagNode.getId()) || inputNodes.containsKey(dagNode.getId());
    }
    layer.assertAlive();
    assertAlive();
    assertConsistent();
    assert null != inputHandles;
    @Nonnull final InnerNode node = new InnerNode(layer, head);
    UUID nodeId = node.getId();
    RefUtil.freeRef(internalNodes.put(nodeId, node.addRef()));
    if (null != label)
      RefUtil.freeRef(labels.put(label, nodeId));
    assertConsistent();
    return node;
  }

  @Nonnull
  public void addInput() {
    assertAlive();
    @Nonnull final UUID key = UUID.randomUUID();
    inputHandles.add(key);
    RefUtil.freeRef(inputNodes.put(key, new InputNode(key)));
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
    int length = inputs.length;
    if (length != inputHandles.size()) {
      RefUtil.freeRef(inputs);
      throw new IllegalArgumentException(length + " != " + inputHandles.size());
    }
    @Nonnull final GraphEvaluationContext context = new GraphEvaluationContext();
    RefMap<UUID, RefAtomicReference<CountingResult>> calculated = context.getCalculated();
    initCalculated(length, calculated, inputs);
    RefMap<UUID, Long> nodeIdReferenceCounts = initNodeRefcounts(getNodes());
    if (!nodeIdReferenceCounts.isEmpty()) {
      RefMap<UUID, Long> expectedCounts = context.getExpectedCounts();
      expectedCounts.putAll(nodeIdReferenceCounts);
      expectedCounts.freeRef();
    } else {
      nodeIdReferenceCounts.freeRef();
    }
    return context;
  }

  public RefMap<UUID, Long> initNodeRefcounts(RefList<DAGNode> nodes) {
    RefMap<UUID, Long> nodeIdReferenceCounts = nodes.stream().flatMap(node -> {
      DAGNode[] nodeInputs = node.getInputs();
      if (null != node)
        node.freeRef();
      return RefArrays.stream(nodeInputs).map(inputNode -> {
        UUID id = inputNode.getId();
        inputNode.freeRef();
        return id;
      });
    })
        .filter(id->!inputHandles.contains(id))
        .collect(RefCollectors.groupingBy(x -> x, RefCollectors.counting()));
    nodes.freeRef();
    return nodeIdReferenceCounts;
  }

  public void initCalculated(int length, RefMap<UUID, RefAtomicReference<CountingResult>> calculated, @Nonnull Result[] inputs) {
    try {
      for (int i = 0; i < length; i++) {
        UUID key = inputHandles.get(i);
        RefUtil.freeRef(calculated.put(key, new RefAtomicReference<>(new CountingResult(inputs[i].addRef()))));
      }
    } finally {
      calculated.freeRef();
      RefUtil.freeRef(inputs);
    }
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
    DAGNode head = getHead();
    assert head != null;
    try {
      return head.get(buildExeCtx(input), null);
    } finally {
      head.freeRef();
    }
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
    this.internalNodes.forEach((id, node) -> {
      @Nonnull final JsonArray linkArray = new JsonArray();
      RefArrays.stream(node.getInputs()).forEach((@Nonnull final DAGNode input) -> {
        linkArray.add(new JsonPrimitive(input.getId().toString()));
        input.freeRef();
      });
      @Nullable final Layer layer = node.getLayer();
      @Nonnull final String nodeId = node.getId().toString();
      node.freeRef();
      RefUtil.freeRef(id);
      assert layer != null;
      final String layerId = layer.getId().toString();
      nodeMap.addProperty(nodeId, layerId);
      layerMap.add(layerId, layer.getJson(resources, dataSerializer));
      layer.freeRef();
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
    RefList<Layer> temp_38_0023 = getChildren();
    RefStream<Layer> stream = temp_38_0023.stream();
    temp_38_0023.freeRef();
    return stream.filter(x -> {
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
  }

  public void visitLayers(@Nonnull @RefAware final RefConsumer<Layer> visitor) {
    assertAlive();
    visitNodes(false, RefUtil.wrapInterface(node -> {
      Layer layer = node.getLayer();
      node.freeRef();
      while (layer instanceof WrapperLayer) {
        Layer inner = ((WrapperLayer) layer).getInner();
        assert null != inner;
        if (inner instanceof DAGNetwork) {
          ((DAGNetwork) inner).visitLayers(RefUtil.addRef(visitor));
        }
        visitor.accept(layer);
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
    RefHashSet<DAGNode> nodes = this.internalNodes.values();
    try {
      nodes.forEach(RefUtil.wrapInterface(node -> {
        Layer layer = node.getLayer();
        try {
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
        } finally {
          if (null != layer)
            layer.freeRef();
        }
        visitor.accept(node);
      }, visitor));
    } finally {
      nodes.freeRef();
    }
  }

  public void _free() {
    internalNodes.freeRef();
    inputNodes.freeRef();
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
    Set<Entry<CharSequence, UUID>> entries = labels.entrySet();
    assert entries.stream().allMatch(e -> {
      boolean containsKey = internalNodes.containsKey(e.getValue());
      RefUtil.freeRef(e);
      return containsKey;
    });
    return true;
  }

  @Nonnull
  private DAGNode[] getDependencies(@Nonnull final Map<UUID, List<UUID>> linkMap, final UUID id) {
    final List<UUID> links = linkMap.get(id);
    if (null == links) {
      return new DAGNode[]{};
    }
    return links.stream().map(this::getNode).toArray(DAGNode[]::new);
  }

  @Nullable
  private DAGNode getNode(final UUID id) {
    DAGNode returnValue = getNodeById(id);
    if (null != returnValue) {
      return returnValue;
    } else {
      RefUtil.freeRef(returnValue);
      return inputNodes.get(id);
    }
  }

  private synchronized void initLinks(@Nonnull final Map<UUID, List<UUID>> nodeLinks,
                                      @Nonnull final RefMap<UUID, Layer> layersByNodeId, final UUID newNodeId) {
    if (inputNodes.containsKey(newNodeId)) {
      layersByNodeId.freeRef();
      return;
    }
    RefMap<UUID, Layer> layersById = getLayersById();
    if (layersById.containsKey(newNodeId)) {
      layersById.freeRef();
      layersByNodeId.freeRef();
      return;
    }
    layersById.freeRef();
    final Layer layer = layersByNodeId.get(newNodeId);
    if (layer == null) {
      layersByNodeId.freeRef();
      throw new IllegalArgumentException(RefString.format("%s is linked to but not defined", newNodeId));
    }
    final List<UUID> links = nodeLinks.get(newNodeId);
    if (null != links) {
      links.forEach(link -> initLinks(nodeLinks, layersByNodeId.addRef(), link));
    }
    layersByNodeId.freeRef();
    assertConsistent();
    @Nonnull final InnerNode node = new InnerNode(layer, newNodeId,
        getDependencies(nodeLinks, newNodeId));
    RefUtil.freeRef(internalNodes.put(node.getId(), node));
    assertConsistent();
  }
}
