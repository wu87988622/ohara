package com.island.ohara.configurator.call

import scala.concurrent.duration._

/**
  * call queue is used to implement the client/server request/response arch. The default impl is based on kafka topic.
  */
object CallQueue {
  def clientBuilder = new CallQueueClientBuilder
  def serverBuilder = new CallQueueServerBuilder
  val DEFAULT_REPLICATION_NUMBER: Short = 1
  val DEFAULT_PARTITION_NUMBER: Int = 1
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_POLL_TIMEOUT: Duration = 5 seconds
  val DEFAULT_EXPIRATION_TIME: Duration = 10 seconds
  val DEFAULT_EXPIRATION_CLEANUP_TIME: Duration = 60 seconds
  val EXPIRED_REQUEST_EXCEPTION = new InterruptedException("The request is expired")
  val TERMINATE_TIMEOUT_EXCEPTION = new InterruptedException(
    "The request/response is interrupted since the client/server is closing")

}
