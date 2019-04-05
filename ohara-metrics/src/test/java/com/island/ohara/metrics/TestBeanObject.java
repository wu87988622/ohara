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

package com.island.ohara.metrics;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Test;

import java.util.Collections;

public class TestBeanObject extends SmallTest {

  @Test (expected = NullPointerException.class)
  public void testNullDomain() {
    BeanObject.builder().domainName(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testEmptyDomain() {
    BeanObject.builder().domainName("");
  }

  @Test (expected = NullPointerException.class)
  public void testNullProperties() {
    BeanObject.builder().properties(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testEmptyProperties() {
    BeanObject.builder().properties(Collections.emptyMap());
  }

  @Test (expected = NullPointerException.class)
  public void testNullValueInProperties() {
    BeanObject.builder().properties(Collections.singletonMap("a", null));
  }

  @Test (expected = IllegalArgumentException.class)
  public void testEmptyValueInProperties() {
    BeanObject.builder().properties(Collections.singletonMap("a", ""));
  }

  @Test (expected = NullPointerException.class)
  public void testNullAttributes() {
    BeanObject.builder().attributes(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testEmptyAttributes() {
    BeanObject.builder().attributes(Collections.emptyMap());
  }

  @Test (expected = NullPointerException.class)
  public void testNullValueInAttributes() {
    BeanObject.builder().attributes(Collections.singletonMap("a", null));
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testImmutableProperties() {
    BeanObject obj = BeanObject.builder()
      .domainName(CommonUtils.randomString())
      .properties(Collections.singletonMap("a", "b"))
      .attributes(Collections.singletonMap("a", "b"))
      .build();
    obj.properties().remove(("a"));
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testImmutableAttributes() {
    BeanObject obj = BeanObject.builder()
      .domainName(CommonUtils.randomString())
      .properties(Collections.singletonMap("a", "b"))
      .attributes(Collections.singletonMap("a", "b"))
      .build();
    obj.attributes().remove(("a"));
  }
}
