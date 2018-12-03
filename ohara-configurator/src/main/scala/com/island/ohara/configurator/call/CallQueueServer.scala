package com.island.ohara.configurator.call

import com.island.ohara.common.util.CloseOnce

import scala.concurrent.duration.Duration

/**
  * a call queue server is used to handle the request sent from client. All received task are buffered in the server,
  * and the server developer should call #take to get the undealt task, and then process it with a good response or a exception.
  */
trait CallQueueServer[Request, Response] extends CloseOnce {

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
