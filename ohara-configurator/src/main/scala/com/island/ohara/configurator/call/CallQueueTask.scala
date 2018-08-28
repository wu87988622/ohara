package com.island.ohara.configurator.call

/**
  * the task means a undealt request. the guy who have a task should generate the response and pass it to task through Task#complete.
  * After task get a response (or a exception), the server is going to send the response back to client.
  */
trait CallQueueTask[REQUEST, RESPONSE] {

  /**
    * @return the request data sent by client
    */
  val request: REQUEST

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
