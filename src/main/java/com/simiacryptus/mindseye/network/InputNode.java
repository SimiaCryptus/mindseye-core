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

import com.simiacryptus.mindseye.lang.Layer;
import com.simiacryptus.mindseye.lang.Result;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The type Input node.
 */
@SuppressWarnings("serial")
final class InputNode extends LazyResult {
//
//  InputNode() {
//    this(null);
//  }

  /**
   * Instantiates a new Input node.
   *
   * @param key the key
   */
  public InputNode(final UUID key) {
    super(key);
  }

  @Nullable
  @Override
  public <T extends Layer> T getLayer() {
    assertAlive();
    return null;
  }

  @Override
  public void setLayer(@Nullable final Layer layer) {
    if (null != layer)
      layer.freeRef();
    throw new IllegalStateException();
  }

//  @NotNull
//  public DAGNode add(@Nonnull final Layer nextHead) {
//    InnerNode temp_10_0002 = dagNetwork.add(nextHead, InputNode.this.addRef());
//    return temp_10_0002;
//  }

  public void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  InputNode addRef() {
    return (InputNode) super.addRef();
  }

  @Override
  protected Result eval(@Nonnull final GraphEvaluationContext context) {
    assertAlive();
    CountingResult countingResult = context.get(id);
    context.freeRef();
    return countingResult;
  }

}
