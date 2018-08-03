package com.island.ohara.rest
import com.island.ohara.io.CloseOnce

/**
  * a helper class used to send the rest request to kafka worker.
  */
trait ConnectorClient extends CloseOnce {

  def sourceCreator(): SourceConnectorCreator

  def sinkCreator(): SinkConnectorCreator

  //TODO: Convert the returned tyoe from string to POJO...by chia
  def delete(name: String): RestResponse

  def existPlugin(name: String): Boolean = listPlugins().body.contains(name)

  //TODO: Convert the returned tyoe from string to POJO...by chia
  def listPlugins(): RestResponse

  def existConnector(name: String): Boolean = listConnectors().body.contains(name)

  //TODO: Convert the returned tyoe from string to POJO...by chia
  def listConnectors(): RestResponse
}

object ConnectorClient {

  def apply(workers: String): ConnectorClient = {
    val workerAddress = workers
      .split(",")
      .map(s => {
        val ss = s.split(":")
        (ss(0), ss(1).toInt)
      })
    if (workerAddress.isEmpty) throw new IllegalArgumentException(s"Invalid workers:$workers")
    new ConnectorClient() {
      private[this] val restClient = BoundRestClient(workerAddress(0)._1, workerAddress(0)._2)
      override def sourceCreator(): SourceConnectorCreator = SourceConnectorCreator(restClient)
      override def sinkCreator(): SinkConnectorCreator = SinkConnectorCreator(restClient)
      override def delete(name: String): RestResponse = restClient.delete(s"connectors/$name")
      override protected def doClose(): Unit = restClient.close()
      override def listPlugins(): RestResponse = restClient.get("connector-plugins")
      override def listConnectors(): RestResponse = restClient.get("connectors")
    }
  }
  def apply(host: String, port: Int): ConnectorClient = apply(s"$host:$port")
}
