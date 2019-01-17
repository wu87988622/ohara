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

/**
  * the task means a undealt request. the guy who have a task should generate the response and pass it to task through Task#complete.
  * After task get a response (or a exception), the server is going to send the response back to client.
  */
trait CallQueueTask[REQUEST, RESPONSE] {

  /**
    * @return the request data sent by client
    */
  def request: REQUEST

  /**
    * Use a response as the return object
    * @param response return object
    */
  def complete(response: RESPONSE): Unit

  /**
    * Use a exception as the return object
    * @param exception return object
    */
  def complete(exception: Throwable): Unit
}
