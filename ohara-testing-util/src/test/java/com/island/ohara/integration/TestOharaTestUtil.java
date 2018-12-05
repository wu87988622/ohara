package com.island.ohara.integration;

import com.island.ohara.common.rule.MediumTest;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;

public class TestOharaTestUtil extends MediumTest {

  @Test(expected = RuntimeException.class)
  public void testLocalMethod() throws Exception {
    setEnv("ohara.it.workers", "123");
    try (OharaTestUtil util = OharaTestUtil.workers()) {
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
