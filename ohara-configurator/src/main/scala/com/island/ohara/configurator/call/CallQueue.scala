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
