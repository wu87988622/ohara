package com.island.ohara.common.data;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Test;

public class TestData extends SmallTest {

  private class MyData extends Data {
    private int a;
    private long b;
    private String c;
    private byte[] d;

    public MyData(int a, long b, String c, byte[] d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
    }
  }

  @Test
  public void testEquals() {
    MyData data = new MyData(5, 6, "test", new byte[] {5, 6, 7});
    assertEquals(data, new MyData(5, 6, "test", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyData(4, 6, "test", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyData(5, 7, "test", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyData(5, 6, "test2", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyData(5, 6, "test", new byte[] {5, 6, 8}));
  }

  @Test
  public void testHashCode() {
    MyData data = new MyData(5, 6, "test", new byte[] {5, 6, 7});
    assertEquals(data.hashCode(), new MyData(5, 6, "test", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(data.hashCode(), new MyData(4, 6, "test", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(data.hashCode(), new MyData(5, 7, "test", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(data.hashCode(), new MyData(5, 6, "test2", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(data.hashCode(), new MyData(5, 6, "test", new byte[] {5, 6, 8}).hashCode());
  }
}
