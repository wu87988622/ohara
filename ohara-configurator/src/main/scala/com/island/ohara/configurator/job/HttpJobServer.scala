package com.island.ohara.configurator.job

import com.island.ohara.config.OharaConfig
import com.island.ohara.configurator.call.{CallQueue, CallQueueServer, CallQueueTask}
import com.island.ohara.io.CloseOnce

import scala.concurrent.duration.Duration

class HttpJobServer(brokers: String, groupId: String, topicName: String, topicOptions: Map[String, String] = Map())
    extends CloseOnce {

  private[this] val server: CallQueueServer[HttpJobRequest, HttpJobResponse] = CallQueue.serverBuilder
    .brokers(brokers)
    .groupId(groupId)
    .topicName(topicName)
    .topicOptions(topicOptions)
    .build[HttpJobRequest, HttpJobResponse]()

  def countOfUndealtTasks: Int = server.countOfUndealtTasks

  def take(timeout: Duration): Option[CallQueueTask[HttpJobRequest, HttpJobResponse]] = server.take(timeout)

  def take(): CallQueueTask[HttpJobRequest, HttpJobResponse] = server.take()

  override protected def doClose(): Unit = server.close()
}
