package com.island.ohara.connector.perf

import java.time.Duration

import com.island.ohara.client.{ConnectorClient, ConnectorJson}
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.OharaTestUtil

import scala.concurrent.duration._
object PerfUtil {
  private[this] val TIMEOUT = Duration.ofSeconds(60)
  def assertFailedConnector(testUtil: OharaTestUtil, name: String): Unit = CommonUtil.await(
    () =>
      try ConnectorClient(testUtil.workersConnProps).status(name).connector.state == ConnectorJson.State.FAILED
      catch {
        case _: Throwable => false
    },
    TIMEOUT)

  def checkConnector(testUtil: OharaTestUtil, name: String): Unit = {
    CommonUtil.await(() => ConnectorClient(testUtil.workersConnProps).activeConnectors().contains(name), TIMEOUT)
    CommonUtil.await(() =>
                       try ConnectorClient(testUtil.workersConnProps).status(name).connector.state == State.RUNNING
                       catch {
                         case _: Throwable => false
                     },
                     TIMEOUT)
  }
}
