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

package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import org.junit.Assert;
import org.junit.Test;

public class TestVersionUtils extends SmallTest {

  @Test
  public void allMembersShouldExist() {
    Assert.assertNotNull(VersionUtils.DATE);
    Assert.assertFalse(VersionUtils.DATE.isEmpty());
    Assert.assertNotNull(VersionUtils.REVISION);
    Assert.assertFalse(VersionUtils.REVISION.isEmpty());
    Assert.assertNotNull(VersionUtils.USER);
    Assert.assertFalse(VersionUtils.USER.isEmpty());
    Assert.assertNotNull(VersionUtils.VERSION);
    Assert.assertFalse(VersionUtils.VERSION.isEmpty());
    Assert.assertNotNull(VersionUtils.BRANCH);
    Assert.assertFalse(VersionUtils.BRANCH.isEmpty());
  }
}
