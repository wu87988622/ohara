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
import com.island.ohara.agent.{NoSuchClusterException, WorkerCollie}
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.StreamApi.StreamApp
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil.{Id, TargetCluster}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
private[configurator] object PipelineRoute {
  private[this] val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(id: String, request: PipelineCreationRequest, swallow: Boolean = false)(
    implicit workerCollie: WorkerCollie,
    store: Store): Future[Pipeline] =
    toRes(Map(id -> request), swallow).map(_.head)

  /**
    * convert the request to response.
    * NOTED: it includes all checks to request.
    * @param swallow true if you don't want to see the exception in checking.
    * @return response
    */
  private[this] def toRes(reqs: Map[String, PipelineCreationRequest],
                          swallow: Boolean)(implicit workerCollie: WorkerCollie, store: Store): Future[Seq[Pipeline]] =
    workerCollie
      .clusters()
      .map { clusters =>
        reqs.map {
          case (id, request) =>
            // we must find a name for pipeline even if the name is not mapped to active worker cluster
            val wkName = request.workerClusterName.getOrElse {
              if (clusters.size == 1) clusters.head._1.name
              else
                throw new IllegalStateException(
                  s"can't match default worker cluster from ${clusters.map(_._1.name).mkString(",")}")
            }
            val wkCluster = clusters.keys.find(_.name == wkName)
            if (!swallow && wkCluster.isEmpty)
              throw new IllegalArgumentException(s"failed to find matched worker cluster. " +
                s"${request.workerClusterName.map(n => s"request:$n").getOrElse("")} actual:${clusters.map(_._1.name).mkString(",")}")
            Pipeline(
              id = id,
              name = request.name,
              rules = request.rules,
              objects = Seq.empty,
              workerClusterName = wkName,
              lastModified = CommonUtil.current()
            ) -> wkCluster
        }
      }
      .map(_.toMap)
      .flatMap(entries =>
        // if the backend worker cluster is gone, we don't do any checks for this pipeline
        Future.sequence(entries.map {
          case (pipeline, clusterOption) =>
            clusterOption
              .map(cluster =>
                verifyRules(pipeline.id, pipeline.rules, cluster).map { rules =>
                  pipeline.copy(rules = rules) -> Some(cluster)
              })
              .getOrElse(Future.successful(pipeline -> None))
        }))
      .flatMap(entries =>
        // if the backend worker cluster is gone, we don't do any checks for this pipeline
        Future.sequence(entries.map {
          case (pipeline, clusterOption) =>
            clusterOption
              .map(cluster =>
                abstracts(pipeline.rules, workerCollie.workerClient(cluster)).map(objects =>
                  pipeline.copy(objects = objects)))
              .getOrElse(Future.successful(pipeline))
        }))
      .map(_.toSeq)

  /**
    * generate the description of all objects hosted by pipeline
    * @param rules pipeline's rules
    * @param workerClient used to communicate to the worker cluster running the pipeline
    * @param store store
    * @return description of objects
    */
  private[this] def abstracts(rules: Map[String, Seq[String]], workerClient: WorkerClient)(
    implicit store: Store): Future[List[ObjectAbstract]] =
    Future
      .sequence(
        rules
          .flatMap {
            case (k, v) => Seq(k) ++ v
          }
          .filterNot(_ == UNKNOWN)
          .toSet
          .map(id => store.value[Data](id)))
      .flatMap { objs =>
        Future.traverse(objs) {
          case data: ConnectorInfo =>
            workerClient
              .status(data.id)
              .map(c => Some(c.connector.state))
              .recover {
                case e: Throwable =>
                  LOG.error(s"Failed to get stats of connector:${data.id}", e)
                  None
              }
              .map { state =>
                ObjectAbstract(data.id,
                               data.name,
                               data.kind,
                               state,
                               ConnectorRoute.errorMessage(state),
                               data.lastModified)
              }
          case data: StreamApp =>
            Future.successful(ObjectAbstract(data.id, data.name, data.kind, data.state, None, data.lastModified))

          case data => Future.successful(ObjectAbstract(data.id, data.name, data.kind, None, None, data.lastModified))
        }
      }
      // NOTED: we have to return a "serializable" list!!!
      .map(_.toList)

  /**
    * we should accept following data type only
    * [ConnectorConfiguration, TopicInfo, StreamApp]
    */
  private[this] def verifyRules(name: String, rules: Map[String, Seq[String]], cluster: WorkerClusterInfo)(
    implicit store: Store): Future[Map[String, Seq[String]]] = {

    // pipeline is bound on specific worker cluster. And all objects in this pipeline should be bound on same cluster.
    // for example:
    // topic -> the broker cluster must be bound by same worker cluster
    // connector -> it's worker cluster must be same to pipeline's worker cluster
    // streamapp -> TODO: it should be bound by worker cluster after issue #321 ...by Sam
    // others -> unsupported
    def verify(id: String): Future[String] = if (id != UNKNOWN) {
      store
        .raw(id)
        .map {
          case d: ConnectorInfo =>
            if (d.workerClusterName != cluster.name)
              throw new IllegalArgumentException(
                s"connector:${d.name} is run by ${d.workerClusterName} so it can't be placed at pipeline:$name which is placed at worker cluster:${cluster.name}")
            else id
          case d: TopicInfo =>
            if (d.brokerClusterName != cluster.brokerClusterName)
              throw new IllegalArgumentException(
                s"topic:${d.name} is run by ${d.brokerClusterName} so it can't be placed at pipeline:$name which is placed at broker cluster:${cluster.brokerClusterName}")
            else id
          case _: StreamApp => id
          case raw          => throw new IllegalArgumentException(s"${raw.getClass.getName} can't be placed at pipeline")
        }
        .recover {
          // the component has been removed!
          case e: NoSuchElementException =>
            LOG.error(s"$id had been removed", e)
            UNKNOWN
        }
    } else Future.successful(id)

    // filter out illegal rules. the following rules are illegal.
    // 1) "a": ["a"] => this case will cause a exception
    // 2) unknown -> others => this will be removed
    // 3) unknown -> ["unknown", others] => the "unknown" in value will be removed
    def verify2(ids: Seq[String]): Future[Seq[String]] = Future.traverse(ids)(verify)
    Future
      .sequence(
        rules
        // pre-filter the unknown key
          .filter {
            case (k, _) => k != UNKNOWN
          }
          .map {
            case (k, v) =>
              verify(k).flatMap { k =>
                // we will remove unknown key later so it is unnecessary to fetch object for values.
                if (k == UNKNOWN) Future.successful(k -> Seq.empty)
                else
                  verify2(v).map { v =>
                    if (v.size == 1 && v.head == k)
                      throw new IllegalArgumentException(s"the from:$k can't be equals to to:${v.head}")
                    k -> v
                  }
              }
          })
      .map(_.toMap
        .filter {
          // if key is unknown, we remove it
          case (k, _) => k != UNKNOWN
        }
        .map {
          // remove unknown from value
          case (k, ids) => k -> ids.filter(_ != UNKNOWN)
        })
  }

  private[this] def update(pipeline: Pipeline)(implicit store: Store, workerCollie: WorkerCollie): Future[Pipeline] =
    update(Seq(pipeline)).map(_.head)

  /**
    * update the response.
    * Noted: it swallows the exception since it is possible that the backed worker cluster is gone.
    */
  private[this] def update(pipelines: Seq[Pipeline])(implicit store: Store,
                                                     workerCollie: WorkerCollie): Future[Seq[Pipeline]] =
    toRes(
      pipelines.map { pipeline =>
        pipeline.id -> PipelineCreationRequest(
          name = pipeline.name,
          workerClusterName = Some(pipeline.workerClusterName),
          rules = pipeline.rules
        )
      }.toMap,
      true
    )

  /**
    * TODO: remove TargetCluster. see https://github.com/oharastream/ohara/issues/206
    */
  private[this] def updateWorkerClusterName(request: PipelineCreationRequest,
                                            t: TargetCluster): PipelineCreationRequest =
    if (request.workerClusterName.isEmpty) request.copy(workerClusterName = t) else request

  def apply(implicit store: Store, workerCollie: WorkerCollie): server.Route =
    RouteUtil.basicRoute[PipelineCreationRequest, Pipeline](
      root = PIPELINES_PREFIX_PATH,
      hookOfAdd =
        (t: TargetCluster, id: Id, request: PipelineCreationRequest) => toRes(id, updateWorkerClusterName(request, t)),
      hookOfUpdate = (id: Id, request: PipelineCreationRequest, previous: Pipeline) =>
        toRes(id, request.copy(workerClusterName = Some(previous.workerClusterName))),
      hookOfGet = (response: Pipeline) => update(response),
      hookOfList = (responses: Seq[Pipeline]) => update(responses),
      hookBeforeDelete = (id: String) =>
        store
          .value[Pipeline](id)
          .flatMap { pipeline =>
            update(pipeline).recover {
              // keep working even through the wk cluster is gone.
              case _: NoSuchClusterException => pipeline
            }
          }
          .flatMap { pipeline =>
            // If any object has "state", we reject to delete pipeline. We can't stop all objects at once.
            val running = pipeline.objects.filter(_.state.isDefined).map(_.id)
            if (running.nonEmpty) Future.failed(new IllegalArgumentException(s"${running.mkString(",")} are running"))
            else
              Future.sequence(pipeline.objects.map(_.id).map(store.value[Data])).flatMap { objs =>
                Future
                  .sequence(
                    objs
                    // we only remove connectors. The streamapps and topics are still stored!
                      .filter(_.isInstanceOf[ConnectorInfo])
                      .map(_.id)
                      .map(store.remove[ConnectorInfo]))
                  .map(_ => pipeline.id)
              }
        },
      hookOfDelete = (response: Pipeline) => Future.successful(response)
    )
}
