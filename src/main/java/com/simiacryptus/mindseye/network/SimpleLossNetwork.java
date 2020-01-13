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
import com.simiacryptus.ref.lang.RefAware;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

@SuppressWarnings("serial")
public class SimpleLossNetwork extends SupervisedNetwork {

  @Nullable
  public final DAGNode lossNode;
  @Nullable
  public final DAGNode studentNode;

  public SimpleLossNetwork(@Nonnull final Layer student, @Nonnull final Layer loss) {
    super(2);
    DAGNode temp_24_0001 = add(student == null ? null : student.addRef(), getInput(0));
    studentNode = temp_24_0001 == null ? null : temp_24_0001.addRef();
    if (null != temp_24_0001)
      temp_24_0001.freeRef();
    student.freeRef();
    DAGNode temp_24_0002 = add(loss == null ? null : loss.addRef(), studentNode == null ? null : studentNode.addRef(),
        getInput(1));
    lossNode = temp_24_0002 == null ? null : temp_24_0002.addRef();
    if (null != temp_24_0002)
      temp_24_0002.freeRef();
    loss.freeRef();
  }

  @Override
  public DAGNode getHead() {
    return lossNode == null ? null : lossNode.addRef();
  }

  public static @SuppressWarnings("unused") SimpleLossNetwork[] addRefs(SimpleLossNetwork[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SimpleLossNetwork::addRef)
        .toArray((x) -> new SimpleLossNetwork[x]);
  }

  public static @SuppressWarnings("unused") SimpleLossNetwork[][] addRefs(SimpleLossNetwork[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SimpleLossNetwork::addRefs)
        .toArray((x) -> new SimpleLossNetwork[x][]);
  }

  public @SuppressWarnings("unused") void _free() {
    if (null != studentNode)
      studentNode.freeRef();
    if (null != lossNode)
      lossNode.freeRef();
  }

  public @Override @SuppressWarnings("unused") SimpleLossNetwork addRef() {
    return (SimpleLossNetwork) super.addRef();
  }
}
