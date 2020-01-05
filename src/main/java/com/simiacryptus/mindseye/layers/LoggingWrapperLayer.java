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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("serial")
public final @RefAware
class LoggingWrapperLayer extends WrapperLayer {
  static final Logger log = LoggerFactory.getLogger(LoggingWrapperLayer.class);

  protected LoggingWrapperLayer(@Nonnull final JsonObject json,
                                Map<CharSequence, byte[]> rs) {
    super(json, rs);
  }

  public LoggingWrapperLayer(final Layer inner) {
    super(inner);
  }

  @SuppressWarnings("unused")
  public static LoggingWrapperLayer fromJson(@Nonnull final JsonObject json,
                                             Map<CharSequence, byte[]> rs) {
    return new LoggingWrapperLayer(json, rs);
  }

  public static @SuppressWarnings("unused")
  LoggingWrapperLayer[] addRefs(LoggingWrapperLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LoggingWrapperLayer::addRef)
        .toArray((x) -> new LoggingWrapperLayer[x]);
  }

  public static @SuppressWarnings("unused")
  LoggingWrapperLayer[][] addRefs(LoggingWrapperLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LoggingWrapperLayer::addRefs)
        .toArray((x) -> new LoggingWrapperLayer[x][]);
  }

  @Override
  public Result eval(@Nonnull final Result... inObj) {
    final LoggingWrapperLayer loggingWrapperLayer = this;
    final Result[] wrappedInput = RefIntStream.range(0, inObj.length).mapToObj(i -> {
      final Result inputToWrap = inObj[i];
      return new Result(inputToWrap.getData(),
          new Result.Accumulator() {
            @Override
            public void accept(DeltaSet<UUID> buffer, TensorList data) {
              @Nonnull final String formatted = data.stream().map(loggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b).get();
              log.info(String.format("Feedback Output %s for key %s: \n\t%s", i, LoggingWrapperLayer.this.getInner().getName(),
                  formatted.replaceAll("\n", "\n\t")));
              inputToWrap.accumulate(buffer, data);
            }
          }) {

        @Override
        public boolean isAlive() {
          return inputToWrap.isAlive();
        }

        public void _free() {
        }
      };
    }).toArray(i -> new Result[i]);
    for (int i = 0; i < inObj.length; i++) {
      final TensorList tensorList = inObj[i].getData();
      @Nonnull final String formatted = tensorList.stream().map(loggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b).get();
      log.info(
          String.format("Input %s for key %s: \n\t%s", i, getInner().getName(), formatted.replaceAll("\n", "\n\t")));
    }
    @Nullable final Result output = getInner().eval(wrappedInput);
    {
      final TensorList tensorList = output.getData();
      @Nonnull final String formatted = tensorList.stream().map(x -> {
        return getString(x);
      }).reduce((a, b) -> a + "\n" + b).get();
      log.info(String.format("Output for key %s: \n\t%s", getInner().getName(), formatted.replaceAll("\n", "\n\t")));
    }
    return new Result(output.getData(), new Result.Accumulator() {
      @Override
      public void accept(DeltaSet<UUID> buffer, TensorList data) {
        @Nonnull final String formatted = data.stream().map(x -> {
          return LoggingWrapperLayer.this.getString(x);
        }).reduce((a, b) -> a + "\n" + b).get();
        log.info(
            String.format("Feedback Input for key %s: \n\t%s", LoggingWrapperLayer.this.getInner().getName(), formatted.replaceAll("\n", "\n\t")));
        output.accumulate(buffer, data);
      }
    }) {

      @Override
      public boolean isAlive() {
        return output.isAlive();
      }

      public void _free() {
      }
    };
  }

  public String getString(Tensor x) {
    return RefArrays.toString(x.getDimensions());
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  public @Override
  @SuppressWarnings("unused")
  LoggingWrapperLayer addRef() {
    return (LoggingWrapperLayer) super.addRef();
  }

}
