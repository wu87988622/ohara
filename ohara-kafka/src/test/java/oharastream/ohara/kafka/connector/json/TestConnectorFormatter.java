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

package oharastream.ohara.kafka.connector.json;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import oharastream.ohara.common.json.JsonUtils;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.ConnectorKey;
import oharastream.ohara.common.setting.TopicKey;
import oharastream.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorFormatter extends OharaTest {

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
    ConnectorFormatter.of().columns(List.of());
  }

  @Test
  public void stringListShouldInKafkaFormat() {
    Set<TopicKey> topicKeys =
        Sets.newHashSet(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()));
    Set<String> topicNames =
        topicKeys.stream().map(TopicKey::topicNameOnKafka).collect(Collectors.toUnmodifiableSet());
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
