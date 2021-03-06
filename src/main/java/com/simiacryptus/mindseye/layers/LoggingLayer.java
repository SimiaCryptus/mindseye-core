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

package com.simiacryptus.mindseye.layers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * The type Logging layer.
 */
@SuppressWarnings("serial")
public final class LoggingLayer extends LayerBase {
  /**
   * The Log.
   */
  static final Logger log = LoggerFactory.getLogger(LoggingLayer.class);
  private boolean logFeedback = true;
  private DetailLevel level;

  /**
   * Instantiates a new Logging layer.
   *
   * @param json the json
   */
  protected LoggingLayer(@Nonnull final JsonObject json) {
    super(json);
    level = DetailLevel.Dimensions;
    level = DetailLevel.valueOf(json.get("level").getAsString());
    logFeedback = json.get("logFeedback").getAsBoolean();
  }

  /**
   * Instantiates a new Logging layer.
   */
  public LoggingLayer() {
    this(DetailLevel.Dimensions);
  }

  /**
   * Instantiates a new Logging layer.
   *
   * @param level the level
   */
  public LoggingLayer(DetailLevel level) {
    this.level = level;
  }

  /**
   * Gets level.
   *
   * @return the level
   */
  public DetailLevel getLevel() {
    return level;
  }

  /**
   * Sets level.
   *
   * @param level the level
   */
  public void setLevel(DetailLevel level) {
    this.level = level;
  }

  /**
   * Is log feedback boolean.
   *
   * @return the boolean
   */
  public boolean isLogFeedback() {
    return logFeedback;
  }

  /**
   * Sets log feedback.
   *
   * @param logFeedback the log feedback
   */
  public void setLogFeedback(boolean logFeedback) {
    this.logFeedback = logFeedback;
  }

  /**
   * From json logging layer.
   *
   * @param json the json
   * @param rs   the rs
   * @return the logging layer
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static LoggingLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new LoggingLayer(json);
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... inObj) {
    if (1 != inObj.length) {
      RefUtil.freeRef(inObj);
      throw new IllegalArgumentException();
    }
    @Nullable final Result output = inObj[0].addRef();
    RefUtil.freeRef(inObj);
    {
      assert output != null;
      final TensorList tensorList = output.getData();
      @Nonnull final String formatted = RefUtil.get(tensorList.stream().map(tensor -> {
        return level.getString(tensor);
      }).reduce((a, b) -> a + "\n" + b)).replaceAll("\n", "\n\t");
      tensorList.freeRef();
      log.info(RefString.format("Output for %s: %s", getName(), formatted));
    }
    if (isLogFeedback()) {
      boolean alive = output.isAlive();
      TensorList data = output.getData();
      Result.Accumulator accumulator = new Accumulator(output.getAccumulator(), getName(), LoggingLayer.this.level);
      output.freeRef();
      return new Result(data, accumulator, alive);
    } else {
      return output;
    }
  }

  @Nullable
  @Override
  public RefList<double[]> state() {
    return new RefArrayList<>();
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LoggingLayer addRef() {
    return (LoggingLayer) super.addRef();
  }

  @Nullable
  @Override
  public JsonElement getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer) {
    JsonObject json = super.getJsonStub();
    json.addProperty("level", level.name());
    json.addProperty("logFeedback", logFeedback);
    return json;
  }

  /**
   * The enum Detail level.
   */
  public enum DetailLevel {
    /**
     * The Dimensions.
     */
    Dimensions {
      @Override
      public String getString(@Nonnull Tensor tensor) {
        try {
          return RefArrays.toString(tensor.getDimensions());
        } finally {
          tensor.freeRef();
        }
      }
    },
    /**
     * The Statistics.
     */
    Statistics {
      @Override
      public String getString(@Nonnull Tensor tensor) {
        try {
          return tensor.doubleStream().summaryStatistics().toString();
        } finally {
          tensor.freeRef();
        }
      }
    },
    /**
     * The Data.
     */
    Data {
      @Override
      public String getString(@Nonnull Tensor tensor) {
        try {
          return tensor.prettyPrint();
        } finally {
          tensor.freeRef();
        }
      }
    };

    /**
     * Gets string.
     *
     * @param tensor the tensor
     * @return the string
     */
    public abstract String getString(@Nonnull Tensor tensor);
  }

  private static class Accumulator extends Result.Accumulator {

    private Result.Accumulator accumulator;
    private String name;
    private DetailLevel level;

    /**
     * Instantiates a new Accumulator.
     *
     * @param accumulator the accumulator
     * @param name        the name
     * @param level       the level
     */
    public Accumulator(Result.Accumulator accumulator, String name, DetailLevel level) {
      this.accumulator = accumulator;
      this.name = name;
      this.level = level;
    }

    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      @Nonnull final String formatted = RefUtil.get(data.stream().map(tensor -> {
        return level.getString(tensor);
      }).reduce((a, b) -> a + "\n" + b)).replaceAll("\n", "\n\t");
      log.info(RefString.format("Feedback for %s: %s", name, formatted));
      this.accumulator.accept(buffer, data);
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      accumulator.freeRef();
    }
  }

}
