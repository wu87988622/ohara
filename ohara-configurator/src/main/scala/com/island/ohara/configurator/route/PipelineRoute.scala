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
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorDescription
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.StreamApi.StreamAppDescription
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils.{Id, TargetCluster}
import com.island.ohara.configurator.store.DataStore
import com.island.ohara.kafka.connector.json.SettingDefinitions
import com.island.ohara.metrics.basic.CounterMBean
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
private[configurator] object PipelineRoute {

  /**
    * this constant represents the "unknown" from or "unknown" to.
    */
  private[this] val UNKNOWN_ID: String = "?"
  private[this] val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(id: String, request: PipelineCreationRequest, swallow: Boolean = false)(
    implicit workerCollie: WorkerCollie,
    store: DataStore,
    executionContext: ExecutionContext): Future[Pipeline] =
    toRes(Map(id -> request), swallow).map(_.head)

  /**
    * convert the request to response.
    * NOTED: it includes all checks to request.
    * @param swallow true if you don't want to see the exception in checking.
    * @return response
    */
  private[this] def toRes(reqs: Map[String, PipelineCreationRequest], swallow: Boolean)(
    implicit workerCollie: WorkerCollie,
    store: DataStore,
    executionContext: ExecutionContext): Future[Seq[Pipeline]] =
    workerCollie.clusters
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
              flows = request.flows,
              objects = Seq.empty,
              workerClusterName = wkName,
              lastModified = CommonUtils.current()
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
                verifyFlows(pipeline.id, pipeline.flows, cluster).map { flows =>
                  pipeline.copy(flows = flows) -> Some(cluster)
              })
              .getOrElse(Future.successful(pipeline -> None))
        }))
      .flatMap(entries =>
        // if the backend worker cluster is gone, we don't do any checks for this pipeline
        Future.sequence(entries.map {
          case (pipeline, clusterOption) =>
            clusterOption
              .map(cluster =>
                abstracts(
                  pipeline.rules,
                  workerCollie.workerClient(cluster),
                  try workerCollie.counters(cluster)
                  catch {
                    // TODO: We can endure the lack of metrics? ... by chia
                    case _: Throwable => Seq.empty
                  }
                ).map(objects => pipeline.copy(objects = objects)))
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
  private[this] def abstracts(rules: Map[String, Seq[String]], workerClient: WorkerClient, counters: Seq[CounterMBean])(
    implicit store: DataStore,
    executionContext: ExecutionContext): Future[List[ObjectAbstract]] =
    Future
      .sequence(
        rules
          .flatMap {
            case (k, v) => Seq(k) ++ v
          }
          .filterNot(_ == UNKNOWN_ID)
          .toSet
          .map(id => store.value[Data](id)))
      .flatMap(objs => workerClient.connectors.map(_ -> objs))
      .flatMap {
        case (connectors, objs) =>
          Future.traverse(objs) {
            case data: ConnectorDescription =>
              // the group of counter is equal to connector's name (this is a part of kafka's core setting)
              // Hence, we filter the connectors having different "name" (we use id instead of name in creating connector)
              val metrics = Metrics(counters.filter(_.group() == data.id).map { counter =>
                CounterInfo(
                  value = counter.getValue,
                  unit = counter.getUnit,
                  document = counter.getDocument,
                  startTime = counter.getStartTime
                )
              })
              workerClient
                .exist(data.id)
                .flatMap(if (_) workerClient.status(data.id).map(Some(_)) else Future.successful(None))
                .map { connectorInfo =>
                  connectorInfo -> SettingDefinitions.kind(
                    connectors
                      .find(_.className == data.className)
                      .getOrElse(throw new NoSuchElementException(s"${data.className} doesn't exist"))
                      .definitions
                      .asJava)
                }
                .map {
                  case (connectorInfo, kind) =>
                    ObjectAbstract(
                      id = data.id,
                      name = data.name,
                      kind = kind,
                      className = Some(data.className),
                      state = connectorInfo.map(_.connector.state.name),
                      error = connectorInfo.flatMap(_.connector.trace),
                      metrics = metrics,
                      lastModified = data.lastModified
                    )
                }
                .recover {
                  case e: Throwable =>
                    LOG.error(s"Failed to get status of connector:${data.id}", e)
                    ObjectAbstract(
                      id = data.id,
                      name = data.name,
                      kind = data.kind,
                      className = None,
                      state = None,
                      error = Some(s"Failed to get status and type of connector:${data.id}." +
                        s"This may be temporary since our worker cluster is too busy to sync status of connector. ${e.getMessage}"),
                      metrics = metrics,
                      lastModified = data.lastModified
                    )
                }

            case data: StreamAppDescription =>
              Future.successful(
                ObjectAbstract(
                  id = data.id,
                  name = data.name,
                  kind = data.kind,
                  className = None,
                  state = data.state,
                  error = None,
                  metrics = Metrics(Seq.empty),
                  lastModified = data.lastModified
                ))

            case data =>
              Future.successful(
                ObjectAbstract(id = data.id,
                               name = data.name,
                               kind = data.kind,
                               className = None,
                               state = None,
                               error = None,
                               metrics = Metrics(Seq.empty),
                               lastModified = data.lastModified))
          }
      }
      // NOTED: we have to return a "serializable" list!!!
      .map(_.toList)

  /**
    * we should accept following data type only
    * [ConnectorConfiguration, TopicInfo, StreamApp]
    */
  private[this] def verifyFlows(name: String, flows: Seq[Flow], cluster: WorkerClusterInfo)(
    implicit store: DataStore,
    executionContext: ExecutionContext): Future[Seq[Flow]] = {

    // pipeline is bound on specific worker cluster. And all objects in this pipeline should be bound on same cluster.
    // for example:
    // topic -> the broker cluster must be bound by same worker cluster
    // connector -> it's worker cluster must be same to pipeline's worker cluster
    // streamapp -> TODO: it should be bound by worker cluster after issue #321 ...by Sam
    // others -> unsupported
    def verify(id: String): Future[String] = if (id != UNKNOWN_ID) {
      store
        .raw(id)
        .map {
          case d: ConnectorDescription =>
            if (d.workerClusterName != cluster.name)
              throw new IllegalArgumentException(
                s"connector:${d.name} is run by ${d.workerClusterName} so it can't be placed at pipeline:$name which is placed at worker cluster:${cluster.name}")
            else id
          case d: TopicInfo =>
            if (d.brokerClusterName != cluster.brokerClusterName)
              throw new IllegalArgumentException(
                s"topic:${d.name} is run by ${d.brokerClusterName} so it can't be placed at pipeline:$name which is placed at broker cluster:${cluster.brokerClusterName}")
            else id
          case _: StreamAppDescription => id
          case raw                     => throw new IllegalArgumentException(s"${raw.getClass.getName} can't be placed at pipeline")
        }
        .recover {
          // the component has been removed!
          case e: NoSuchElementException =>
            LOG.error(s"$id had been removed", e)
            UNKNOWN_ID
        }
    } else Future.successful(id)

    // filter out illegal rules. the following rules are illegal.
    // 1) "a": ["a"] => this case will cause a exception
    // 2) unknown -> others => this will be removed
    // 3) unknown -> ["unknown", others] => the "unknown" in value will be removed
    def verify2(ids: Seq[String]): Future[Seq[String]] = Future.traverse(ids)(verify)
    Future
      .sequence(
        flows
        // pre-filter the unknown key
          .filter(_.from != UNKNOWN_ID)
          .map { flow =>
            verify(flow.from).flatMap { from =>
              // we will remove unknown key later so it is unnecessary to fetch object for values.
              if (from == UNKNOWN_ID)
                Future.successful(
                  Flow(
                    from = from,
                    to = Seq.empty
                  ))
              else
                verify2(flow.to).map { to =>
                  if (to.size == 1 && to.head == from)
                    throw new IllegalArgumentException(s"the from:$from can't be equals to to:${to.head}")
                  Flow(from = from, to = to)
                }
            }
          })
      .map(_.filter(_.from != UNKNOWN_ID).map(
        flow =>
          Flow(
            from = flow.from,
            to = flow.to.filter(_ != UNKNOWN_ID),
        )))
  }

  private[this] def update(pipeline: Pipeline)(implicit store: DataStore,
                                               workerCollie: WorkerCollie,
                                               executionContext: ExecutionContext): Future[Pipeline] =
    update(Seq(pipeline)).map(_.head)

  /**
    * update the response.
    * Noted: it swallows the exception since it is possible that the backed worker cluster is gone.
    */
  private[this] def update(pipelines: Seq[Pipeline])(implicit store: DataStore,
                                                     workerCollie: WorkerCollie,
                                                     executionContext: ExecutionContext): Future[Seq[Pipeline]] =
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

  /**
    * throw exception if request has invalid ids
    */
  private[this] def assertNoUnknown(req: PipelineCreationRequest)(
    implicit store: DataStore,
    executionContext: ExecutionContext): Future[PipelineCreationRequest] =
    Future
      .traverse(req.flows.flatMap(f => Seq(f.from) ++ f.to).toSet) { id =>
        store.exist[Data](id).map(if (_) None else Some(id))
      }
      .map(_.flatten.toSeq)
      .map { invalidIds =>
        if (invalidIds.isEmpty) req
        else throw new IllegalArgumentException(s"$invalidIds don't exist!!!")
      }

  def apply(implicit store: DataStore, workerCollie: WorkerCollie, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[PipelineCreationRequest, Pipeline](
      root = PIPELINES_PREFIX_PATH,
      hookOfAdd = (t: TargetCluster, id: Id, request: PipelineCreationRequest) =>
        assertNoUnknown(request).flatMap(checkedRequest => toRes(id, updateWorkerClusterName(checkedRequest, t))),
      hookOfUpdate = (id: Id, request: PipelineCreationRequest, previous: Pipeline) =>
        assertNoUnknown(request).flatMap(checkedRequest =>
          toRes(id, checkedRequest.copy(workerClusterName = Some(previous.workerClusterName)))),
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
                      .filter(_.isInstanceOf[ConnectorDescription])
                      .map(_.id)
                      .map(store.remove[ConnectorDescription]))
                  .map(_ => pipeline.id)
              }
        },
      hookOfDelete = (response: Pipeline) => Future.successful(response)
    )
}
