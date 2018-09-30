package com.island.ohara.connector.ftp
import com.island.ohara.client.ConnectorJson
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.integration.OharaTestUtil

import scala.concurrent.duration._
object TestFtpUtil {
  private[this] val TIMEOUT = 60 seconds
  def assertFailedConnector(testUtil: OharaTestUtil, name: String): Unit = OharaTestUtil.await(
    () =>
      try testUtil.connectorClient.status(name).connector.state == ConnectorJson.State.FAILED
      catch {
        case _: Throwable => false
    },
    TIMEOUT)

  def checkConnector(testUtil: OharaTestUtil, name: String): Unit = {
    OharaTestUtil.await(() => testUtil.connectorClient.activeConnectors().contains(name), TIMEOUT)
    OharaTestUtil.await(() =>
                          try testUtil.connectorClient.status(name).connector.state == State.RUNNING
                          catch {
                            case _: Throwable => false
                        },
                        TIMEOUT)
  }
}
