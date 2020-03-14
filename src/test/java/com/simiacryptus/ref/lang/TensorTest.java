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

package com.simiacryptus.ref.lang;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.ref.wrappers.*;
import com.simiacryptus.util.test.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TensorTest {
  private static final Logger log = LoggerFactory.getLogger(TensorTest.class);

  public void parse(final String str) {
    final JsonElement json = new GsonBuilder().create().fromJson(str, JsonElement.class);
    @Nullable final Tensor tensor = Tensor.fromJson(json, null);
    assert tensor != null;
    RefAssert.assertEquals(json, tensor.getJson(null, Tensor.json_precision));
    tensor.freeRef();
  }

  public void test(@Nonnull final Tensor t) {
    @Nonnull final JsonElement json = t.getJson(null, Tensor.json_precision);
    RefAssert.assertEquals(Tensor.fromJson(json, null), t);
    parse(json.toString());
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testCoordStream() {
    Tensor temp_52_0001 = new Tensor(2, 2, 2);
    final RefList<CharSequence> coordinates = temp_52_0001.coordStream(true)
        .map(c -> RefString.format("%s - %s", c.getIndex(), RefArrays.toString(c.getCoords())))
        .collect(RefCollectors.toList());
    temp_52_0001.freeRef();
    coordinates.forEach(msg -> log.info(msg.toString()));
    coordinates.freeRef();
  }
  //
  //  /**
  //   * Test shuffle stream.
  //   *
  //   * @throws Exception the exception
  //   */
  //  @Test
  //  @Category(TestCategories.UnitTest.class)
  //  public void testShuffleStream() {
  //    @Nonnull HashSet<Object> ids = new HashSet<>();
  //    int max = 10000;
  //    TestUtil.shuffle(IntStream.range(0, max)).forEach((int i) -> {
  //      if (i >= 0 && i >= max) throw new AssertionError(i);
  //      if (!ids.add(i)) throw new AssertionError(i);
  //    });
  //  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testToJson() {
    Tensor temp_52_0002 = new Tensor(3, 3, 1);
    test(temp_52_0002.map(v -> Math.random()));
    temp_52_0002.freeRef();
    Tensor temp_52_0003 = new Tensor(1, 3, 3);
    test(temp_52_0003.map(v -> Math.random()));
    temp_52_0003.freeRef();
  }

}
