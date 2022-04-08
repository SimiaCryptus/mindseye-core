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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is the TrainableDataMask interface.
 *
 * @docgenVersion 9
 */
public interface TrainableDataMask extends Trainable {
  /**
   * @return an array of booleans that may be null
   * @docgenVersion 9
   */
  @Nullable
  boolean[] getMask();


  /**
   * Sets the mask.
   *
   * @param mask the mask
   * @docgenVersion 9
   */
  void setMask(boolean... mask);

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  void _free();

  /**
   * @return a new reference to this TrainableDataMask
   * @docgenVersion 9
   */
  @Nonnull
  TrainableDataMask addRef();
}
