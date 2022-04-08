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

package com.simiacryptus.mindseye.opt;

import javax.annotation.Nullable;

/**
 * This class is responsible for monitoring training progress.
 *
 * @docgenVersion 9
 */
public class TrainingMonitor {
  /**
   * Clears the contents of this buffer.
   *
   * @docgenVersion 9
   */
  public void clear() {
  }

  /**
   * Logs the given message.
   *
   * @param msg the message to log
   * @docgenVersion 9
   */
  public void log(final String msg) {
  }

  /**
   * This method is invoked when a step is completed.
   *
   * @param currentPoint the current point in the step process; may be null
   * @docgenVersion 9
   */
  public void onStepComplete(@Nullable final Step currentPoint) {
    if (null != currentPoint)
      currentPoint.freeRef();
  }

  /**
   * This method is called when a step fails.
   *
   * @param currentPoint the current point in the step process
   * @return false
   * @docgenVersion 9
   */
  public boolean onStepFail(@Nullable final Step currentPoint) {
    if (null != currentPoint)
      currentPoint.freeRef();
    return false;
  }
}
