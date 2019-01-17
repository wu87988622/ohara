/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator.call

import com.island.ohara.common.util.ReleaseOnce

import com.island.ohara.client.configurator.v0.ErrorApi._
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Call queue client is designed for sending the request(sync) and receive the response(async).
  *
  * @tparam Request request type
  * @tparam Response response type
  */
trait CallQueueClient[Request, Response] extends ReleaseOnce {

  /**
    * send the request and then wait the response.
    * An TimeoutException will be thrown if fail to receive the response within the timeout period
    * @param request the request
    * @param timeout the valid duration from this request
    * @return a true response or a exception
    */
  def request(request: Request, timeout: Duration = CallQueue.DEFAULT_EXPIRATION_TIME): Future[Either[Error, Response]]
}

object CallQueueClient {
  def builder() = new CallQueueClientBuilder()
}
