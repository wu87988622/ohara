package com.island.ohara.connector.ftp
import java.time.Duration

import com.island.ohara.client.{ConnectorClient, ConnectorJson}
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.OharaTestUtil

import scala.concurrent.duration._
object FtpUtil {
  private[this] val TIMEOUT = Duration.ofSeconds(60)
  def assertFailedConnector(testUtil: OharaTestUtil, name: String): Unit = CommonUtil.await(
    () => {
      val connectorClient = ConnectorClient(testUtil.workersConnProps)
      try connectorClient.status(name).connector.state == ConnectorJson.State.FAILED
      catch {
        case _: Throwable => false
      } finally connectorClient.close()
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
        try connectorClient.status(name).connector.state == State.RUNNING
        catch {
          case _: Throwable => false
        } finally connectorClient.close()
      },
      TIMEOUT
    )
  }
}
