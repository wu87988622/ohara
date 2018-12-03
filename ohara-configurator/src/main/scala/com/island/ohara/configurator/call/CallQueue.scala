package com.island.ohara.configurator.call

import scala.concurrent.duration._

/**
  * call queue is used to implement the client/server request/response arch. The default impl is based on kafka topic.
  */
object CallQueue {
  def clientBuilder(): CallQueueClientBuilder = new CallQueueClientBuilder
  def serverBuilder(): CallQueueServerBuilder = new CallQueueServerBuilder
  private[call] val DEFAULT_POLL_TIMEOUT: Duration = 5 seconds
  private[call] val DEFAULT_EXPIRATION_TIME: Duration = 10 seconds
  private[call] val DEFAULT_EXPIRATION_CLEANUP_TIME: Duration = 60 seconds
  private[call] val EXPIRED_REQUEST_EXCEPTION = new InterruptedException("The request is expired")
  private[call] val TERMINATE_TIMEOUT_EXCEPTION = new InterruptedException(
    "The request/response is interrupted since the client/server is closing")
}
