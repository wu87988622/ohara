package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Assert;
import org.junit.Test;

public class TestCloseOnce extends SmallTest {

  @Test
  public void testIsClosed() {
    SimpleCloseOnce c = new SimpleCloseOnce();
    c.close();
    Assert.assertTrue(c.isClosed());
  }

  @Test
  public void testCloseOnce() {
    SimpleCloseOnce c = new SimpleCloseOnce();
    c.close();
    Assert.assertEquals(1, c.closeCount);
    c.close();
    Assert.assertEquals(1, c.closeCount);
  }

  @Test
  public void testSwallowException() {
    CloseOnce.close(new TerribleCloseOnce());
    CloseOnce.close(new TerribleCloseOnce(), true);
  }

  /** NOTED: all exception is converted to RuntimeException */
  @Test(expected = RuntimeException.class)
  public void testThrowException() {
    CloseOnce.close(new TerribleCloseOnce(), false);
  }

  private static class SimpleCloseOnce extends CloseOnce {
    private int closeCount = 0;

    @Override
    protected void doClose() {
      ++closeCount;
    }
  }

  private static class TerribleCloseOnce extends CloseOnce {

    @Override
    protected void doClose() {
      throw new RuntimeException("awwwwwwwww");
    }
  }
}
