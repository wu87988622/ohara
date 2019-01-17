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

import scala.concurrent.duration.Duration

/**
  * a call queue server is used to handle the request sent from client. All received task are buffered in the server,
  * and the server developer should call #take to get the undealt task, and then process it with a good response or a exception.
  */
trait CallQueueServer[Request, Response] extends ReleaseOnce {

  /**
    * get and remove the latest undealt task.
    * @param timeout how long to wait before giving up, in units from milliseconds
    * @return None if specified waiting time elapses before an undealt task is available. Otherwise, a undealt task
    */
  def take(timeout: Duration): Option[CallQueueTask[Request, Response]]

  /**
    * get and remove the latest undealt task. This method will be blocker until there is a undealt task
    * @return a undealt task
    */
  def take(): CallQueueTask[Request, Response]

  /**
    * @return number from undealt task
    */
  def countOfUndealtTasks: Int

  /**
    * @return number from processing task
    */
  def countOfProcessingTasks: Int
}

object CallQueueServer {
  def builder() = new CallQueueServerBuilder()
}
