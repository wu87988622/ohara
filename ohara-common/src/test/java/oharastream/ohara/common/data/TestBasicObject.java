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

package oharastream.ohara.common.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import oharastream.ohara.common.rule.OharaTest;
import org.junit.Test;

public class TestBasicObject extends OharaTest {

  private static class MyBasicObject extends BasicObject {
    private final int a;
    private final long b;
    private final String c;
    private final byte[] d;

    public MyBasicObject(int a, long b, String c, byte[] d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
    }
  }

  @Test
  public void testEquals() {
    MyBasicObject data = new MyBasicObject(5, 6, "test", new byte[] {5, 6, 7});
    assertEquals(data, new MyBasicObject(5, 6, "test", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyBasicObject(4, 6, "test", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyBasicObject(5, 7, "test", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyBasicObject(5, 6, "test2", new byte[] {5, 6, 7}));
    assertNotEquals(data, new MyBasicObject(5, 6, "test", new byte[] {5, 6, 8}));
  }

  @Test
  public void testHashCode() {
    MyBasicObject data = new MyBasicObject(5, 6, "test", new byte[] {5, 6, 7});
    assertEquals(data.hashCode(), new MyBasicObject(5, 6, "test", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(
        data.hashCode(), new MyBasicObject(4, 6, "test", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(
        data.hashCode(), new MyBasicObject(5, 7, "test", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(
        data.hashCode(), new MyBasicObject(5, 6, "test2", new byte[] {5, 6, 7}).hashCode());
    assertNotEquals(
        data.hashCode(), new MyBasicObject(5, 6, "test", new byte[] {5, 6, 8}).hashCode());
  }
}
