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

package oharastream.ohara.it.collie

import java.time.Duration

import oharastream.ohara.agent.docker.ContainerState
import oharastream.ohara.agent.{ArgumentsBuilder, DataCollie, RemoteFolderHandler}
import oharastream.ohara.client.configurator.NodeApi.Node
import oharastream.ohara.client.configurator.VolumeApi.Volume
import oharastream.ohara.client.configurator.{BrokerApi, ZookeeperApi}
import oharastream.ohara.common.data.Serializer
import oharastream.ohara.common.setting.TopicKey
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.it.{ContainerPlatform, IntegrationTest}
import oharastream.ohara.kafka.{Consumer, Producer}
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{After, AssumptionViolatedException, Test}
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

@deprecated(message = s"this IT does not use public APIs", since = "0.11.0")
@RunWith(value = classOf[Parameterized])
class TestMountVolume(platform: ContainerPlatform) extends IntegrationTest {
  private[this] val containerClient = platform.setupContainerClient()
  private[this] val nodes: Seq[Node] = ContainerPlatform.dockerNodes.getOrElse(
    throw new AssumptionViolatedException(s"${ContainerPlatform.DOCKER_NODES_KEY} the key is not exists")
  )

  @Test
  def test(): Unit = {
    val zkNodePath      = s"/tmp/zk-${CommonUtils.randomString(5)}"
    val bkNodePath      = s"/tmp/bk-${CommonUtils.randomString(5)}"
    val dataCollie      = DataCollie(nodes)
    val remoteFolder    = RemoteFolderHandler.builder().dataCollie(dataCollie).build
    val zkContainerName = s"zookeeper-${CommonUtils.randomString(5)}"
    val bkContainerName = s"broker-${CommonUtils.randomString(5)}"
    val zkNodeName      = s"${platform.nodeNames.head}"
    val bkNodeName      = s"${platform.nodeNames.head}"
    val zkVolume = Volume(
      group = CommonUtils.randomString(5),
      name = CommonUtils.randomString(5),
      nodeNames = Set(zkNodeName),
      path = zkNodePath,
      state = Option.empty,
      error = Option.empty,
      tags = Map.empty,
      lastModified = CommonUtils.current()
    )

    val bkVolume = Volume(
      group = CommonUtils.randomString(5),
      name = CommonUtils.randomString(5),
      nodeNames = Set(bkNodeName),
      path = bkNodePath,
      state = Option.empty,
      error = Option.empty,
      tags = Map.empty,
      lastModified = CommonUtils.current()
    )
    try {
      result(remoteFolder.create(zkNodeName, zkNodePath))
      result(remoteFolder.create(bkNodeName, bkNodePath))

      val zkClientPort = CommonUtils.availablePort()
      val bkClientPort = CommonUtils.availablePort()
      // Create zookeeper volume
      result(
        containerClient.volumeCreator
          .name(zkVolume.key.toPlain)
          .nodeName(zkVolume.nodeNames.head)
          .path(zkVolume.path)
          .create()
      )

      // Create broker volume
      result(
        containerClient.volumeCreator
          .name(bkVolume.key.toPlain)
          .nodeName(bkVolume.nodeNames.head)
          .path(bkVolume.path)
          .create()
      )

      createZkContainer(zkContainerName, zkClientPort, zkVolume)
      await { () =>
        result(containerClient.containers(zkContainerName)).head.state.equals(ContainerState.RUNNING.name)
      }
      createBkContainer(bkContainerName, zkClientPort, bkClientPort, bkVolume)

      val topicKey        = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
      val numberOfRecords = 5
      val connectionProps = s"${platform.nodeNames.head}:$bkClientPort"
      writeData(topicKey, connectionProps, numberOfRecords)
      readData(topicKey, connectionProps, numberOfRecords)

      // Remove zookeeper and broker the container
      result(containerClient.forceRemove(bkContainerName))
      result(containerClient.forceRemove(zkContainerName))

      // Create a new container
      createZkContainer(zkContainerName, zkClientPort, zkVolume)
      await { () =>
        result(containerClient.containers(zkContainerName)).head.state.equals(ContainerState.RUNNING.name) &&
        result(containerClient.log(zkContainerName, None)).head._2.contains("Expiring session")
      }
      createBkContainer(bkContainerName, zkClientPort, bkClientPort, bkVolume)
      readData(topicKey, connectionProps, numberOfRecords)
    } finally {
      Releasable.close(() => result(containerClient.forceRemove(bkContainerName)))
      Releasable.close(() => result(containerClient.forceRemove(zkContainerName)))
      Releasable.close(() => result(containerClient.removeVolumes(bkVolume.key.toPlain)))
      Releasable.close(() => result(containerClient.removeVolumes(zkVolume.key.toPlain)))
      Releasable.close(() => result(remoteFolder.delete(bkNodeName, bkNodePath)))
      Releasable.close(() => result(remoteFolder.delete(zkNodeName, zkNodePath)))
    }
  }

  private[this] def readData(topicKey: TopicKey, connectionProps: String, numberOfRecords: Int): Unit = {
    val consumer = Consumer
      .builder()
      .connectionProps(connectionProps)
      .offsetFromBegin()
      .topicKey(topicKey)
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .build()
    try {
      val records = consumer.poll(Duration.ofSeconds(30), numberOfRecords)
      records.size() shouldBe numberOfRecords
    } finally consumer.close()
  }

  private[this] def writeData(topicKey: TopicKey, connectionProps: String, numberOfRecords: Int): Unit = {
    val producer = Producer
      .builder()
      .connectionProps(connectionProps)
      .allAcks()
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .build()

    try {
      (0 until numberOfRecords).foreach(
        index => producer.sender().key(index.toString).value(index.toString).topicKey(topicKey).send()
      )
      producer.flush()
    } finally producer.close()
  }

  private[this] def createZkContainer(containerName: String, zkClientPort: Int, volume: Volume): Unit = {
    val zkContainerConfigPath = s"/home/ohara/default/conf/zoo.cfg"
    val dataFolder            = "/tmp/zk_data"
    val zkArguments = ArgumentsBuilder()
      .mainConfigFile(zkContainerConfigPath)
      .file(zkContainerConfigPath)
      .append("clientPort", zkClientPort)
      .append(ZookeeperApi.DATA_DIR_KEY, dataFolder)
      .done
      .file(s"$dataFolder/myid")
      .append(0)
      .done
      .build

    val nodeName = platform.nodeNames.head
    result(
      containerClient.containerCreator
        .nodeName(nodeName)
        .name(containerName)
        .portMappings(Map(zkClientPort -> zkClientPort))
        .routes(Map(nodeName -> CommonUtils.address(nodeName)))
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        .volumeMaps(Map(volume.key.toPlain -> dataFolder))
        .arguments(zkArguments)
        .create()
    )
  }

  private[this] def createBkContainer(
    containerName: String,
    zkClientPort: Int,
    bkClientPort: Int,
    volume: Volume
  ): Unit = {
    val bkConfigPath: String = s"/home/ohara/default/config/broker.config"
    val dataFolder           = "/tmp/bk_data"
    val bkArguments = ArgumentsBuilder()
      .mainConfigFile(bkConfigPath)
      .file(bkConfigPath)
      .append("zookeeper.connect", s"${platform.nodeNames.head}:$zkClientPort")
      .append(BrokerApi.LOG_DIRS_KEY, dataFolder)
      .append(BrokerApi.NUMBER_OF_REPLICATIONS_4_OFFSETS_TOPIC_KEY, 1)
      .append(s"listeners=PLAINTEXT://:$bkClientPort")
      .append(s"advertised.listeners=PLAINTEXT://${platform.nodeNames.head}:$bkClientPort")
      .done
      .build

    val nodeName = platform.nodeNames.head
    result(
      containerClient.containerCreator
        .nodeName(nodeName)
        .name(containerName)
        .portMappings(Map(bkClientPort -> bkClientPort))
        .routes(Map(nodeName -> CommonUtils.address(nodeName)))
        .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
        .volumeMaps(Map(volume.key.toPlain -> dataFolder))
        .arguments(bkArguments)
        .create()
    )
  }

  @After
  def tearDown(): Unit = Releasable.close(containerClient)
}

object TestMountVolume {
  @Parameters(name = "{index} mode = {0}")
  def parameters: java.util.Collection[ContainerPlatform] = {
    val modes = ContainerPlatform.all
    if (modes.isEmpty) java.util.List.of(ContainerPlatform.empty)
    else modes.asJava
  }
}
