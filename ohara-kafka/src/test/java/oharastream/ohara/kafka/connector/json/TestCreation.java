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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import oharastream.ohara.common.json.JsonUtils;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestCreation extends OharaTest {

  @Test
  public void testEqual() throws IOException {
    Creation creation =
        Creation.of(
            CommonUtils.randomString(5), CommonUtils.randomString(5), CommonUtils.randomString(5));
    ObjectMapper mapper = JsonUtils.objectMapper();
    Assert.assertEquals(
        creation,
        mapper.readValue(mapper.writeValueAsString(creation), new TypeReference<Creation>() {}));
  }

  @Test
  public void testGetter() {
    String id = CommonUtils.randomString(5);
    String key = CommonUtils.randomString(5);
    String value = CommonUtils.randomString(5);
    Creation creation = Creation.of(id, key, value);
    Assert.assertEquals(id, creation.name());
    Assert.assertEquals(value, creation.configs().get(key));
  }

  @Test(expected = NullPointerException.class)
  public void nullName() {
    Creation.of(null, CommonUtils.randomString(5), CommonUtils.randomString(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyName() {
    Creation.of("", CommonUtils.randomString(5), CommonUtils.randomString(5));
  }

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    Creation.of(CommonUtils.randomString(5), null, CommonUtils.randomString(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    Creation.of(CommonUtils.randomString(5), "", CommonUtils.randomString(5));
  }

  @Test(expected = NullPointerException.class)
  public void nullValue() {
    Creation.of(CommonUtils.randomString(5), CommonUtils.randomString(5), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyValue() {
    Creation.of(CommonUtils.randomString(5), CommonUtils.randomString(5), "");
  }

  @Test
  public void testToString() {
    String name = CommonUtils.randomString(5);
    String key = CommonUtils.randomString(5);
    String value = CommonUtils.randomString(5);
    Creation creation = Creation.of(name, key, value);
    Assert.assertTrue(creation.toString().contains(name));
    Assert.assertTrue(creation.toString().contains(key));
    Assert.assertTrue(creation.toString().contains(value));
  }
}
