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
import com.simiacryptus.lang.ref.RefSettings;
import com.simiacryptus.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Cuda settings.
 */
public class CoreSettings extends RefSettings {

  private static final Logger logger = LoggerFactory.getLogger(CoreSettings.class);
  private static transient CoreSettings INSTANCE = null;
  public final int backpropAggregationSize;
  private final boolean singleThreaded;
  public final int jvmThreads;

  private CoreSettings() {
    this.jvmThreads = Settings.get("THREADS", 64);
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(jvmThreads));
    this.singleThreaded = Settings.get("SINGLE_THREADED", false);
    this.backpropAggregationSize = Settings.get("BACKPROP_AGG_SIZE", 3);
  }

  /**
   * The constant INSTANCE.
   *
   * @return the core settings
   */
  public static CoreSettings INSTANCE() {
    if(null==INSTANCE) {
      synchronized (CoreSettings.class) {
        if(null==INSTANCE) {
          INSTANCE = new CoreSettings();
          logger.info(String.format("Initialized %s = %s", INSTANCE.getClass().getSimpleName(), JsonUtil.toJson(INSTANCE)));
        }
      }
    }
    return INSTANCE;
  }

  /**
   * Is conservative boolean.
   *
   * @return the boolean
   */
  public boolean isSingleThreaded() {
    return singleThreaded;
  }

}
