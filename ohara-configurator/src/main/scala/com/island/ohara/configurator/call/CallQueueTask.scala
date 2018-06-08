package com.island.ohara.configurator.call

import com.island.ohara.configurator.data.OharaData

/**
  * the task means a undealt request. the guy who have a task should generate the response and pass it to task through Task#complete.
  * After task get a response (or a exception), the server is going to send the response back to client.
  */
trait CallQueueTask[REQUEST <: OharaData, RESPONSE <: OharaData] {

  /**
    * @return the request data sent by client
    */
  val request: REQUEST

  /**
    * Use a OharaData as the return object
    * @param oharaData return object
    */
  def complete(oharaData: RESPONSE): Unit

  /**
    * Use a exception as the return object
    * @param exception return object
    */
  def complete(exception: Throwable): Unit
}
