/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oharastream.ohara.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oharastream.ohara.common.data.Pair;
import oharastream.ohara.common.rule.OharaTest;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamUtils extends OharaTest {

  private final List<String> names = Arrays.asList("a", "b", "c");

  @Test
  public void testIterate() {
    Stream<String> stream = StreamUtils.iterate(names.iterator());
    Assert.assertEquals("a,b,c", stream.collect(Collectors.joining(",")));
    stream = StreamUtils.iterate(names.iterator());
    Assert.assertEquals(3, stream.collect(Collectors.toList()).size());
  }

  @Test
  public void testZipWithIndex() {
    Map<Integer, String> namesWithIndex =
        StreamUtils.zipWithIndex(names.stream()).collect(Collectors.toMap(Pair::left, Pair::right));
    Assert.assertEquals(3, namesWithIndex.size());
    Assert.assertEquals("a", namesWithIndex.get(0));
    Assert.assertEquals("b", namesWithIndex.get(1));
    Assert.assertEquals("c", namesWithIndex.get(2));
  }

  @Test
  public void testMapWithIndex() {
    List<Pair<Integer, String>> namesWithIndex =
        StreamUtils.mapWithIndex(names.stream(), (index, name) -> Pair.of(index, name))
            .collect(Collectors.toList());
    Assert.assertEquals(Pair.of(0, "a"), namesWithIndex.get(0));
    Assert.assertEquals(Pair.of(1, "b"), namesWithIndex.get(1));
    Assert.assertEquals(Pair.of(2, "c"), namesWithIndex.get(2));
  }
}
