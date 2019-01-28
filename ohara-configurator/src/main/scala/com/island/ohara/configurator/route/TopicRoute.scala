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
import com.island.ohara.agent.BrokerCollie
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
private[configurator] object TopicRoute {

  private[this] val LOG = Logger(TopicRoute.getClass)
  def apply(implicit store: Store, brokerCollie: BrokerCollie): server.Route =
    RouteUtil.basicRoute[TopicCreationRequest, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      hookOfAdd = (targetCluster: TargetCluster, id: Id, request: TopicCreationRequest) =>
        CollieUtils.topicAdmin(targetCluster).flatMap {
          case (cluster, client) =>
            client
              .creator()
              // NOTED: we allow user to change topic's name arbitrarily
              .name(id)
              .numberOfPartitions(request.numberOfPartitions)
              .numberOfReplications(request.numberOfReplications)
              .create()
              .map { info =>
                try TopicInfo(id,
                              request.name,
                              info.numberOfPartitions,
                              info.numberOfReplications,
                              cluster.name,
                              CommonUtil.current())
                finally client.close()
              }
      },
      hookOfUpdate = (id: Id, request: TopicCreationRequest, previous: TopicInfo) =>
        CollieUtils.topicAdmin(Some(previous.brokerClusterName)).flatMap {
          case (cluster, client) =>
            if (previous.numberOfPartitions != request.numberOfPartitions)
              client.changePartitions(id, request.numberOfPartitions).map { info =>
                try TopicInfo(info.name,
                              request.name,
                              info.numberOfPartitions,
                              info.numberOfReplications,
                              cluster.name,
                              CommonUtil.current())
                finally client.close()
              } else if (previous.numberOfReplications != request.numberOfReplications) {
              // we have got to release the client
              Releasable.close(client)
              Future.failed(new IllegalArgumentException("Non-support to change the number from replications"))
            } else {
              // we have got to release the client
              Releasable.close(client)
              Future.successful(previous)
            }
      },
      hookOfDelete = (response: TopicInfo) =>
        CollieUtils.topicAdmin(Some(response.brokerClusterName)).map {
          case (_, client) =>
            try client.delete(response.id)
            catch {
              case e: Throwable =>
                LOG.error(s"failed to remove topic:${response.id} from kafka", e)
            } finally Releasable.close(client)
            response
      },
      hookOfGet = (response: TopicInfo) => Future.successful(response),
      hookOfList = (responses: Seq[TopicInfo]) => Future.successful(responses)
    )
}
