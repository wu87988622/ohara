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

package com.island.ohara.agent
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.VersionUtil
import org.junit.Test
import org.scalatest.Matchers

class TestVersion extends SmallTest with Matchers {

  @Test
  def testZookeeper(): Unit = {
    ZookeeperCollie.IMAGE_NAME_DEFAULT shouldBe s"oharastream/zookeeper:${VersionUtil.VERSION}"
  }

  @Test
  def testBroker(): Unit = {
    BrokerCollie.IMAGE_NAME_DEFAULT shouldBe s"oharastream/broker:${VersionUtil.VERSION}"
  }

  @Test
  def testWorker(): Unit = {
    WorkerCollie.IMAGE_NAME_DEFAULT shouldBe s"oharastream/connect-worker:${VersionUtil.VERSION}"
  }
}
