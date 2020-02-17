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

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestValidation extends OharaTest {

  @Test(expected = NoSuchElementException.class)
  public void ignoreClassName() {
    Validation.of(Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()))
        .className();
  }

  @Test(expected = NoSuchElementException.class)
  public void ignoreTopicNames() {
    Validation.of(Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()))
        .topicNames();
  }

  @Test
  public void testRequiredSettings() {
    String className = CommonUtils.randomString();
    List<String> topicNames = Collections.singletonList(CommonUtils.randomString());
    Validation validation = Validation.of(className, topicNames);
    Assert.assertEquals(className, validation.className());
    Assert.assertEquals(topicNames, validation.topicNames());
  }

  @Test
  public void testEqual() {
    Validation validation =
        Validation.of(
            Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()));
    Assert.assertEquals(validation, validation);
  }

  @Test
  public void testToString() {
    Validation validation =
        Validation.of(
            Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()));
    Assert.assertEquals(validation.toString(), validation.toString());
  }

  @Test
  public void testToJsonString() {
    Validation validation =
        Validation.of(
            Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()));
    Assert.assertEquals(validation.toJsonString(), validation.toJsonString());
  }

  @Test
  public void testHashCode() {
    Validation validation =
        Validation.of(
            Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()));
    Assert.assertEquals(validation.hashCode(), validation.hashCode());
  }

  @Test
  public void testGetter() {
    String key = CommonUtils.randomString(5);
    String value = CommonUtils.randomString(5);
    Validation validation = Validation.of(Collections.singletonMap(key, value));
    Assert.assertEquals(Collections.singletonMap(key, value), validation.settings());
    Assert.assertEquals(value, validation.settings().get(key));
  }

  @Test(expected = NullPointerException.class)
  public void nullSettings() {
    Validation.of(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptySettings() {
    Validation.of(Collections.emptyMap());
  }

  @Test(expected = NullPointerException.class)
  public void nullValue() {
    Validation.of(Collections.singletonMap(CommonUtils.randomString(), null));
  }

  @Test
  public void emptyValue() {
    Validation.of(Collections.singletonMap(CommonUtils.randomString(), ""));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void failToModify() {
    Validation.of(Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()))
        .settings()
        .remove("a");
  }
}
