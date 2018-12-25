package com.island.ohara.connector.perf

import java.time.Duration

import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.data.connector.ConnectorState
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.OharaTestUtil
object PerfUtil {
  private[this] val TIMEOUT = Duration.ofSeconds(60)
  def assertFailedConnector(testUtil: OharaTestUtil, name: String): Unit = CommonUtil.await(
    () => {
      val client = ConnectorClient(testUtil.workersConnProps)
      try client.status(name).connector.state == ConnectorState.FAILED
      catch {
        case _: Throwable => false
      } finally client.close()
    },
    TIMEOUT
  )

  def checkConnector(testUtil: OharaTestUtil, name: String): Unit = {
    CommonUtil.await(
      () => {
        val connectorClient = ConnectorClient(testUtil.workersConnProps)
        try connectorClient.activeConnectors().contains(name)
        finally connectorClient.close()
      },
      TIMEOUT
    )
    CommonUtil.await(
      () => {
        val connectorClient = ConnectorClient(testUtil.workersConnProps)
        try connectorClient.status(name).connector.state == ConnectorState.RUNNING
        catch {
          case _: Throwable => false
        } finally connectorClient.close()
      },
      TIMEOUT
    )
  }
}
