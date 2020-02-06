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

@SuppressWarnings("serial")
public final class LoggingWrapperLayer extends WrapperLayer {
  static final Logger log = LoggerFactory.getLogger(LoggingWrapperLayer.class);

  protected LoggingWrapperLayer(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    super(json, rs);
  }

  public LoggingWrapperLayer(final Layer inner) {
    super(inner);
  }

  @Nonnull
  @SuppressWarnings("unused")
  public static LoggingWrapperLayer fromJson(@Nonnull final JsonObject json, Map<CharSequence, byte[]> rs) {
    return new LoggingWrapperLayer(json, rs);
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... inObj) {
    final LoggingWrapperLayer loggingWrapperLayer = this.addRef();
    final Result[] wrappedInput = RefIntStream.range(0, inObj.length)
        .mapToObj(RefUtil.wrapInterface((IntFunction<Result>) i -> {
          final Result inputToWrap = inObj[i].addRef();
          try {
            Result.Accumulator accumulator = new Result.Accumulator() {
              {
                loggingWrapperLayer.addRef();
                inputToWrap.addRef();
              }
              @Override
              public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
                @Nonnull final String formatted = RefUtil.get(data.stream().map(loggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b));
                log.info(RefString.format("Feedback Output %s for key %s: \n\t%s", i, inner.getName(),
                    formatted.replaceAll("\n", "\n\t")));
                inputToWrap.accumulate(buffer, data);
              }

              public @SuppressWarnings("unused")
              void _free() {
                super._free();
                loggingWrapperLayer.freeRef();
                inputToWrap.freeRef();
              }
            };
            return new Result(inputToWrap.getData(), accumulator) {

              {
                inputToWrap.addRef();
              }

              @Override
              public boolean isAlive() {
                return inputToWrap.isAlive();
              }

              @Override
              public void _free() {
                inputToWrap.freeRef();
                super._free();
              }
            };
          } finally {
            inputToWrap.freeRef();
          }
        }, loggingWrapperLayer.addRef(), RefUtil.addRefs(inObj)))
        .toArray(Result[]::new);
    for (int i = 0; i < inObj.length; i++) {
      final TensorList tensorList = inObj[i].getData();
      @Nonnull final String formatted = RefUtil.get(
          tensorList.stream().map(loggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b)
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
      @Nonnull final String formatted = RefUtil.get(tensorList.stream().map(loggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b));
      tensorList.freeRef();
      log.info(RefString.format("Output for key %s: \n\t%s", inner.getName(), formatted.replaceAll("\n", "\n\t")));
    }
    try {
      Result.Accumulator accumulator = new Result.Accumulator() {
        {
          loggingWrapperLayer.addRef();
          output.addRef();
        }

        @Override
        public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
          @Nonnull final String formatted = RefUtil.get(data.stream().map(tensor -> {
            return loggingWrapperLayer.getString(tensor);
          }).reduce((a, b) -> a + "\n" + b));
          log.info(RefString.format("Feedback Input for key %s: \n\t%s", inner.getName(),
              formatted.replaceAll("\n", "\n\t")));
          output.accumulate(buffer, data);
        }

        public @SuppressWarnings("unused")
        void _free() {
          super._free();
          output.freeRef();
          loggingWrapperLayer.freeRef();
        }
      };
      loggingWrapperLayer.freeRef();
      return new Result(output.getData(), accumulator) {
        {
          output.addRef();
        }

        @Override
        public boolean isAlive() {
          return output.isAlive();
        }
        @Override
        public void _free() {
          output.freeRef();
          super._free();
        }
      };
    } finally {
      output.freeRef();
    }
  }

  @Nonnull
  public String getString(@Nonnull Tensor tensor) {
    try {
      return RefArrays.toString(tensor.getDimensions());
    } finally {
      tensor.freeRef();
    }
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

}
