package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Assert;
import org.junit.Test;

public class TestReleaseOnce extends SmallTest {

  @Test
  public void testIsClosed() {
    SimpleReleaseOnce c = new SimpleReleaseOnce();
    c.close();
    Assert.assertTrue(c.isClosed());
  }

  @Test
  public void testCloseOnce() {
    SimpleReleaseOnce c = new SimpleReleaseOnce();
    c.close();
    Assert.assertEquals(1, c.closeCount);
    c.close();
    Assert.assertEquals(1, c.closeCount);
  }

  @Test
  public void testSwallowException() {
    ReleaseOnce.close(new TerribleReleaseOnce());
    ReleaseOnce.close(new TerribleReleaseOnce(), true);
  }

  /** NOTED: all exception is converted to RuntimeException */
  @Test(expected = RuntimeException.class)
  public void testThrowException() {
    ReleaseOnce.close(new TerribleReleaseOnce(), false);
  }

  private static class SimpleReleaseOnce extends ReleaseOnce {
    private int closeCount = 0;

    @Override
    protected void doClose() {
      ++closeCount;
    }
  }

  private static class TerribleReleaseOnce extends ReleaseOnce {

    @Override
    protected void doClose() {
      throw new RuntimeException("awwwwwwwww");
    }
  }
}
