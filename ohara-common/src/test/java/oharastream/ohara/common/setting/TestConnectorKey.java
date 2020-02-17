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

package oharastream.ohara.common.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import oharastream.ohara.common.json.JsonUtils;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorKey extends OharaTest {

  @Test
  public void testEqual() throws IOException {
    ConnectorKey key = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5));
    ObjectMapper mapper = JsonUtils.objectMapper();
    Assert.assertEquals(
        key, mapper.readValue(mapper.writeValueAsString(key), new TypeReference<KeyImpl>() {}));
  }

  @Test
  public void testGetter() {
    String group = CommonUtils.randomString(5);
    String name = CommonUtils.randomString(5);
    ConnectorKey key = ConnectorKey.of(group, name);
    Assert.assertEquals(group, key.group());
    Assert.assertEquals(name, key.name());
  }

  @Test(expected = NullPointerException.class)
  public void nullGroup() {
    ConnectorKey.of(null, CommonUtils.randomString(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyGroup() {
    ConnectorKey.of("", CommonUtils.randomString(5));
  }

  @Test(expected = NullPointerException.class)
  public void nullName() {
    ConnectorKey.of(CommonUtils.randomString(5), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyName() {
    ConnectorKey.of(CommonUtils.randomString(5), "");
  }

  @Test
  public void testToString() {
    String group = CommonUtils.randomString(5);
    String name = CommonUtils.randomString(5);
    ConnectorKey key = ConnectorKey.of(group, name);
    Assert.assertTrue(key.toString().contains(group));
    Assert.assertTrue(key.toString().contains(name));
  }

  @Test
  public void testSerialization() {
    Assert.assertTrue(
        ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
            instanceof Serializable);
    Assert.assertTrue(
        ConnectorKey.toConnectorKey(
                ConnectorKey.toJsonString(
                    ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))))
            instanceof Serializable);
  }

  @Test
  public void testEqualToOtherKindsOfKey() {
    String group = CommonUtils.randomString();
    String name = CommonUtils.randomString();
    Assert.assertEquals(ObjectKey.of(group, name), ConnectorKey.of(group, name));
    Assert.assertEquals(TopicKey.of(group, name), ConnectorKey.of(group, name));
    Assert.assertEquals(ConnectorKey.of(group, name), ConnectorKey.of(group, name));
  }

  @Test
  public void testToConnectorKeys() {
    List<ConnectorKey> keys =
        Arrays.asList(
            ConnectorKey.of(CommonUtils.randomString(), CommonUtils.randomString()),
            ConnectorKey.of(CommonUtils.randomString(), CommonUtils.randomString()));

    List<ConnectorKey> copy = ConnectorKey.toConnectorKeys(ConnectorKey.toJsonString(keys));

    Assert.assertEquals(keys, copy);
  }
}
