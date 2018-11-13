package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Assert;
import org.junit.Test;

public class TestVersionUtil extends SmallTest {

  @Test
  public void allMembersShouldExist() {
    Assert.assertNotNull(VersionUtil.DATE);
    Assert.assertFalse(VersionUtil.DATE.isEmpty());
    Assert.assertNotNull(VersionUtil.REVISION);
    Assert.assertFalse(VersionUtil.REVISION.isEmpty());
    Assert.assertNotNull(VersionUtil.USER);
    Assert.assertFalse(VersionUtil.USER.isEmpty());
    Assert.assertNotNull(VersionUtil.VERSION);
    Assert.assertFalse(VersionUtil.VERSION.isEmpty());
  }
}
