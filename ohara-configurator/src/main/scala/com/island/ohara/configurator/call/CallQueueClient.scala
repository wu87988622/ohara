package com.island.ohara.configurator.call

import com.island.ohara.client.ConfiguratorJson.Error
import com.island.ohara.io.CloseOnce

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Call queue client is designed for sending the request(sync) and receive the response(async).
  *
  * @tparam Request request type
  * @tparam Response response type
  */
trait CallQueueClient[Request, Response] extends CloseOnce {

  /**
    * send the request and then wait the response.
    * An TimeoutException will be thrown if fail to receive the response within the timeout period
    * @param request the request
    * @param timeout the valid duration of this request
    * @return a true response or a exception
    */
  def request(request: Request, timeout: Duration = CallQueue.DEFAULT_EXPIRATION_TIME): Future[Either[Error, Response]]
}

object CallQueueClient {
  def builder() = new CallQueueClientBuilder()
}
