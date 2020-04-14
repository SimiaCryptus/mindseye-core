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

import com.google.gson.JsonObject;
import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * The type Logging wrapper layer.
 */
@SuppressWarnings("serial")
public final class LoggingWrapperLayer extends WrapperLayer {
  /**
   * The Log.
   */
  static final Logger log = LoggerFactory.getLogger(LoggingWrapperLayer.class);

  /**
   * Instantiates a new Logging wrapper layer.
   *
   * @param json the json
   * @param rs   the rs
   */
  protected LoggingWrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
  }

  /**
   * Instantiates a new Logging wrapper layer.
   *
   * @param inner the inner
   */
  public LoggingWrapperLayer(final Layer inner) {
    super(inner);
  }

  /**
   * From json logging wrapper layer.
   *
   * @param json the json
   * @param rs   the rs
   * @return the logging wrapper layer
   */
  @Nonnull
  @SuppressWarnings("unused")
  public static LoggingWrapperLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new LoggingWrapperLayer(json, rs);
  }

  /**
   * Gets string.
   *
   * @param tensor the tensor
   * @return the string
   */
  @Nonnull
  public static String getString(@Nonnull Tensor tensor) {
    try {
      return RefArrays.toString(tensor.getDimensions());
    } finally {
      tensor.freeRef();
    }
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... inObj) {
    final LoggingWrapperLayer loggingWrapperLayer = this.addRef();
    final Result[] wrappedInput = RefIntStream.range(0, inObj.length)
        .mapToObj(RefUtil.wrapInterface((IntFunction<Result>) i -> {
          final Result inputToWrap = inObj[i].addRef();
          boolean alive = inputToWrap.isAlive();
          TensorList data = inputToWrap.getData();
          Result.Accumulator accumulator = new Accumulator2(inner.addRef(), i, inputToWrap.getAccumulator());
          inputToWrap.freeRef();
          return new Result(data, accumulator, alive);
        }, loggingWrapperLayer.addRef(), RefUtil.addRef(inObj)))
        .toArray(Result[]::new);
    for (int i = 0; i < inObj.length; i++) {
      final TensorList tensorList = inObj[i].getData();
      @Nonnull final String formatted = RefUtil.get(
          tensorList.stream().map(LoggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b)
      );
      tensorList.freeRef();
      log.info(RefString.format("Input %s for key %s: \n\t%s", i, inner.getName(),
          formatted.replaceAll("\n", "\n\t")));
    }
    RefUtil.freeRef(inObj);
    @Nullable final Result output = inner.eval(wrappedInput);
    {
      assert output != null;
      final TensorList tensorList = output.getData();
      @Nonnull final String formatted = RefUtil.get(tensorList.stream().map(LoggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b));
      tensorList.freeRef();
      log.info(RefString.format("Output for key %s: \n\t%s", inner.getName(), formatted.replaceAll("\n", "\n\t")));
    }
    boolean alive = output.isAlive();
    TensorList data = output.getData();
    Result.Accumulator accumulator = new Accumulator(inner.addRef(), output.getAccumulator());
    output.freeRef();
    loggingWrapperLayer.freeRef();
    return new Result(data, accumulator, alive);
  }

  public @SuppressWarnings("unused")
  void _free() {
    super._free();
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LoggingWrapperLayer addRef() {
    return (LoggingWrapperLayer) super.addRef();
  }

  private static class Accumulator extends Result.Accumulator {

    private Layer inner;
    private Result.Accumulator accumulator;

    /**
     * Instantiates a new Accumulator.
     *
     * @param inner       the inner
     * @param accumulator the accumulator
     */
    public Accumulator(Layer inner, Result.Accumulator accumulator) {
      this.inner = inner;
      this.accumulator = accumulator;
    }

    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      @Nonnull final String formatted = RefUtil.get(data.stream().map(tensor -> {
        return getString(tensor);
      }).reduce((a, b) -> a + "\n" + b));
      log.info(RefString.format("Feedback Input for key %s: \n\t%s", inner.getName(),
          formatted.replaceAll("\n", "\n\t")));
      Result.Accumulator accumulator = this.accumulator;
      try {
        accumulator.accept(buffer, data);
      } finally {
        accumulator.freeRef();
      }
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      accumulator.freeRef();
      inner.freeRef();
    }
  }

  private static class Accumulator2 extends Result.Accumulator {

    private final int i;
    private Layer inner;
    private Result.Accumulator accumulator;

    /**
     * Instantiates a new Accumulator 2.
     *
     * @param inner       the inner
     * @param i           the
     * @param accumulator the accumulator
     */
    public Accumulator2(Layer inner, int i, Result.Accumulator accumulator) {
      this.i = i;
      this.inner = inner;
      this.accumulator = accumulator;
    }

    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      @Nonnull final String formatted = RefUtil.get(data.stream().map(LoggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b));
      log.info(RefString.format("Feedback Output %s for key %s: \n\t%s", i, inner.getName(),
          formatted.replaceAll("\n", "\n\t")));
      Result.Accumulator accumulator = this.accumulator;
      try {
        accumulator.accept(buffer, data);
      } finally {
        accumulator.freeRef();
      }
    }

    public @SuppressWarnings("unused")
    void _free() {
      super._free();
      accumulator.freeRef();
      inner.freeRef();
    }
  }
}
