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

package com.simiacryptus.mindseye.lang;

import com.simiacryptus.lang.Settings;
import com.simiacryptus.ref.wrappers.RefString;
import com.simiacryptus.ref.wrappers.RefSystem;
import com.simiacryptus.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.simiacryptus.lang.Settings.get;

/**
 * This class contains the settings for the Core class.
 *
 * @docgenVersion 9
 */
public class CoreSettings implements Settings {

  private static final Logger logger = LoggerFactory.getLogger(CoreSettings.class);
  @Nullable
  private static transient CoreSettings INSTANCE = null;
  /**
   * The Backprop aggregation size.
   */
  public final int backpropAggregationSize = get("BACKPROP_AGG_SIZE", 3);
  /**
   * The Jvm threads.
   */
  public final int jvmThreads = get("THREADS", 64);
  /**
   * The Single threaded.
   */
  public final boolean singleThreaded = get("SINGLE_THREADED", false);

  private CoreSettings() {
    RefSystem.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
        Integer.toString(jvmThreads));
  }

  /**
   * Returns the singleton instance of CoreSettings.
   * If the instance does not exist, it is created and initialized.
   *
   * @docgenVersion 9
   */
  public static CoreSettings INSTANCE() {
    if (null == INSTANCE) {
      synchronized (CoreSettings.class) {
        if (null == INSTANCE) {
          INSTANCE = new CoreSettings();
          logger.info(
              RefString.format("Initialized %s = %s", INSTANCE.getClass().getSimpleName(), JsonUtil.toJson(INSTANCE)));
        }
      }
    }
    return INSTANCE;
  }

}
