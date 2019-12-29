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

import com.google.gson.*;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.mindseye.layers.StochasticComponent;
import com.simiacryptus.mindseye.layers.WrapperLayer;
import com.simiacryptus.util.MonitoredItem;
import com.simiacryptus.util.MonitoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("serial")
public abstract class DAGNetwork extends LayerBase {

  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DAGNetwork.class);
  public final List<UUID> inputHandles = new ArrayList<>();
  public final LinkedHashMap<UUID, InputNode> inputNodes = new LinkedHashMap<>();
  protected final LinkedHashMap<CharSequence, UUID> labels = new LinkedHashMap<>();
  protected final LinkedHashMap<UUID, DAGNode> internalNodes = new LinkedHashMap<>();

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
      InputNode replaced = inputNodes.put(key, new InputNode(this, key));
    }
    final JsonObject jsonNodes = json.getAsJsonObject("nodes");
    final JsonObject jsonLayers = json.getAsJsonObject("layers");
    final JsonObject jsonLinks = json.getAsJsonObject("links");
    final JsonObject jsonLabels = json.getAsJsonObject("labels");
    @Nonnull
    final Map<UUID, Layer> source_layersByNodeId = new HashMap<>();
    @Nonnull
    final Map<UUID, Layer> source_layersByLayerId = new HashMap<>();
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonLayers.entrySet()) {
      @Nonnull
      Layer value = Layer.fromJson(e.getValue().getAsJsonObject(), rs);
      source_layersByLayerId.put(UUID.fromString(e.getKey()), value);
    }
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonNodes.entrySet()) {
      @Nonnull
      final UUID nodeId = UUID.fromString(e.getKey());
      @Nonnull
      final UUID layerId = UUID.fromString(e.getValue().getAsString());
      final Layer layer = source_layersByLayerId.get(layerId);
      assert null != layer;
      source_layersByNodeId.put(nodeId, layer);
    }
    @Nonnull
    final LinkedHashMap<CharSequence, UUID> labels = new LinkedHashMap<>();
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonLabels.entrySet()) {
      labels.put(e.getKey(), UUID.fromString(e.getValue().getAsString()));
    }
    @Nonnull
    final Map<UUID, List<UUID>> deserializedLinks = new HashMap<>();
    for (@Nonnull
    final Entry<String, JsonElement> e : jsonLinks.entrySet()) {
      @Nonnull
      final ArrayList<UUID> linkList = new ArrayList<>();
      for (@Nonnull
      final JsonElement linkItem : e.getValue().getAsJsonArray()) {
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
    @Nonnull
    final UUID head = UUID.fromString(json.getAsJsonPrimitive("head").getAsString());
    initLinks(deserializedLinks, source_layersByNodeId, head);
    this.labels.putAll(labels);
    assertConsistent();
    source_layersByLayerId.values().forEach(ReferenceCounting::freeRef);
  }

  @Nonnull
  public static UnaryOperator<String> getReplacementOperator(final Map<String, String> replacements) {
    return json -> {
      for (final Entry<String, String> entry : replacements.entrySet()) {
        String regex = entry.getKey();
        String newValue = entry.getValue();
        //regex = regex.replaceAll("\\-", "\\\\-");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        log.debug(String.format("%s (%s) => %s", pattern, entry.getKey(), newValue));
        json = replaceAll(pattern.matcher(json), newValue);
      }
      return json;
    };
  }

  public static String replaceAll(final Matcher matcher, String replacement) {
    matcher.reset();
    boolean result = matcher.find();
    if (result) {
      int cnt = 0;
      StringBuffer sb = new StringBuffer();
      do {
        matcher.appendReplacement(sb, replacement);
        result = matcher.find();
        cnt++;
      } while (result);
      matcher.appendTail(sb);
      log.debug(String.format("Replaced %d instances", cnt));
      return sb.toString();
    }
    return replacement;
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

  @Nullable
  public InnerNode wrap(@Nonnull final Layer nextHead, final DAGNode... head) {
    return add(null, nextHead, head);
  }

  public InnerNode add(@Nullable final CharSequence label, @Nonnull final Layer layer, final DAGNode... head) {
    if (null == layer && head.length > 0)
      throw new IllegalArgumentException();
    if (null == layer)
      return null;
    assert Arrays.stream(head)
        .allMatch(x -> x == null || internalNodes.containsKey(x.getId()) || inputNodes.containsKey(x.getId()));
    assert layer.assertAlive();
    if (null == layer)
      throw new IllegalArgumentException();
    assert layer.assertAlive();
    assertAlive();
    assertConsistent();
    assert null != inputHandles;
    @Nonnull
    final InnerNode node = new InnerNode(this, layer, head);
    DAGNode replaced = internalNodes.put(node.getId(), node);
    if (null != label)
      labels.put(label, node.getId());
    assertConsistent();
    return node;
  }

  @Override
  protected void _free() {
    super._free();
    this.internalNodes.values().forEach(ReferenceCounting::freeRef);
    this.inputNodes.values().forEach(ReferenceCounting::freeRef);
    this.inputNodes.clear();
  }

  @Nonnull
  public Layer addInput() {
    @Nonnull
    final UUID key = UUID.randomUUID();
    inputHandles.add(key);
    InputNode replaced = inputNodes.put(key, new InputNode(this, key));
    if (null != replaced)
      throw new RuntimeException("UUID Conflict: " + key);
    return this;
  }

  protected boolean assertConsistent() {
    assertAlive();
    assert null != inputHandles;
    for (@Nonnull
    final Entry<CharSequence, UUID> e : labels.entrySet()) {
      assert internalNodes.containsKey(e.getValue());
    }
    return true;
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
    @Nonnull
    final GraphEvaluationContext context = new GraphEvaluationContext();
    for (int i = 0; i < inputs.length; i++) {
      UUID key = inputHandles.get(i);
      Result input = inputs[i];
      if (!context.calculated.containsKey(key)) {
        input.getData();
        context.calculated.put(key, new Singleton<CountingResult>().set(new CountingResult(input)));
      }
    }
    context.expectedCounts.putAll(getNodes().stream().flatMap(t -> {
      return Arrays.stream(t.getInputs()).map(n -> n.getId());
    }).filter(x -> !inputHandles.contains(x)).collect(Collectors.groupingBy(x -> x, Collectors.counting())));
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

  public DAGNode getChildNode(final UUID id) {
    synchronized (internalNodes) {
      if (internalNodes.containsKey(id)) {
        return internalNodes.get(id);
      }
    }
    return this.internalNodes.values().stream().map(x -> x.getLayer()).filter(x -> x instanceof DAGNetwork)
        .map(x -> ((DAGNetwork) x).getChildNode(id)).filter(x -> x != null).findAny().orElse(null);
  }

  @Override
  public List<Layer> getChildren() {
    return getLayersById().values().stream().flatMap(l -> l.getChildren().stream()).distinct()
        .sorted(Comparator.comparing(l -> l.getId().toString())).collect(Collectors.toList());
  }

  private DAGNode[] getDependencies(@Nonnull final Map<UUID, List<UUID>> deserializedLinks, final UUID e) {
    final List<UUID> links = deserializedLinks.get(e);
    if (null == links)
      return new DAGNode[] {};
    return links.stream().map(id -> getNode(id)).toArray(i -> new DAGNode[i]);
  }

  @Nullable
  public abstract DAGNode getHead();

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
    this.internalNodes.values().forEach(node -> {
      @Nonnull
      final JsonArray linkArray = new JsonArray();
      Arrays.stream(node.getInputs())
          .forEach((@Nonnull final DAGNode input) -> linkArray.add(new JsonPrimitive(input.getId().toString())));
      @Nullable
      final Layer layer = node.getLayer();
      @Nonnull
      final String nodeId = node.getId().toString();
      final String layerId = layer.getId().toString();
      nodeMap.addProperty(nodeId, layerId);
      layerMap.add(layerId, layer.getJson(resources, dataSerializer));
      links.add(nodeId, linkArray);
    });
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

  @Nonnull
  public Layer getLayer() {
    return this;
  }

  private DAGNode getNode(final UUID id) {
    DAGNode returnValue = getNodeById(id);
    if (null == returnValue) {
      returnValue = inputNodes.get(id);
    }
    return returnValue;
  }

  public List<DAGNode> getNodes() {
    return Stream.concat(this.internalNodes.values().stream(), inputHandles.stream().map(inputNodes::get))
        .collect(Collectors.toList());
  }

  private synchronized void initLinks(@Nonnull final Map<UUID, List<UUID>> nodeLinks,
      @Nonnull final Map<UUID, Layer> layersByNodeId, final UUID newNodeId) {
    Map<UUID, Layer> layersById = getLayersById();
    if (layersById.containsKey(newNodeId))
      return;
    if (inputNodes.containsKey(newNodeId))
      return;
    final Layer layer = layersByNodeId.get(newNodeId);
    if (layer == null) {
      throw new IllegalArgumentException(String.format("%s is linked to but not defined", newNodeId));
    }
    final List<UUID> links = nodeLinks.get(newNodeId);
    if (null != links) {
      for (final UUID link : links) {
        initLinks(nodeLinks, layersByNodeId, link);
      }
    }
    assertConsistent();
    @Nonnull
    final InnerNode node = new InnerNode(this, layer, newNodeId, getDependencies(nodeLinks, newNodeId));
    DAGNode replaced = internalNodes.put(node.getId(), node);
    assertConsistent();
  }

  public synchronized void reset() {
    this.internalNodes.values().forEach(ReferenceCounting::freeRef);
    this.internalNodes.clear();
    labels.clear();
  }

  @Nonnull
  @Override
  public DAGNetwork setFrozen(final boolean frozen) {
    super.setFrozen(frozen);
    visitLayers(layer -> layer.setFrozen(frozen));
    return this;
  }

  @Override
  public List<double[]> state() {
    return getChildren().stream().filter(x -> !x.isFrozen()).flatMap(l -> l.state().stream()).distinct()
        .collect(Collectors.toList());
  }

  public void visitLayers(@Nonnull final Consumer<Layer> visitor) {
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

  public void visitNodes(@Nonnull final Consumer<DAGNode> visitor) {
    visitNodes(true, visitor);
  }

  public void visitNodes(boolean recurse, @Nonnull final Consumer<DAGNode> visitor) {
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

  @Nonnull
  public DAGNetwork scrambleCopy(final Map<String, String> replacements) {
    return rewriteJson(getReplacementOperator(populateScrambleMap(replacements)));
  }

  @Nonnull
  public DAGNetwork rewriteJson(final UnaryOperator<String> fn) {
    assertAlive();
    @Nonnull
    HashMap<CharSequence, byte[]> resources = new HashMap<>();
    JsonObject originalJson = getJson(resources, SerialPrecision.Float);
    String postFilter = fn.apply(originalJson.toString());
    JsonObject replacedJson = new GsonBuilder().create().fromJson(postFilter, JsonObject.class).getAsJsonObject();
    return (DAGNetwork) Layer.fromJson(replacedJson, resources);
  }

  public Map<String, String> populateScrambleMap(final Map<String, String> replacements) {
    //logKeys();
    assert replacements.isEmpty();
    for (final String id : keys()) {
      replacements.put(id, UUID.randomUUID().toString());
    }
    return replacements;
  }

  public void logKeys() {
    internalNodes.forEach((id, node) -> {
      log.info(String.format("%s : Node[%s]", id, node.getLayer()));
    });
    getLayersById().forEach((id, layer) -> {
      log.info(String.format("%s : %s", id, layer));
    });
  }

  public Set<String> keys() {
    return Stream
        .concat(Stream.of(getId()), Stream.concat(getLayersById().keySet().stream(), internalNodes.keySet().stream()))
        .map(Object::toString).distinct().collect(Collectors.toSet());
  }

  public Map<UUID, Layer> getLayersById() {
    LinkedHashMap<UUID, Layer> map = new LinkedHashMap<>();
    visitLayers(layer -> {
      UUID id = layer.getId();
      Layer previous = map.put(id, layer);
      if (null != previous && previous != layer)
        throw new RuntimeException(String.format("Duplicated key found: %s (%s)", previous, id));
    });
    return Collections.unmodifiableMap(map);
  }

  public UUID getHeadId() {
    DAGNode head = getHead();
    return head.getId();
  }
}
