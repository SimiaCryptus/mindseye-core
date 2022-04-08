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

package com.simiacryptus.mindseye.network;

import com.simiacryptus.mindseye.lang.*;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.wrappers.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for counting results.
 *
 * @docgenVersion 9
 */
public class CountingResult extends Result {
  /**
   * The constant logger.
   */
  protected static final Logger logger = LoggerFactory.getLogger(CountingResult.class);

  /**
   * Instantiates a new Counting result.
   *
   * @param inner the inner
   */
  public CountingResult(@Nonnull final Result inner) {
    super(inner.getData(), new CountingAccumulator(inner.getAccumulator()), inner.isAlive());
    inner.freeRef();
  }

  /**
   * Instantiates a new Counting result.
   *
   * @param inner    the inner
   * @param samples  the samples
   * @param consumer the consumer
   */
  public CountingResult(@Nonnull final Result inner, final int samples, Layer consumer) {
    this(inner);
    Accumulator a = getAccumulator();
    if (a instanceof CountingResult.CountingAccumulator) {
      CountingResult.CountingAccumulator accumulator = (CountingResult.CountingAccumulator) a;
      for (int i = 0; i < samples; i++) {
        accumulator.incrementFwd(consumer.addRef());
      }
      accumulator.freeRef();
    } else {
      assert Result.isNull(a);
      a.freeRef();
    }
    consumer.freeRef();
  }

  /**
   * Returns an array of stack trace elements representing the stack trace
   * of this throwable.  The first element of the array (assuming the array
   * is non-null) represents the top of the stack, which is the last method
   * invocation in the sequence.  The bottom of the stack is represented by
   * the last element of the array.
   *
   * @docgenVersion 9
   */
  @NotNull
  private static StackTraceElement[] getStackTrace() {
    return new StackTraceElement[]{};
//    return Thread.currentThread().getStackTrace();
  }

  /**
   * Frees the memory associated with this object.
   *
   * @docgenVersion 9
   */
  public void _free() {
    super._free();
  }

  /**
   * Adds a reference to the CountingResult.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  @SuppressWarnings("unused")
  CountingResult addRef() {
    return (CountingResult) super.addRef();
  }

  /**
   * This class represents an accumulator that keeps track of counts.
   *
   * @param fwdLinks         A list of layers that the accumulator uses to keep track of counts.
   * @param passbackBuffers  A map of stack trace elements to tensor lists. This is used to keep track of counts for each stack trace element.
   * @param accumulations    A list of stack trace elements. This is used to keep track of which stack trace elements have been accumulated.
   * @param innerAccumulator The accumulator that this class wraps.
   * @docgenVersion 9
   */
  static class CountingAccumulator extends Result.Accumulator {
    @Nonnull
    private final RefList<Layer> fwdLinks;
    @Nonnull
    private final RefMap<StackTraceElement[], TensorList> passbackBuffers;
    @Nonnull
    private final List<StackTraceElement[]> accumulations;
    private Accumulator innerAccumulator;

    /**
     * Instantiates a new Counting accumulator.
     *
     * @param accumulator the accumulator
     */
    public CountingAccumulator(Accumulator accumulator) {
      innerAccumulator = accumulator;
      fwdLinks = new RefArrayList<>();
      passbackBuffers = new RefHashMap<>();
      accumulations = new ArrayList<>();
    }

    /**
     * Returns the number of times the forward button has been pressed.
     *
     * @docgenVersion 9
     */
    public int getFwdCount() {
      return this.fwdLinks.size();
    }

    /**
     * Increments the value of the variable and returns the new value.
     *
     * @docgenVersion 9
     */
    public int incrementFwd(Layer consumer) {
      synchronized (this.fwdLinks) {
        this.fwdLinks.add(consumer);
        return this.fwdLinks.size();
      }
    }

    /**
     * Accepts an input.
     *
     * @docgenVersion 9
     */
    @Override
    public void accept(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      assertAlive();
      data.assertAlive();
      if (1 >= getFwdCount()) {
        accum(buffer, data);
      } else {
        add(buffer, data);
      }
    }

    /**
     * This is the accumulator method.
     *
     * @docgenVersion 9
     */
    public void accum(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      innerAccumulator.accept(buffer, data);
    }

    /**
     * Frees the memory associated with this object.
     *
     * @docgenVersion 9
     */
    public void _free() {
      super._free();
      synchronized (passbackBuffers) {
        if (passbackBuffers.size() > 0 && accumulations.size() > 0) {
          logger.error("Passback incomplete");
        }
        passbackBuffers.freeRef();
      }
      fwdLinks.freeRef();
      if (null != innerAccumulator) innerAccumulator.freeRef();
    }

    /**
     * Adds a reference to the CountingAccumulator.
     *
     * @docgenVersion 9
     */
    @Nonnull
    public @Override
    @SuppressWarnings("unused")
    CountingAccumulator addRef() {
      return (CountingAccumulator) super.addRef();
    }

    /**
     * Adds an element to the end of the list.
     *
     * @docgenVersion 9
     */
    private void add(@Nullable DeltaSet<UUID> buffer, @Nonnull TensorList data) {
      //assert allAlive();
      @NotNull StackTraceElement[] stackTrace = getStackTrace();
      synchronized (passbackBuffers) {
        RefUtil.freeRef(passbackBuffers.put(stackTrace, data));
        if (passbackBuffers.size() > CoreSettings.INSTANCE().backpropAggregationSize) {
          RefUtil.freeRef(passbackBuffers.put(stackTrace, reduce()));
          //assert allAlive();
        }
      }
      int expected = getFwdCount();
      int accumulationCount;
      synchronized (accumulations) {
        accumulations.add(stackTrace);
        accumulationCount = accumulations.size() % expected;
      }
      if (accumulationCount == 0) {
        accum(buffer, reduce());
      } else {
        buffer.freeRef();
      }
    }

    /**
     * Returns true if all players are alive, false otherwise.
     *
     * @docgenVersion 9
     */
    private boolean allAlive() {
      synchronized (passbackBuffers) {
        passbackBuffers.forEach((k, v) -> {
          v.assertAlive();
          v.freeRef();
        });
        return true;
      }
    }

    /**
     * Reduces the TensorList to a single Tensor.
     *
     * @docgenVersion 9
     */
    @NotNull
    private TensorList reduce() {
      synchronized (passbackBuffers) {
        RefCollection<TensorList> values = passbackBuffers.values();
        RefStream<TensorList> stream = values.stream();
        if (!CoreSettings.INSTANCE().singleThreaded)
          stream = stream.parallel();
        TensorList reduced = RefUtil.get(stream.reduce((a, b) -> {
          TensorList c = a.addAndFree(b);
          a.freeRef();
          return c;
        }));
        values.freeRef();
        passbackBuffers.clear();
        return reduced;
      }
    }

  }
}
