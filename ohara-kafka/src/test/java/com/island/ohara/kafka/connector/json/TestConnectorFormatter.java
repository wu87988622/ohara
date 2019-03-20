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

package com.island.ohara.kafka.connector.json;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorFormatter extends SmallTest {

  @Test
  public void stringListShouldInKafkaFormat() {
    List<String> topicNames = Collections.singletonList(CommonUtils.randomString());
    Creation creation =
        ConnectorFormatter.of()
            .name(CommonUtils.randomString())
            .topicsNames(topicNames)
            .requestOfCreation();
    Assert.assertNotNull(creation.configs().get(ConnectorFormatter.TOPIC_NAMES_KEY));
    Assert.assertEquals(
        StringList.toKafkaString(topicNames),
        creation.configs().entrySet().iterator().next().getValue());
  }

  @Test
  public void stringListShouldInKafkaFormat2() {
    List<String> topicNames = Arrays.asList(CommonUtils.randomString(), CommonUtils.randomString());
    Creation creation =
        ConnectorFormatter.of()
            .name(CommonUtils.randomString())
            .topicsNames(topicNames)
            .requestOfCreation();
    Assert.assertNotNull(creation.configs().get(ConnectorFormatter.TOPIC_NAMES_KEY));
    Assert.assertEquals(
        StringList.toKafkaString(topicNames),
        creation.configs().entrySet().iterator().next().getValue());
  }

  @Test
  public void configsNameShouldBeRemoved() {
    Creation creation =
        ConnectorFormatter.of()
            .name(CommonUtils.randomString())
            .topicsName(CommonUtils.randomString())
            .requestOfCreation();
    Assert.assertNull(creation.configs().get(ConnectorFormatter.NAME_KEY));
  }

  @Test
  public void testSetKeyConverter() {
    Creation creation =
        ConnectorFormatter.of()
            .name(CommonUtils.randomString())
            .converterTypeOfKey(ConverterType.JSON)
            .converterTypeOfValue(ConverterType.JSON)
            .requestOfCreation();
    Assert.assertTrue(creation.toJsonString().contains(ConverterType.JSON.className()));
    Assert.assertFalse(creation.toJsonString().contains(ConverterType.NONE.className()));
  }

  @Test
  public void testStringList() {
    List<String> topicNames = Arrays.asList(CommonUtils.randomString(), CommonUtils.randomString());
    Creation creation =
        ConnectorFormatter.of()
            .name(CommonUtils.randomString())
            .topicsNames(topicNames)
            .requestOfCreation();
    Assert.assertEquals(
        StringList.toKafkaString(topicNames),
        creation.configs().get(ConnectorFormatter.TOPIC_NAMES_KEY));
  }

  @Test(expected = NullPointerException.class)
  public void ignoreName() {
    ConnectorFormatter.of().requestOfCreation();
  }

  @Test(expected = NullPointerException.class)
  public void nullName() {
    ConnectorFormatter.of().name(null).requestOfCreation();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyName() {
    ConnectorFormatter.of().name("").requestOfCreation();
  }
}
