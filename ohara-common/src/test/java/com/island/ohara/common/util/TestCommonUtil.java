package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class TestCommonUtil extends SmallTest {

  @Test
  public void testTimer() throws InterruptedException {
    Assert.assertTrue(CommonUtil.current() != 0);
    CommonUtil.inject(() -> 0);
    Assert.assertEquals(0, CommonUtil.current());
    TimeUnit.SECONDS.sleep(2);
    Assert.assertEquals(0, CommonUtil.current());
    CommonUtil.reset();
    Assert.assertTrue(CommonUtil.current() != 0);
  }

  @Test
  public void testPath() {
    Assert.assertEquals("/ccc/abc", CommonUtil.path("/ccc", "abc"));
    Assert.assertEquals("/ccc/abc", CommonUtil.path("/ccc/", "abc"));
  }

  @Test
  public void testName() {
    Assert.assertEquals("ddd", CommonUtil.name("/abc/ddd"));
    Assert.assertEquals("aaa", CommonUtil.name("/abc/ddd/aaa"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failedToExtractNameFromRootPath() {
    CommonUtil.name("/");
  }

  @Test
  public void testReplaceParentFolder() {
    Assert.assertEquals("ccc/ddd", CommonUtil.replaceParent("ccc", "/abc/ddd"));
    Assert.assertEquals("/a/ddd", CommonUtil.replaceParent("/a", "/abc/ddd"));
    Assert.assertEquals("/a/ddd", CommonUtil.replaceParent("/a", "/abc/ttt/ddd"));
    Assert.assertEquals("/a/ddd", CommonUtil.replaceParent("/a", "/abc/tt/t/ddd"));
  }
}
