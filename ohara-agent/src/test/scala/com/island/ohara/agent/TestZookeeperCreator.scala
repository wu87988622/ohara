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

import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Future

class TestZookeeperCreator extends SmallTest with Matchers {

  private[this] def zkCreator(): ZookeeperCollie.ClusterCreator = (clusterName: String,
                                                                   imageName: String,
                                                                   clientPort: Int,
                                                                   peerPort: Int,
                                                                   electionPort: Int,
                                                                   nodeNames: Seq[String]) => {
    // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
    if (clusterName == null || clusterName.isEmpty) throw new AssertionError()
    if (imageName == null || imageName.isEmpty) throw new AssertionError()
    if (clientPort <= 0) throw new AssertionError()
    if (peerPort <= 0) throw new AssertionError()
    if (electionPort <= 0) throw new AssertionError()
    if (nodeNames == null || nodeNames.isEmpty) throw new AssertionError()
    Future.successful(
      ZookeeperClusterInfo(
        name = clusterName,
        imageName = imageName,
        clientPort = clientPort,
        peerPort = peerPort,
        electionPort = electionPort,
        nodeNames = nodeNames
      ))
  }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().clusterName(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().clusterName("")
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().clientPort(-1)
  }

  @Test
  def negativePeerPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().peerPort(-1)
  }

  @Test
  def negativeElectionPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().electionPort(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().nodeNames(Seq.empty)
  }

  @Test
  def testNameLength(): Unit = {
    // pass
    zkCreator()
      .clusterName(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString(10))
      .peerPort(CommonUtils.availablePort())
      .clientPort(CommonUtils.availablePort())
      .electionPort(CommonUtils.availablePort())
      .nodeNames(Seq("asdasd"))
      .create()

    an[IllegalArgumentException] should be thrownBy zkCreator()
      .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH + 1))
      .imageName(CommonUtils.randomString(10))
      .peerPort(CommonUtils.availablePort())
      .clientPort(CommonUtils.availablePort())
      .electionPort(CommonUtils.availablePort())
      .nodeNames(Seq("asdasd"))
      .create()
  }
}
