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

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Future

class TestBrokerCreator extends SmallTest with Matchers {

  private[this] def bkCreator(): BrokerCollie.ClusterCreator = (clusterName: String,
                                                                imageName: String,
                                                                zookeeperClusterName: String,
                                                                clientPort: Int,
                                                                exporterPort: Int,
                                                                nodeNames: Seq[String]) => {
    // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
    if (clusterName == null || clusterName.isEmpty) throw new AssertionError()
    if (imageName == null || imageName.isEmpty) throw new AssertionError()
    if (clientPort <= 0) throw new AssertionError()
    if (exporterPort <= 0) throw new AssertionError()
    if (zookeeperClusterName == null || zookeeperClusterName.isEmpty) throw new AssertionError()
    if (nodeNames == null || nodeNames.isEmpty) throw new AssertionError()
    Future.successful(
      BrokerClusterInfo(
        name = clusterName,
        imageName = imageName,
        zookeeperClusterName = zookeeperClusterName,
        clientPort = clientPort,
        exporterPort = exporterPort,
        nodeNames = nodeNames
      ))
  }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().clusterName(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().clusterName("")
  }

  @Test
  def nullZkClusterName(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().zookeeperClusterName(null)
  }

  @Test
  def emptyZkClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().zookeeperClusterName("")
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().clientPort(-1)
  }

  @Test
  def negativeExporterPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().exporterPort(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().nodeNames(Seq.empty)
  }

  @Test
  def testNameLength(): Unit = {
    // pass
    bkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .zookeeperClusterName("zk")
      .exporterPort(CommonUtil.availablePort())
      .clientPort(CommonUtil.availablePort())
      .nodeNames(Seq("abc"))
      .create()

    an[IllegalArgumentException] should be thrownBy bkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(Collie.LIMIT_OF_NAME_LENGTH + 1))
      .zookeeperClusterName("zk")
      .exporterPort(CommonUtil.availablePort())
      .clientPort(CommonUtil.availablePort())
      .nodeNames(Seq("abc"))
      .create()
  }
}
