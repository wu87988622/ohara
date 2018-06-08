package com.island.ohara.configurator.job

import com.island.ohara.config.OharaConfig
import com.island.ohara.configurator.call.CallQueue
import com.island.ohara.configurator.data.OharaException
import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.DataType

import scala.concurrent.Future

class HttpJobClient(brokers: String, topicName: String, config: OharaConfig = OharaConfig()) extends CloseOnce {

  private[this] val client = CallQueue.clientBuilder
    .brokers(brokers)
    .topicName(topicName)
    .configuration(config)
    .build[HttpJobRequest, HttpJobResponse]()

  def request(job: HttpJobRequest): Future[Either[OharaException, HttpJobResponse]] =
    client.request(job)

  def request(action: Action,
              path: String,
              schema: Map[String, DataType],
              config: Map[String, String]): Future[Either[OharaException, HttpJobResponse]] =
    request(HttpJobRequest.apply(action, path, schema, config))

  override protected def doClose(): Unit = client.close()
}
