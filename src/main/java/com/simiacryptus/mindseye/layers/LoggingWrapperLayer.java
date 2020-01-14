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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
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

  @Nullable
  public static @SuppressWarnings("unused")
  LoggingWrapperLayer[] addRefs(@Nullable LoggingWrapperLayer[] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LoggingWrapperLayer::addRef)
        .toArray((x) -> new LoggingWrapperLayer[x]);
  }

  @Nullable
  public static @SuppressWarnings("unused")
  LoggingWrapperLayer[][] addRefs(@Nullable LoggingWrapperLayer[][] array) {
    if (array == null)
      return null;
    return Arrays.stream(array).filter((x) -> x != null).map(LoggingWrapperLayer::addRefs)
        .toArray((x) -> new LoggingWrapperLayer[x][]);
  }

  @Nonnull
  @Override
  public Result eval(@Nonnull final Result... inObj) {
    final LoggingWrapperLayer loggingWrapperLayer = this.addRef();
    final Result[] wrappedInput = RefIntStream.range(0, inObj.length)
        .mapToObj(RefUtil.wrapInterface((IntFunction<Result>) i -> {
          final Result inputToWrap = inObj[i].addRef();
          try {
            return new Result(inputToWrap.getData(), new Result.Accumulator() {
              @Override
              public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
                @Nonnull final String formatted = RefUtil.get(data.stream().map(loggingWrapperLayer::getString)
                    .reduce((a, b) -> a + "\n" + b));
                Layer temp_46_0006 = LoggingWrapperLayer.this.getInner();
                assert temp_46_0006 != null;
                log.info(RefString.format("Feedback Output %s for key %s: \n\t%s", i, temp_46_0006.getName(),
                    formatted.replaceAll("\n", "\n\t")));
                temp_46_0006.freeRef();
                inputToWrap.accumulate(buffer == null ? null : buffer.addRef(), data.addRef());
                data.freeRef();
                if (null != buffer)
                  buffer.freeRef();
              }

              public @SuppressWarnings("unused")
              void _free() {
              }
            }) {

              @Override
              public boolean isAlive() {
                return inputToWrap.isAlive();
              }

              public void _free() {
              }
            };
          } finally {
            inputToWrap.freeRef();
          }
        }, loggingWrapperLayer.addRef(), Result.addRefs(inObj)))
        .toArray(i -> new Result[i]);
    for (int i = 0; i < inObj.length; i++) {
      final TensorList tensorList = inObj[i].getData();
      @Nonnull final String formatted = RefUtil.get(
          tensorList.stream().map(loggingWrapperLayer::getString).reduce((a, b) -> a + "\n" + b)
      );
      tensorList.freeRef();
      Layer temp_46_0007 = getInner();
      assert temp_46_0007 != null;
      log.info(RefString.format("Input %s for key %s: \n\t%s", i, temp_46_0007.getName(),
          formatted.replaceAll("\n", "\n\t")));
      temp_46_0007.freeRef();
    }
    ReferenceCounting.freeRefs(inObj);
    loggingWrapperLayer.freeRef();
    Layer temp_46_0008 = getInner();
    assert temp_46_0008 != null;
    @Nullable final Result output = temp_46_0008.eval(Result.addRefs(wrappedInput));
    temp_46_0008.freeRef();
    ReferenceCounting.freeRefs(wrappedInput);
    {
      assert output != null;
      final TensorList tensorList = output.getData();
      @Nonnull final String formatted = RefUtil.get(tensorList.stream().map(x -> {
        String temp_46_0003 = getString(x == null ? null : x.addRef());
        if (null != x)
          x.freeRef();
        return temp_46_0003;
      }).reduce((a, b) -> a + "\n" + b));
      tensorList.freeRef();
      Layer temp_46_0009 = getInner();
      log.info(
          RefString.format("Output for key %s: \n\t%s", temp_46_0009.getName(), formatted.replaceAll("\n", "\n\t")));
      temp_46_0009.freeRef();
    }
    try {
      return new Result(output.getData(), new Result.Accumulator() {
        {
        }

        @Override
        public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
          @Nonnull final String formatted = RefUtil.get(data.stream().map(x -> {
            String temp_46_0004 = LoggingWrapperLayer.this.getString(x == null ? null : x.addRef());
            if (null != x)
              x.freeRef();
            return temp_46_0004;
          }).reduce((a, b) -> a + "\n" + b));
          Layer temp_46_0010 = LoggingWrapperLayer.this.getInner();
          log.info(RefString.format("Feedback Input for key %s: \n\t%s", temp_46_0010.getName(),
              formatted.replaceAll("\n", "\n\t")));
          temp_46_0010.freeRef();
          output.accumulate(buffer == null ? null : buffer.addRef(), data.addRef());
          data.freeRef();
          if (null != buffer)
            buffer.freeRef();
        }

        public @SuppressWarnings("unused")
        void _free() {
        }
      }) {

        {
        }

        @Override
        public boolean isAlive() {
          return output.isAlive();
        }

        public void _free() {
        }
      };
    } finally {
      output.freeRef();
    }
  }

  @Nonnull
  public String getString(@Nonnull Tensor x) {
    String temp_46_0005 = RefArrays.toString(x.getDimensions());
    x.freeRef();
    return temp_46_0005;
  }

  public @SuppressWarnings("unused")
  void _free() {
  }

  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  LoggingWrapperLayer addRef() {
    return (LoggingWrapperLayer) super.addRef();
  }

}
