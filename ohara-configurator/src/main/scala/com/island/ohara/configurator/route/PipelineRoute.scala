package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.TopicApi.TopicDescription
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

import scala.concurrent.Await
import scala.concurrent.duration._
private[configurator] object PipelineRoute {
  private[this] def toRes(id: String, request: PipelineCreationRequest)(implicit store: Store,
                                                                        connectorClient: ConnectorClient) =
    Pipeline(id, request.name, request.rules, abstracts(request.rules), CommonUtil.current())

  private[this] def checkExist(ids: Set[String])(implicit store: Store): Unit = {
    ids.foreach(
      uuid =>
        if (Await.result(store.nonExist[Data](uuid), 10 seconds))
          throw new IllegalArgumentException(s"the uuid:$uuid does not exist"))
  }

  private[this] def abstracts(rules: Map[String, String])(implicit store: Store,
                                                          connectorClient: ConnectorClient): Seq[ObjectAbstract] = {
    val keys = rules.keys.filterNot(_ == UNKNOWN).toSet
    checkExist(keys)
    val values = rules.values.filterNot(_ == UNKNOWN).toSet
    checkExist(values)
    Await
      .result(store.raw(), 30 seconds)
      .filter(data => keys.contains(data.id) || values.contains(data.id))
      .map {
        case data: ConnectorConfiguration =>
          ObjectAbstract(data.id,
                         data.name,
                         data.kind,
                         if (connectorClient.exist(data.id)) Some(connectorClient.status(data.id).connector.state)
                         else None,
                         data.lastModified)
        case data => ObjectAbstract(data.id, data.name, data.kind, None, data.lastModified)
      }
      .toList // NOTED: we have to return a "serializable" list!!!
  }

  private[this] def verifyRules(pipeline: Pipeline)(implicit store: Store): Pipeline = {
    def verify(id: String): Unit = if (id != UNKNOWN) {
      val data = Await.result(store.raw(id), 10 seconds)
      if (!data.isInstanceOf[ConnectorConfiguration] && !data.isInstanceOf[TopicDescription])
        throw new IllegalArgumentException(s"""${data.getClass.getName} can't be placed at "from"""")
    }
    pipeline.rules.foreach {
      case (k, v) =>
        if (k == v) throw new IllegalArgumentException(s"the from:$k can't be equals to to:$v")
        verify(k)
        verify(v)
    }
    pipeline
  }

  private[this] def update(pipeline: Pipeline)(implicit store: Store, connectorClient: ConnectorClient): Pipeline =
    pipeline.copy(objects = abstracts(pipeline.rules))

  def apply(implicit store: Store, connectorClient: ConnectorClient): server.Route =
    RouteUtil.basicRoute[PipelineCreationRequest, Pipeline](
      root = PIPELINES_PREFIX_PATH,
      hookOfAdd = (id: String, request: PipelineCreationRequest) => verifyRules(toRes(id, request)),
      hookOfUpdate = (id: String, request: PipelineCreationRequest, _: Pipeline) => verifyRules(toRes(id, request)),
      hookOfGet = (response: Pipeline) => update(response),
      hookOfList = (responses: Seq[Pipeline]) => responses.map(update),
      hookBeforeDelete = (id: String) => id,
      hookOfDelete = (response: Pipeline) => response
    )
}
