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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.{BrokerApi, NodeApi, ZookeeperApi}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.fake.FakeZookeeperCollie
import org.junit.{After, Before, Test}
import org.scalatest.Matchers
import spray.json.{DeserializationException, JsArray, JsNumber, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
class TestZookeeperRoute extends OharaTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder.fake(numberOfCluster, 0).build()

  /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val zookeeperApi = ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val nodeNames: Set[String] = Set("n0", "n1")

  private[this] def result[T](f: Future[T]): T = Await.result(f, Duration("20 seconds"))
  @Before
  def setup(): Unit = {
    val nodeAccess = NodeApi.access.hostname(configurator.hostname).port(configurator.port)

    nodeNames.isEmpty shouldBe false
    nodeNames.foreach { n =>
      result(nodeAccess.request.hostname(n).port(22).user("user").password("pwd").create())
    }

    result(nodeAccess.list()).size shouldBe (nodeNames.size + numberOfDefaultNodes)
  }

  @Test
  def repeatedlyDelete(): Unit = {
    (0 to 10).foreach { index =>
      result(zookeeperApi.delete(ObjectKey.of(index.toString, index.toString)))
    }
  }

  @Test
  def removeZookeeperClusterUsedByBrokerCluster(): Unit = {
    val zks = result(zookeeperApi.list())
    val bks = result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list())
    System.out.println(zks)
    System.out.println(bks)
    // we have a default zk cluster
    zks.isEmpty shouldBe false

    val zk = zks.head

    // this zookeeper cluster is used by broker cluster
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.stop(zk.key))

    // remove all broker clusters
    result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list())
      .map(_.key)
      .foreach(key =>
        result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).stop(key)
          flatMap (_ => BrokerApi.access.hostname(configurator.hostname).port(configurator.port).delete(key))))

    // pass
    result(zookeeperApi.stop(zk.key))
    result(zookeeperApi.delete(zk.key))
  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).nodeName(CommonUtils.randomString(10)).create()
    )

    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.start(zk.key))
  }

  @Test
  def testImageName(): Unit = {
    // pass by default image
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))
    result(zookeeperApi.get(zk.key)).imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT

    // in fake mode only IMAGE_NAME_DEFAULT is supported
    val p = result(
      zookeeperApi.request
        .name(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .create())
    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.start(p.key)
    )
  }

  @Test
  def testList(): Unit = {
    val init = result(zookeeperApi.list()).size
    val count = 3
    (0 until count).foreach { _ =>
      result(
        zookeeperApi.request.name(CommonUtils.randomString(10)).nodeNames(nodeNames).create()
      )
    }
    result(zookeeperApi.list()).size shouldBe count + init
  }

  @Test
  def testDelete(): Unit = {
    val init = result(zookeeperApi.list()).size
    val cluster = result(zookeeperApi.request.name(CommonUtils.randomString(10)).nodeNames(nodeNames).create())
    result(zookeeperApi.list()).size shouldBe init + 1
    result(zookeeperApi.delete(cluster.key))
    result(zookeeperApi.list()).size shouldBe init
  }

  @Test
  def testStop(): Unit = {
    val init = result(zookeeperApi.list()).size
    val cluster = result(zookeeperApi.request.name(CommonUtils.randomString(10)).nodeNames(nodeNames).create())
    result(zookeeperApi.start(cluster.key))
    result(zookeeperApi.list()).size shouldBe init + 1
    result(zookeeperApi.stop(cluster.key))
    result(zookeeperApi.delete(cluster.key))
    result(zookeeperApi.list()).size shouldBe init
  }

  @Test
  def testAddNode(): Unit = {
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).nodeName(nodeNames.head).create()
    )
    result(zookeeperApi.start(zk.key))
    zk.nodeNames.size shouldBe 1
    zk.nodeNames.head shouldBe nodeNames.head
    // we don't support to add zk node at runtime
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.addNode(zk.key, nodeNames.last))
  }

  @Test
  def testRemoveNode(): Unit = {
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))
    // we don't support to remove zk node at runtime
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.removeNode(zk.key, nodeNames.head))
  }

  @Test
  def testInvalidClusterName(): Unit =
    an[DeserializationException] should be thrownBy result(
      zookeeperApi.request.name("abc def").nodeNames(nodeNames).create()
    )

  @Test
  def createZkClusterWithSameName(): Unit = {
    val name = CommonUtils.randomString(10)
    result(
      zookeeperApi.request.name(name).nodeNames(nodeNames).create()
    )

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.request.name(name).nodeNames(nodeNames).create()
    )
  }

  @Test
  def clientPortConflict(): Unit = {
    val clientPort = CommonUtils.availablePort()
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).clientPort(clientPort).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))

    val zk2 = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).clientPort(clientPort).nodeNames(nodeNames).create()
    )
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.start(zk2.key))
  }

  @Test
  def peerPortConflict(): Unit = {
    val peerPort = CommonUtils.availablePort()
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).peerPort(peerPort).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))

    val zk2 = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).peerPort(peerPort).nodeNames(nodeNames).create()
    )
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.start(zk2.key))
  }

  @Test
  def electionPortConflict(): Unit = {
    val electionPort = CommonUtils.availablePort()
    val zk = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).electionPort(electionPort).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))

    val zk2 = result(
      zookeeperApi.request.name(CommonUtils.randomString(10)).electionPort(electionPort).nodeNames(nodeNames).create()
    )
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.start(zk2.key))
  }

  @Test
  def testIdempotentStart(): Unit = {
    val zk = result(
      zookeeperApi.request.nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))
    val info1 = result(zookeeperApi.get(zk.key))
    // duplicated start will return the current cluster info
    result(zookeeperApi.start(zk.key))
    val info2 = result(zookeeperApi.get(zk.key))
    info1.name shouldBe info2.name
    info1.imageName shouldBe info2.imageName
    info1.nodeNames shouldBe info2.nodeNames
    info1.state shouldBe info2.state
    info1.error shouldBe info2.error
    info1.electionPort shouldBe info2.electionPort

    // we could graceful stop zookeeper
    result(zookeeperApi.stop(zk.key))
    // stop should be idempotent
    result(zookeeperApi.stop(zk.key))
    // delete should be idempotent also
    result(zookeeperApi.delete(zk.key))
    result(zookeeperApi.delete(zk.key))
    // after delete, stop will cause NoSuchElement exception
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.stop(zk.key))
  }

  @Test
  def testForceStop(): Unit = {
    val initialCount = configurator.clusterCollie.zookeeperCollie.asInstanceOf[FakeZookeeperCollie].forceRemoveCount
    val name = CommonUtils.randomString(10)
    // graceful stop
    val zk = result(
      zookeeperApi.request.name(name).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk.key))
    result(zookeeperApi.stop(zk.key))
    result(zookeeperApi.delete(zk.key))
    configurator.clusterCollie.zookeeperCollie.asInstanceOf[FakeZookeeperCollie].forceRemoveCount shouldBe initialCount

    // force stop
    val zk2 = result(
      zookeeperApi.request.name(name).nodeNames(nodeNames).create()
    )
    result(zookeeperApi.start(zk2.key))
    result(zookeeperApi.forceStop(zk2.key))
    result(zookeeperApi.delete(zk2.key))
    configurator.clusterCollie.zookeeperCollie
      .asInstanceOf[FakeZookeeperCollie]
      .forceRemoveCount shouldBe initialCount + 1
  }

  @Test
  def failToUpdateRunningZookeeperCluster(): Unit = {
    val zk = result(zookeeperApi.request.nodeName(nodeNames.head).create())
    result(zookeeperApi.start(zk.key))
    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.request.name(zk.name).nodeNames(nodeNames).update())
    result(zookeeperApi.stop(zk.key))
    result(zookeeperApi.request.name(zk.name).nodeNames(nodeNames).update())
    result(zookeeperApi.start(zk.key))
  }

  @Test
  def testCustomTagsShouldExistAfterRunning(): Unit = {
    val tags = Map(
      "aa" -> JsString("bb"),
      "cc" -> JsNumber(123),
      "dd" -> JsArray(JsString("bar"), JsString("foo"))
    )
    val zk = result(zookeeperApi.request.tags(tags).nodeNames(nodeNames).create())
    zk.tags shouldBe tags

    // after create, tags should exist
    result(zookeeperApi.get(zk.key)).tags shouldBe tags

    // after start, tags should still exist
    result(zookeeperApi.start(zk.key))
    result(zookeeperApi.get(zk.key)).tags shouldBe tags

    // after stop, tags should still exist
    result(zookeeperApi.stop(zk.key))
    result(zookeeperApi.get(zk.key)).tags shouldBe tags
  }

  @Test
  def testGroup(): Unit = {
    val group = CommonUtils.randomString(10)
    // different name but same group
    result(zookeeperApi.request.group(group).nodeNames(nodeNames).create()).group shouldBe group
    result(zookeeperApi.request.group(group).nodeNames(nodeNames).create()).group shouldBe group

    // number of created (2) + configurator default (1)
    result(zookeeperApi.list()).size shouldBe 3

    // same name but different group
    val name = CommonUtils.randomString(10)
    val zk1 = result(zookeeperApi.request.name(name).nodeNames(nodeNames).create())
    zk1.name shouldBe name
    zk1.group should not be group
    val zk2 = result(zookeeperApi.request.name(name).group(group).nodeNames(nodeNames).create())
    zk2.name shouldBe name
    zk2.group shouldBe group

    // number of created (4) + configurator default (1)
    result(zookeeperApi.list()).size shouldBe 5
  }

  @Test
  def testUpdateAsCreateRequest(): Unit = {
    val info = result(zookeeperApi.request.nodeNames(nodeNames).create())

    // use same name and group will cause a update request
    result(zookeeperApi.request.name(info.name).group(info.group).clientPort(1234).update()).clientPort shouldBe 1234

    // use different group will cause a create request
    result(
      zookeeperApi.request
        .name(info.name)
        .group(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .peerPort(1234)
        .update()).peerPort should not be info.peerPort
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
