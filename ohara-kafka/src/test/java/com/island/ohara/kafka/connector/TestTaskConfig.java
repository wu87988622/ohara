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

package com.island.ohara.kafka.connector;

import com.google.common.collect.ImmutableMap;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.json.PropGroups;
import com.island.ohara.kafka.connector.json.StringList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestTaskConfig extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullInput() {
    TaskConfig.of(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyInput() {
    TaskConfig.of(Collections.emptyMap());
  }

  @Test
  public void nullKey() {
    Map<String, String> map = Collections.singletonMap(null, CommonUtils.randomString());
    assertException(NullPointerException.class, () -> TaskConfig.of(map));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    TaskConfig.of(Collections.singletonMap("", CommonUtils.randomString()));
  }

  @Test
  public void nullValue() {
    Map<String, String> map = Collections.singletonMap(CommonUtils.randomString(), null);
    assertException(NullPointerException.class, () -> TaskConfig.of(map));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyValue() {
    TaskConfig.of(Collections.singletonMap(CommonUtils.randomString(), ""));
  }

  @Test
  public void testParseBoolean() {
    String key = CommonUtils.randomString();
    boolean value = true;
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.booleanValue(key));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseNonBoolean() {
    String key = CommonUtils.randomString();
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, CommonUtils.randomString()));
    config.booleanValue(key);
  }

  @Test
  public void testParseShort() {
    String key = CommonUtils.randomString();
    short value = 123;
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.shortValue(key));
  }

  @Test
  public void testParseInt() {
    String key = CommonUtils.randomString();
    int value = 123;
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.intValue(key));
  }

  @Test
  public void testParseLong() {
    String key = CommonUtils.randomString();
    long value = 123;
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.longValue(key));
  }

  @Test
  public void testParseDouble() {
    String key = CommonUtils.randomString();
    double value = 123.333;
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.doubleValue(key), 0);
  }

  @Test
  public void testParseStrings() {
    String key = CommonUtils.randomString();
    List<String> ss = Arrays.asList(CommonUtils.randomString(), CommonUtils.randomString());
    TaskConfig config = TaskConfig.of(Collections.singletonMap(key, StringList.toKafkaString(ss)));
    List<String> ss2 = config.stringList(key);
    Assert.assertEquals(ss.size(), ss2.size());
    ss.forEach(s -> Assert.assertEquals(1, ss2.stream().filter(s::equals).count()));
  }

  @Test
  public void testToPropGroups() {
    PropGroups propGroups =
        PropGroups.of(
            Arrays.asList(
                ImmutableMap.of("k0", "v0", "k1", "v1", "k2", "v2"),
                ImmutableMap.of("k0", "v0", "k1", "v1")));
    TaskConfig config = TaskConfig.of(ImmutableMap.of("pgs", propGroups.toJsonString()));
    PropGroups another = config.propGroups("pgs");
    Assert.assertEquals(propGroups, another);
  }

  @Test
  public void getEmptyColumn() {
    TaskConfig config = TaskConfig.of(ImmutableMap.of("pgs", "asdasd"));
    Assert.assertTrue(config.columns().isEmpty());
  }
}
