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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorVersion extends SmallTest {
  @Test(expected = NullPointerException.class)
  public void testNullVersion() {
    ConnectorVersion.builder().version(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyVersion() {
    ConnectorVersion.builder().version("");
  }

  @Test
  public void ignoreVersion() {
    // pass
    ConnectorVersion.builder()
        .revision(CommonUtils.randomString())
        .author(CommonUtils.randomString())
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void testNullRevision() {
    ConnectorVersion.builder().revision(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyRevision() {
    ConnectorVersion.builder().revision("");
  }

  @Test
  public void ignoreRevision() {
    // pass
    ConnectorVersion.builder()
        .version(CommonUtils.randomString())
        .author(CommonUtils.randomString())
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void testNullAuthor() {
    ConnectorVersion.builder().author(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyAuthor() {
    ConnectorVersion.builder().author("");
  }

  @Test
  public void ignoreAuthor() {
    // pass
    ConnectorVersion.builder()
        .revision(CommonUtils.randomString())
        .version(CommonUtils.randomString())
        .build();
  }

  @Test
  public void testEquals() {
    ConnectorVersion.Builder builder =
        ConnectorVersion.builder()
            .version(CommonUtils.randomString())
            .revision(CommonUtils.randomString())
            .author(CommonUtils.randomString());
    Assert.assertEquals(builder.build(), builder.build());
  }

  @Test
  public void testGetter() {
    String version = CommonUtils.randomString();
    String revision = CommonUtils.randomString();
    String author = CommonUtils.randomString();
    ConnectorVersion connectorVersion =
        ConnectorVersion.builder().version(version).revision(revision).author(author).build();
    Assert.assertEquals(version, connectorVersion.version());
    Assert.assertEquals(revision, connectorVersion.revision());
    Assert.assertEquals(author, connectorVersion.author());
  }
}
