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

package com.island.ohara.integration;

import com.island.ohara.common.rule.MediumTest;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;

public class TestOharaTestUtil extends MediumTest {

  @Test(expected = RuntimeException.class)
  public void testLocalMethod() {
    setEnv("ohara.it.workers", "123");
    try (OharaTestUtil util = OharaTestUtil.workers(1)) {
      util.brokersConnProps();
    }
  }

  @SuppressWarnings("unchecked")
  private void setEnv(String key, String value) {
    try {
      Field field = System.getenv().getClass().getDeclaredField("m");
      field.setAccessible(true);
      Map<String, String> map = (Map<String, String>) field.get(System.getenv());
      map.put(key, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
