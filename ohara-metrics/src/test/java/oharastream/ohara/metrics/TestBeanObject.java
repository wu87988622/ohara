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

package oharastream.ohara.metrics;

import java.util.Map;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.util.CommonUtils;
import org.junit.Test;

public class TestBeanObject extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void testNullDomain() {
    BeanObject.builder().domainName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyDomain() {
    BeanObject.builder().domainName("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullProperties() {
    BeanObject.builder().properties(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyProperties() {
    BeanObject.builder().properties(Map.of());
  }

  @Test(expected = NullPointerException.class)
  public void testNullValueInProperties() {
    BeanObject.builder().properties(Map.of("a", null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyValueInProperties() {
    BeanObject.builder().properties(Map.of("a", ""));
  }

  @Test(expected = NullPointerException.class)
  public void testNullAttributes() {
    BeanObject.builder().attributes(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyAttributes() {
    BeanObject.builder().attributes(Map.of());
  }

  @Test(expected = NullPointerException.class)
  public void testNullValueInAttributes() {
    BeanObject.builder().attributes(Map.of("a", null));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testImmutableProperties() {
    BeanObject obj =
        BeanObject.builder()
            .domainName(CommonUtils.randomString())
            .properties(Map.of("a", "b"))
            .attributes(Map.of("a", "b"))
            .queryTime(CommonUtils.current())
            .build();
    obj.properties().remove(("a"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testImmutableAttributes() {
    BeanObject obj =
        BeanObject.builder()
            .domainName(CommonUtils.randomString())
            .properties(Map.of("a", "b"))
            .attributes(Map.of("a", "b"))
            .queryTime(CommonUtils.current())
            .build();
    obj.attributes().remove(("a"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroQueryNumber() {
    BeanObject.builder()
        .domainName(CommonUtils.randomString())
        .properties(Map.of("a", "b"))
        .attributes(Map.of("a", "b"))
        .queryTime(0)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeQueryNumber() {
    BeanObject.builder()
        .domainName(CommonUtils.randomString())
        .properties(Map.of("a", "b"))
        .attributes(Map.of("a", "b"))
        .queryTime(-999)
        .build();
  }
}
