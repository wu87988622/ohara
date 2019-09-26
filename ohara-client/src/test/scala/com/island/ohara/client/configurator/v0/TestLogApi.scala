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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.v0.LogApi.{ClusterLog, NodeLog}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._

class TestLogApi extends OharaTest with Matchers {

  @Test
  def testStaleName(): Unit = {
    val nodeLog = NodeLog(
      hostname = CommonUtils.randomString(),
      value = CommonUtils.randomString()
    )
    val clusterLog = ClusterLog(
      clusterKey = ObjectKey.of(CommonUtils.randomString(), CommonUtils.randomString()),
      logs = Seq(nodeLog)
    )
    LogApi.NODE_LOG_FORMAT.write(nodeLog).asJsObject.fields("name").convertTo[String] shouldBe nodeLog.hostname
    LogApi.CLUSTER_LOG_FORMAT
      .write(clusterLog)
      .asJsObject
      .fields("name")
      .convertTo[String] shouldBe clusterLog.clusterKey.name()
  }
}
