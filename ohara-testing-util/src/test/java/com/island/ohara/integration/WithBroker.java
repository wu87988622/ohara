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

package com.island.ohara.integration;

import com.island.ohara.common.rule.LargeTest;
import com.island.ohara.common.util.ReleaseOnce;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * This class create a mini broker cluster with single node. And the cluster will be closed after
 * all test cases have been done.
 *
 * <p>NOTED: You can't create a topic with multi-partitions or multi-replication in this env,
 *
 * <p>NOTED: All subclass has "same" reference to util. This is ok in junit test since it default
 * run each test on "different" jvm. The "same" static member won't cause trouble in testing.
 * However, you should move the static "util" into your test if you don't depend on junit...by chia
 */
public abstract class WithBroker extends LargeTest {
  protected static OharaTestUtil util;

  @BeforeClass
  public static void beforeAll() {
    if (util != null)
      throw new IllegalArgumentException(
          "The test util had been initialized!!! This happens on your tests don't run on different jvm");
    util = OharaTestUtil.broker();
  }

  protected OharaTestUtil testUtil() {
    return util;
  }

  @AfterClass
  public static void afterAll() {
    ReleaseOnce.close(util);
    // we have to assign null to util since we allow junit to reuse jvm
    util = null;
  }
}
