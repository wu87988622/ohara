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

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
  * a helper class to build the call queue client. Excluding the #brokers and #topicName, other arguments
  * are optional. TODO: introduce a way to highlight the required arguments and optional arguments.
  */
class CallQueueClientBuilder private[call] {

  private[this] var brokers: Option[String] = None
  private[this] var requestTopic: Option[String] = None
  private[this] var responseTopic: Option[String] = None
  private[this] var pollTimeout: Option[Duration] = Some(CallQueue.DEFAULT_POLL_TIMEOUT)
  private[this] var expirationCleanupTime: Option[Duration] = Some(CallQueue.DEFAULT_EXPIRATION_CLEANUP_TIME)

  /**
    * set the kafka brokers information.
    * @param brokers kafka brokers
    * @return this builder
    */
  def brokers(brokers: String): CallQueueClientBuilder = {
    this.brokers = Some(brokers)
    this
  }

  /**
    * set the topic used to send request
    * @param requestTopic topic name
    * @return this builder
    */
  def requestTopic(requestTopic: String): CallQueueClientBuilder = {
    this.requestTopic = Some(requestTopic)
    this
  }

  /**
    * set the topic used to receive response
    * @param responseTopic topic name
    * @return this builder
    */
  def responseTopic(responseTopic: String): CallQueueClientBuilder = {
    this.responseTopic = Some(responseTopic)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(pollTimeout: Duration): CallQueueClientBuilder = {
    this.pollTimeout = Some(pollTimeout)
    this
  }

  /**
    * set the time to clear the expired request.
    * @param expirationCleanupTime time to clear the expired request. in milliseconds
    * @return this builder
    */
  def expirationCleanupTime(expirationCleanupTime: Duration): CallQueueClientBuilder = {
    this.expirationCleanupTime = Some(expirationCleanupTime)
    this
  }

  /**
    * construct the call queue client
    * @tparam REQUEST the type from request
    * @tparam RESPONSE the type from response
    * @return a call queue client implementation
    */
  def build[REQUEST <: AnyRef, RESPONSE: ClassTag](): CallQueueClient[REQUEST, RESPONSE] =
    CallQueueClientImpl[REQUEST, RESPONSE](
      brokers.get,
      requestTopic.get,
      responseTopic.get,
      pollTimeout.get,
      expirationCleanupTime.get
    )
}
