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

package com.island.ohara.it.connector

import java.sql.{Statement, Timestamp}

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.connector.jdbc.source.{JDBCSourceConnector, JDBCSourceConnectorConfig}
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.Consumer.Record
import com.island.ohara.kafka.connector.TaskSetting
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import org.junit.{After, Before, Test}

abstract class BasicTestJDBCSourceConnector extends BasicTestConnectorCollie {
  private[this] val log = Logger(classOf[BasicTestJDBCSourceConnector])
  private[this] val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), "JDBC-Source-Connector-Test")
  private[this] val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))

  private[this] val tableName: String = s"table_${CommonUtils.randomString(10)}"
  private[this] val timestampColumn: String = "column1"
  private[this] var client: DatabaseClient = _

  protected[this] val DB_URL_KEY: String = "ohara.it.db.url"
  protected[this] val DB_USER_NAME_KEY: String = "ohara.it.db.username"
  protected[this] val DB_PASSWORD_KEY: String = "ohara.it.db.password"

  protected def dbUrl(): Option[String]
  protected def dbUserName(): Option[String]
  protected def dbPassword(): Option[String]
  protected def dataBaseClient(): DatabaseClient
  protected def checkClusterInfo(): Unit

  protected val cleanup: Boolean = true

  @Before
  final def setup(): Unit = {
    checkClusterInfo()

    //Check db info
    if (dbUrl.isEmpty || dbUserName.isEmpty || dbPassword.isEmpty)
      skipTest(
        s"skip postgresql jdbc source connector test, Please setting $DB_URL_KEY, $DB_USER_NAME_KEY and $DB_PASSWORD_KEY properties")

    client = dataBaseClient()

    val columnName1 = "column1"
    val columnName2 = "column2"
    val columnName3 = "column3"

    val column1 = RdbColumn(columnName1, "TIMESTAMP", false)
    val column2 = RdbColumn(columnName2, "varchar(45)", false)
    val column3 = RdbColumn(columnName3, "integer", true)
    client.createTable(tableName, Seq(column1, column2, column3))

    val statement: Statement = client.connection.createStatement()

    (1 to 100).foreach(i => {
      statement.executeUpdate(
        s"INSERT INTO $tableName($columnName1, $columnName2, $columnName3) VALUES('2018-09-01 00:00:00', 'a${i}', ${i})")
    })
  }

  @Test
  def testJDBCSourceConnector(): Unit = {
    val zkCluster = result(
      zk_create(
        clusterName = generateClusterName(),
        clientPort = CommonUtils.availablePort(),
        electionPort = CommonUtils.availablePort(),
        peerPort = CommonUtils.availablePort(),
        nodeNames = Set(nodeCache.head.name)
      ))
    result(zk_start(zkCluster.name))
    assertCluster(() => result(zk_clusters()), () => result(zk_containers(zkCluster.name)), zkCluster.name)
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(zk_containers(zkCluster.name))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
    val bkCluster = result(
      bk_create(
        clusterName = generateClusterName(),
        clientPort = CommonUtils.availablePort(),
        exporterPort = CommonUtils.availablePort(),
        jmxPort = CommonUtils.availablePort(),
        zkClusterName = zkCluster.name,
        nodeNames = Set(nodeCache.head.name)
      ))
    result(bk_start(bkCluster.name))
    assertCluster(() => result(bk_clusters()), () => result(bk_containers(bkCluster.name)), bkCluster.name)
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(bk_containers(bkCluster.name))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
    log.info("[WORKER] start to test worker")
    val nodeName = nodeCache.head.name
    val clusterName = generateClusterName()
    result(wk_exist(clusterName)) shouldBe false
    log.info("[WORKER] verify:nonExists done")
    val clientPort = CommonUtils.availablePort()
    val jmxPort = CommonUtils.availablePort()

    log.info("[WORKER] create ...")
    val wkCluster = result(
      wk_create(
        clusterName = clusterName,
        clientPort = clientPort,
        jmxPort = jmxPort,
        bkClusterName = bkCluster.name,
        nodeNames = Set(nodeName)
      ))

    log.info("[WORKER] create done")
    log.info("[WORKER] verify:create done")
    result(wk_exist(wkCluster.name)) shouldBe true
    log.info("[WORKER] verify:exist done")
    // we can't assume the size since other tests may create zk cluster at the same time
    result(wk_clusters()).isEmpty shouldBe false
    log.info("[WORKER] verify:list done")
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(wk_containers(clusterName))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
    result(wk_containers(clusterName)).foreach { container =>
      container.nodeName shouldBe nodeName
      container.name.contains(clusterName) shouldBe true
      container.hostname.contains(clusterName) shouldBe true
      // [BEFORE] ClusterCollieImpl applies --network=host to all worker containers so there is no port mapping.
      // The following checks are disabled rather than deleted since it seems like a bug if we don't check the port mapping.
      // [AFTER] ClusterCollieImpl use bridge network now
      container.portMappings.head.portPairs.size shouldBe 2
      container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
      container.environments.exists(_._2 == clientPort.toString) shouldBe true
    }
    val logs = result(wk_logs(clusterName))
    logs.size shouldBe 1
    logs.foreach(log =>
      withClue(log) {
        log.contains("- ERROR") shouldBe false
        // we cannot assume "k8s get logs" are complete since log may rotate
        // so log could be empty in k8s environment
        // also see : https://github.com/kubernetes/kubernetes/issues/11046#issuecomment-121140315
    })
    log.info("[WORKER] verify:log done")

    Thread.sleep(20000L)

    runningJDBCSourceConnector(wkCluster.connectionProps)
    checkTopicData(bkCluster.connectionProps, topicKey.topicNameOnKafka())
  }

  private[this] def runningJDBCSourceConnector(workerConnProps: String): Unit = {
    val workerClient = WorkerClient(workerConnProps)
    result(
      workerClient
        .connectorCreator()
        .connectorKey(connectorKey)
        .connectorClass(classOf[JDBCSourceConnector])
        .topicKey(topicKey)
        .numberOfTasks(1)
        .settings(props.toMap)
        .create())
  }

  private[this] def checkTopicData(brokers: String, topicNameOnKafka: String): Unit = {
    val consumer =
      Consumer
        .builder[Row, Array[Byte]]()
        .topicName(topicNameOnKafka)
        .offsetFromBegin()
        .connectionProps(brokers)
        .keySerializer(Serializer.ROW)
        .valueSerializer(Serializer.BYTES)
        .build()
    try {
      val record: Seq[Record[Row, Array[Byte]]] = consumer.poll(java.time.Duration.ofSeconds(50), 100).asScala
      record.size shouldBe 100

      record.head.key.get.cell(0).value.asInstanceOf[Timestamp].getTime shouldBe 1535760000000L
      record.head.key.get.cell(1).value shouldBe "a1"
      record.head.key.get.cell(2).value shouldBe 1

      record.last.key.get.cell(0).value.asInstanceOf[Timestamp].getTime shouldBe 1535760000000L
      record.last.key.get.cell(1).value shouldBe "a100"
      record.last.key.get.cell(2).value shouldBe 100
    } finally {
      consumer.close()
    }
  }

  private[this] def props(): JDBCSourceConnectorConfig = {
    JDBCSourceConnectorConfig(
      TaskSetting.of(Map(
        "source.db.url" -> dbUrl.get,
        "source.db.username" -> dbUserName.get,
        "source.db.password" -> dbPassword.get,
        "source.table.name" -> tableName,
        "source.timestamp.column.name" -> timestampColumn
      ).asJava))
  }

  @After
  def afterTest(): Unit = {
    Releasable.close(client)
    Releasable.close(clusterCollie)
    if (cleanup) Releasable.close(nameHolder)
  }

  private[this] def generateClusterName(): String = nameHolder.generateClusterName()
}
