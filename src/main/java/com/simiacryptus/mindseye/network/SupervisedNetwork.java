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

/**
 * The type Supervised network.
 */
@SuppressWarnings("serial")
public abstract class SupervisedNetwork extends DAGNetwork {
  /**
   * Instantiates a new Supervised network.
   *
   * @param inputs the inputs
   */
  public SupervisedNetwork(final int inputs) {
    super(inputs);
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  SupervisedNetwork addRef() {
    return (SupervisedNetwork) super.addRef();
  }
}
