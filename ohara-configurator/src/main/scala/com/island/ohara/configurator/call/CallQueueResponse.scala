package com.island.ohara.configurator.call

/**
  * a internal-purposed ohara data. see CallQueueServerImpl and CallQueueClientImpl for more details.
  * @param config stores all properties
  */
case class CallQueueResponse(uuid: String, reqId: String)
