package com.island.ohara.connector.ftp
import java.time.Duration

import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.OharaTestUtil
object FtpUtil {
  private[this] val TIMEOUT = Duration.ofSeconds(60)
  def assertFailedConnector(testUtil: OharaTestUtil, name: String): Unit = CommonUtil.await(
    () => {
      val connectorClient = ConnectorClient(testUtil.workersConnProps)
      try connectorClient.status(name).connector.state == ConnectorState.FAILED
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
        try connectorClient.status(name).connector.state == ConnectorState.RUNNING
        catch {
          case _: Throwable => false
        } finally connectorClient.close()
      },
      TIMEOUT
    )
  }
}
