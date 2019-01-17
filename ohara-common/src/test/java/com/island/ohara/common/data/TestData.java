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

package com.island.ohara.common.data;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Test;

public class TestData extends SmallTest {

  private class MyData extends Data {
    private final int a;
    private final long b;
    private final String c;
    private final byte[] d;

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
