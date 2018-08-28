package com.island.ohara.configurator.call

import scala.concurrent.duration.Duration

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
case class CallQueueRequest(uuid: String, lease: Duration)
