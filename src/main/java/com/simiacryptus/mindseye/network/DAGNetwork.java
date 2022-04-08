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

/**
 * This class defines a DAGNetwork.
 *
 * @param inputHandles  the Input handles
 * @param inputNodes    the Input nodes
 * @param labels        the Labels
 * @param internalNodes the Internal nodes
 * @docgenVersion 9
 */
@SuppressWarnings("serial")
public abstract class DAGNetwork extends LayerBase {

  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DAGNetwork.class);
  /**
   * The Input handles.
   */
  public final RefArrayList<UUID> inputHandles = new RefArrayList<>();
  /**
   * The Input nodes.
   */
  public final RefLinkedHashMap<UUID, InputNode> inputNodes = new RefLinkedHashMap<>();
  /**
   * The Labels.
   */
  protected final LinkedHashMap<CharSequence, UUID> labels = new LinkedHashMap<>();
  /**
   * The Internal nodes.
   */
  protected final RefLinkedHashMap<UUID, DAGNode> internalNodes = new RefLinkedHashMap<>();

  /**
   * Instantiates a new Dag network.
   *
   * @param inputs the inputs
   * @param id     the id
   * @param name   the name
   */
  public DAGNetwork(final int inputs, UUID id, String name) {
    super(id, name);
    assert 0 < inputs;
    for (int i = 0; i < inputs; i++) {
      addInput();
    }
  }

  /**
   * Instantiates a new Dag network.
   *
   * @param inputs the inputs
   */
  public DAGNetwork(final int inputs) {
    super();
    //assert 0 < inputs;
    for (int i = 0; i < inputs; i++) {
      addInput();
    }
  }

  /**
   * Instantiates a new Dag network.
   *
   * @param json the json
   * @param rs   the rs
   */
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

  /**
   * @return a list of child layers
   * @docgenVersion 9
   */
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

  /**
   * @return the head of the DAG, or null if the DAG is empty
   * @docgenVersion 9
   */
  @Nullable
  public abstract DAGNode getHead();

  /**
   * Returns the UUID of the head node.
   *
   * @docgenVersion 9
   */
  public UUID getHeadId() {
    DAGNode head = getHead();
    assert head != null;
    UUID temp_38_0003 = head.getId();
    head.freeRef();
    return temp_38_0003;
  }

  /**
   * Returns an unmodifiable list of layers.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public RefList<Layer> getLayers() {
    RefList<Layer> list = new RefArrayList<>();
    visitLayers(RefUtil.wrapInterface(layer -> {
      list.add(layer);
    }, RefUtil.addRef(list)));
    return RefCollections.unmodifiableList(list);
  }

  /**
   * Returns a map of layers, keyed by their UUIDs.
   *
   * @return a map of layers, keyed by their UUIDs
   * @docgenVersion 9
   */
  @Nonnull
  public RefMap<UUID, Layer> getLayersById() {
    RefLinkedHashMap<UUID, Layer> map = new RefLinkedHashMap<>();
    visitLayers(RefUtil.wrapInterface(layer -> {
      RefUtil.freeRef(map.put(layer.getId(), layer));
    }, RefUtil.addRef(map)));
    return RefCollections.unmodifiableMap(map);
  }

  /**
   * Returns a list of all DAGNodes in this DAG, including both internal and input nodes.
   *
   * @docgenVersion 9
   */
  public RefList<DAGNode> getNodes() {
    RefList<DAGNode> allNodes = new RefArrayList<>();
    this.internalNodes.forEach((k, v) -> {
      allNodes.add(v);
    });
    this.inputNodes.forEach((k, v) -> {
      allNodes.add(v);
    });
    return allNodes;
  }

  /**
   * Returns a map of DAGNode objects, keyed by UUID, for all nodes in the current layer.
   *
   * @docgenVersion 9
   */
  protected RefMap<UUID, DAGNode> getNodesByLayerId() {
    RefHashMap<UUID, DAGNode> map = new RefHashMap<>();
    internalNodes.forEach((nodeId, node) -> {
      Layer layer = node.getLayer();
      if (layer != null) {
        RefUtil.freeRef(map.put(layer.getId(), node));
        layer.freeRef();
      } else {
        node.freeRef();
      }
    });
    return map;
  }

  /**
   * Sets the frozen state of this layer and all its child layers.
   *
   * @param frozen the new frozen state
   * @docgenVersion 9
   */
  @Override
  public void setFrozen(final boolean frozen) {
    super.setFrozen(frozen);
    visitLayers(layer -> {
      layer.setFrozen(frozen);
      layer.freeRef();
    });
  }

  /**
   * @NotNull private static RefMap<UUID, Layer> getLayersById(JsonObject jsonLayers, Map<CharSequence, byte[]> rs);
   * @docgenVersion 9
   */
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

  /**
   * @return a LinkedHashMap mapping label names to UUIDs, or null if jsonLabels is null
   * @docgenVersion 9
   */
  @NotNull
  private static LinkedHashMap<CharSequence, UUID> getLabels(JsonObject jsonLabels) {
    @Nonnull final LinkedHashMap<CharSequence, UUID> labels = new LinkedHashMap<>();
    for (@Nonnull final Entry<String, JsonElement> e : jsonLabels.entrySet()) {
      labels.put(e.getKey(), UUID.fromString(e.getValue().getAsString()));
    }
    return labels;
  }

  /**
   * @return a map of UUIDs to lists of UUIDs, or null if the input is null
   * @docgenVersion 9
   */
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

  /**
   * Dereferences a map of UUIDs to long values.
   *
   * @param refMap the map to dereference
   * @return a new map with the same data as the original
   * @docgenVersion 9
   */
  @NotNull
  private static HashMap<UUID, Long> deref(@RefAware Map<UUID, Long> refMap) {
    try {
      HashMap<UUID, Long> hashMap = new HashMap<>();
      refMap.forEach((k, v) -> hashMap.put(k, v));
      return hashMap;
    } finally {
      RefUtil.freeRef(refMap);
    }
  }

  /**
   * Transfers the given node from the given source DAGNetwork to this DAGNetwork.
   *
   * @param source the DAGNetwork to transfer the node from
   * @param node   the node to transfer
   * @return the transferred node
   * @throws NullPointerException if the given node is null
   * @docgenVersion 9
   */
  @NotNull
  public InnerNode transferNode(DAGNetwork source, @Nonnull DAGNode node) {
    try {
      RefMap<UUID, DAGNode> nodesByLayerId = getNodesByLayerId();
      final DAGNode[] directInputs = RefArrays.stream(node.getInputs())
          .map((DAGNode input) -> {
            final UUID inputId = input.getId();
            assert source != null;
            if (source.inputNodes.containsKey(inputId)) {
              input.freeRef();
              return getInput(source.inputHandles.indexOf(inputId));
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
              if (dagNode != null) {
                input.freeRef();
                return dagNode;
              } else {
                return transferNode(source.addRef(), input);
              }
            }
          }).toArray(DAGNode[]::new);
      nodesByLayerId.freeRef();
      return add(node.getLayer(), directInputs);
    } finally {
      node.freeRef();
      source.freeRef();
    }
  }

  /**
   * This method shuffles the elements in the list using the given seed.
   *
   * @docgenVersion 9
   */
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

  /**
   * Clears noise from all stochastic components in the network.
   *
   * @docgenVersion 9
   */
  public void clearNoise() {
    visitLayers(layer -> {
      if (layer instanceof StochasticComponent)
        ((StochasticComponent) layer).clearNoise();
      if (null != layer)
        layer.freeRef();
    });
  }

  /**
   * @param nextHead the next head layer to add
   * @param head     the DAGNode(s) to add as head(s)
   * @return the resulting InnerNode
   * @docgenVersion 9
   */
  @Nonnull
  public InnerNode add(@Nonnull final Layer nextHead, @Nullable final DAGNode... head) {
    return add(null, nextHead, head);
  }

  /**
   * Add a node with the given label and layer to the DAG, pointing to the given head nodes.
   *
   * @param label the label for the new node
   * @param layer the layer for the new node
   * @param head  the head nodes for the new node
   * @return the new node
   * @docgenVersion 9
   */
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

  /**
   * Add an input to the current node.
   *
   * @docgenVersion 9
   */
  public void addInput() {
    assertAlive();
    @Nonnull final UUID key = UUID.randomUUID();
    inputHandles.add(key);
    RefUtil.freeRef(inputNodes.put(key, new InputNode(key)));
  }

  /**
   * Attaches the given MonitoredObject to this monitor.
   *
   * @param obj the MonitoredObject to attach
   * @throws NullPointerException if obj is null
   * @docgenVersion 9
   */
  public void attach(@Nonnull final MonitoredObject obj) {
    visitLayers(RefUtil.wrapInterface(layer -> {
      if (layer instanceof MonitoredItem) {
        obj.addObj(layer.getName(), (MonitoredItem) layer);
      } else {
        if (null != layer) layer.freeRef();
      }
    }, obj));
  }

  /**
   * Builds an execution context for the given inputs.
   *
   * @param inputs the inputs to use
   * @return the execution context
   * @docgenVersion 9
   */
  @Nonnull
  public GraphEvaluationContext buildExeCtx(@Nonnull final Result... inputs) {
    int length = inputs.length;
    if (length != inputHandles.size()) {
      RefUtil.freeRef(inputs);
      throw new IllegalArgumentException(length + " != " + inputHandles.size());
    }
    @Nonnull final GraphEvaluationContext context = new GraphEvaluationContext();
    initCalculated(context.getCalculated(), inputs);
    context.getExpectedCounts().putAll(initNodeRefcounts(getNodes()));
    return context;
  }

  /**
   * Initializes a map of node reference counts, mapping each node's UUID to its initial reference count.
   *
   * @param nodes the list of nodes
   * @return the map of node reference counts
   * @docgenVersion 9
   */
  public Map<UUID, Long> initNodeRefcounts(RefList<DAGNode> nodes) {
    RefMap<UUID, Long> nodeIdReferenceCounts = nodes.stream().flatMap(node -> {
          DAGNode[] nodeInputs = node.getInputs();
          if (null != node)
            node.freeRef();
          return Arrays.stream(nodeInputs).map(inputNode -> {
            UUID id = inputNode.getId();
            inputNode.freeRef();
            return id;
          });
        }).filter(id -> !inputHandles.contains(id))
        .collect(RefCollectors.groupingBy(x -> x, RefCollectors.counting()));
    nodes.freeRef();
    return deref(nodeIdReferenceCounts);
  }

  /**
   * Initializes the calculated map with the inputs.
   *
   * @param calculated the map to initialize
   * @param inputs     the inputs to use
   * @docgenVersion 9
   */
  public void initCalculated(RefMap<UUID, RefAtomicReference<CountingResult>> calculated, @Nonnull Result[] inputs) {
    try {
      for (int i = 0; i < inputs.length; i++)
        synchronized (calculated) {
          RefUtil.freeRef(calculated.put(
              inputHandles.get(i),
              new RefAtomicReference<>(new CountingResult(inputs[i].addRef()))
          ));
        }
    } finally {
      calculated.freeRef();
      RefUtil.freeRef(inputs);
    }
  }

  /**
   * Returns a copy of this DAGNetwork with the specified precision.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public DAGNetwork copy(SerialPrecision precision) {
    return (DAGNetwork) super.copy(precision);
  }

  /**
   * @Nullable
   * @Override public Result eval(@Nullable final Result... input) {
   * assertAlive();
   * @Nonnull DAGNode head = getHead();
   * assert head != null;
   * try {
   * return head.get(buildExeCtx(input), null);
   * } finally {
   * head.freeRef();
   * }
   * }
   * @docgenVersion 9
   */
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

  /**
   * @return the node with the given id, or null if no such node exists
   * @docgenVersion 9
   */
  @Nullable
  public DAGNode getNodeById(final UUID k) {
    return internalNodes.get(k);
  }

  /**
   * @return the input DAGNode at the given index; never null
   * @throws IndexOutOfBoundsException if the index is out of range
   * @docgenVersion 9
   */
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

  /**
   * @Override public JsonObject getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer);
   * @docgenVersion 9
   */
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

  /**
   * Resets the tree by clearing all internal nodes and labels.
   *
   * @docgenVersion 9
   */
  public synchronized void reset() {
    this.internalNodes.clear();
    labels.clear();
  }

  /**
   * @return the state of the RefList as an array of doubles
   * @docgenVersion 9
   */
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

  /**
   * Visits each layer in the current map.
   *
   * @param visitor the consumer of each layer
   * @docgenVersion 9
   */
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

  /**
   * Visits all nodes in the DAG, optionally including disabled nodes.
   *
   * @param includeDisabled if true, disabled nodes will be visited as well
   * @param visitor         the consumer of nodes
   * @docgenVersion 9
   */
  public void visitNodes(@Nonnull @RefAware final RefConsumer<DAGNode> visitor) {
    visitNodes(true, visitor);
  }

  /**
   * Visit each node in the DAG, optionally recursing into child nodes.
   *
   * @param recurse whether to recurse into child nodes
   * @param visitor the visitor to invoke for each node
   * @docgenVersion 9
   */
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

  /**
   * Frees the resources used by this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    internalNodes.freeRef();
    inputNodes.freeRef();
    inputHandles.freeRef();
    super._free();
  }

  /**
   * Adds a reference to this DAGNetwork and returns it.
   *
   * @docgenVersion 9
   */
  public @Override
  @SuppressWarnings("unused")
  DAGNetwork addRef() {
    return (DAGNetwork) super.addRef();
  }

  /**
   * Checks whether the current state of the object is consistent with its internal representation.
   *
   * @return true if the state is consistent, false otherwise
   * @docgenVersion 9
   */
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

  /**
   * @return a map of node IDs to layers, based on the given JSON object and map of source layers
   * @docgenVersion 9
   */
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

  /**
   * Get the dependencies for the given node.
   *
   * @param linkMap the map of links
   * @param id      the node ID
   * @return the node's dependencies
   * @docgenVersion 9
   */
  @Nonnull
  private DAGNode[] getDependencies(@Nonnull final Map<UUID, List<UUID>> linkMap, final UUID id) {
    final List<UUID> links = linkMap.get(id);
    if (null == links) {
      return new DAGNode[]{};
    }
    return links.stream().map(this::getNode).toArray(DAGNode[]::new);
  }

  /**
   * Gets the node.
   *
   * @param id the id
   * @return the node
   * @docgenVersion 9
   */
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

  /**
   * Initializes links for the given node.
   *
   * @param nodeLinks      a map of node IDs to lists of node IDs that they are linked to
   * @param layersByNodeId a map of node IDs to the layers that they are in
   * @param newNodeId      the ID of the node to initialize links for
   * @docgenVersion 9
   */
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
