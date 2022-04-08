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

package com.simiacryptus.mindseye.eval;

import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.ref.wrappers.RefList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is the DataTrainable interface.
 *
 * @docgenVersion 9
 */
public interface DataTrainable extends Trainable {
  /**
   * @return the data as a 2D array of Tensors, or null if there is no data
   * @docgenVersion 9
   */
  @Nullable
  Tensor[][] getData();

  /**
   * Sets the data for this object.
   *
   * @param tensors the data to set
   * @docgenVersion 9
   */
  void setData(RefList<Tensor[]> tensors);

  /**
   * Adds a reference to the DataTrainable.
   *
   * @return the DataTrainable
   * @docgenVersion 9
   */
  @Nonnull
  DataTrainable addRef();
}
