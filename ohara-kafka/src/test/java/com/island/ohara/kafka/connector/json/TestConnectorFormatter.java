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

import com.google.common.collect.Sets;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.setting.ConnectorKey;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorFormatter extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullColumn() {
    ConnectorFormatter.of().column(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullColumns() {
    ConnectorFormatter.of().columns(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyColumns() {
    ConnectorFormatter.of().columns(Collections.emptyList());
  }

  @Test
  public void stringListShouldInKafkaFormat() {
    Set<TopicKey> topicKeys =
        Sets.newHashSet(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()));
    Set<String> topicNames =
        topicKeys.stream().map(TopicKey::topicNameOnKafka).collect(Collectors.toSet());
    Creation creation =
        ConnectorFormatter.of()
            .connectorKey(ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
            .topicKeys(topicKeys)
            .requestOfCreation();
    Assert.assertNotNull(creation.configs().get(ConnectorDefUtils.TOPIC_NAMES_DEFINITION.key()));
    Assert.assertEquals(
        StringList.toKafkaString(topicNames),
        creation.configs().get(ConnectorDefUtils.TOPIC_NAMES_DEFINITION.key()));
    Assert.assertNotNull(creation.configs().get(ConnectorDefUtils.TOPIC_KEYS_DEFINITION.key()));
    Assert.assertEquals(
        JsonUtils.toString(topicKeys),
        creation.configs().get(ConnectorDefUtils.TOPIC_KEYS_DEFINITION.key()));
  }

  @Test
  public void configsNameShouldBeRemoved() {
    Creation creation =
        ConnectorFormatter.of()
            .connectorKey(ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
            .topicKey(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()))
            .requestOfCreation();
    Assert.assertNull(creation.configs().get(ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.key()));
  }

  @Test
  public void testSetKeyConverter() {
    Creation creation =
        ConnectorFormatter.of()
            .connectorKey(ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
            .converterTypeOfKey(ConverterType.JSON)
            .converterTypeOfValue(ConverterType.JSON)
            .requestOfCreation();
    Assert.assertTrue(creation.toJsonString().contains(ConverterType.JSON.className()));
    Assert.assertFalse(creation.toJsonString().contains(ConverterType.NONE.className()));
  }

  @Test(expected = NullPointerException.class)
  public void ignoreName() {
    ConnectorFormatter.of().requestOfCreation();
  }

  @Test(expected = NullPointerException.class)
  public void nullConnectorKey() {
    ConnectorFormatter.of().connectorKey(null).requestOfCreation();
  }

  @Test
  public void emptyListShouldDisappear() {
    ConnectorFormatter format = ConnectorFormatter.of();
    int initialSize = format.settings.size();
    format.setting("a", "[]");
    Assert.assertEquals(initialSize, format.settings.size());
  }
}
