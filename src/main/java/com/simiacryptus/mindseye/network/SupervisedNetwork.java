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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

@SuppressWarnings("serial")
public abstract class SupervisedNetwork extends DAGNetwork {
  public SupervisedNetwork(final int inputs) {
    super(inputs);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  SupervisedNetwork[] addRefs(@Nullable SupervisedNetwork[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SupervisedNetwork::addRef)
        .toArray((x) -> new SupervisedNetwork[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  SupervisedNetwork[][] addRefs(@Nullable SupervisedNetwork[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(SupervisedNetwork::addRefs)
        .toArray((x) -> new SupervisedNetwork[x][]);
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SupervisedNetwork addRef() {
    return (SupervisedNetwork) super.addRef();
  }
}
