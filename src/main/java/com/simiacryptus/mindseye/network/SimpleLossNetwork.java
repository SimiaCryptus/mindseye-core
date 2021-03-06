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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The type Simple loss network.
 */
@SuppressWarnings("serial")
public class SimpleLossNetwork extends SupervisedNetwork {

  /**
   * The Loss node.
   */
  @Nullable
  public final DAGNode lossNode;
  /**
   * The Student node.
   */
  @Nullable
  public final DAGNode studentNode;

  /**
   * Instantiates a new Simple loss network.
   *
   * @param student the student
   * @param loss    the loss
   */
  public SimpleLossNetwork(@Nonnull final Layer student, @Nonnull final Layer loss) {
    super(2);
    DAGNode temp_24_0001 = add(student.addRef(), getInput(0));
    studentNode = temp_24_0001.addRef();
    temp_24_0001.freeRef();
    student.freeRef();
    DAGNode temp_24_0002 = add(loss.addRef(), studentNode.addRef(),
        getInput(1));
    lossNode = temp_24_0002.addRef();
    temp_24_0002.freeRef();
    loss.freeRef();
  }

  @Override
  public DAGNode getHead() {
    return lossNode == null ? null : lossNode.addRef();
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
    if (null != studentNode)
      studentNode.freeRef();
    if (null != lossNode)
      lossNode.freeRef();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SimpleLossNetwork addRef() {
    return (SimpleLossNetwork) super.addRef();
  }
}
