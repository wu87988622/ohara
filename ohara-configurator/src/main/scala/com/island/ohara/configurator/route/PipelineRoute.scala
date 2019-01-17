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

package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.client.WorkerClient
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

import scala.concurrent.Await
import scala.concurrent.duration._
private[configurator] object PipelineRoute {
  private[this] def toRes(id: String, request: PipelineCreationRequest)(implicit store: Store,
                                                                        workerClient: WorkerClient) =
    Pipeline(id, request.name, request.rules, abstracts(request.rules), CommonUtil.current())

  private[this] def checkExist(ids: Set[String])(implicit store: Store): Unit = {
    ids.foreach(
      uuid =>
        if (Await.result(store.nonExist[Data](uuid), 10 seconds))
          throw new IllegalArgumentException(s"the uuid:$uuid does not exist"))
  }

  private[this] def abstracts(rules: Map[String, String])(implicit store: Store,
                                                          workerClient: WorkerClient): Seq[ObjectAbstract] = {
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
                         if (workerClient.exist(data.id)) Some(workerClient.status(data.id).connector.state)
                         else None,
                         data.lastModified)
        case data => ObjectAbstract(data.id, data.name, data.kind, None, data.lastModified)
      }
      .toList // NOTED: we have to return a "serializable" list!!!
  }

  private[this] def verifyRules(pipeline: Pipeline)(implicit store: Store): Pipeline = {
    def verify(id: String): Unit = if (id != UNKNOWN) {
      val data = Await.result(store.raw(id), 10 seconds)
      if (!data.isInstanceOf[ConnectorConfiguration] && !data.isInstanceOf[TopicInfo])
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

  private[this] def update(pipeline: Pipeline)(implicit store: Store, workerClient: WorkerClient): Pipeline =
    pipeline.copy(objects = abstracts(pipeline.rules))

  def apply(implicit store: Store, workerClient: WorkerClient): server.Route =
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
