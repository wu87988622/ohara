package com.island.ohara.connector.jdbc

import com.island.ohara.integration.With3Brokers3Workers
import org.junit.Test
import org.scalatest.Matchers

/**
  * Test the JDBC Source Connector
  */
class TestJDBCSourceConnector extends With3Brokers3Workers with Matchers {
  private[this] val connectorClient = testUtil.connectorClient

  @Test
  def testJDBCSourceConnector(): Unit = {
    val connectorName: String = "JDBC-Source-Connector-Test"
    val topicName: String = "topic-test-1"

    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[JDBCSourceConnector])
      .topic(topicName)
      .numberOfTasks(1)
      .create()

    //TODO Get Topic message for test
  }
}
