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
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
private[configurator] object TopicRoute {
  def apply(implicit store: Store, brokerCollie: BrokerCollie): server.Route =
    RouteUtil.basicRoute[TopicCreationRequest, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      hookOfAdd = (targetCluster: TargetCluster, id: Id, request: TopicCreationRequest) =>
        CollieUtils.brokerClient(targetCluster).map {
          case (cluster, client) =>
            try {
              val topicInfo = TopicInfo(id,
                                        request.name,
                                        request.numberOfPartitions,
                                        request.numberOfReplications,
                                        cluster.name,
                                        CommonUtil.current())
              if (client.exist(topicInfo.id))
                // this should be impossible....
                throw new IllegalArgumentException(s"The topic:${topicInfo.id} exists")
              else
                client
                  .topicCreator()
                  .numberOfPartitions(topicInfo.numberOfPartitions)
                  .numberOfReplications(topicInfo.numberOfReplications)
                  // NOTED: we use the id to create topic since we allow user to change the topic name arbitrary
                  .create(topicInfo.id)
              topicInfo
            } finally client.close()
      },
      hookOfUpdate = (id: Id, request: TopicCreationRequest, previous: TopicInfo) =>
        CollieUtils.brokerClient(Some(previous.brokerClusterName)).map {
          case (cluster, bkClient) =>
            try {
              if (previous.numberOfReplications != request.numberOfReplications)
                throw new IllegalArgumentException("Non-support to change the number from replications")
              if (previous.numberOfPartitions != request.numberOfPartitions)
                bkClient.addPartitions(id, request.numberOfPartitions)
              TopicInfo(id,
                        request.name,
                        request.numberOfPartitions,
                        request.numberOfReplications,
                        cluster.name,
                        CommonUtil.current())
            } finally bkClient.close()
      },
      hookOfDelete = (response: TopicInfo) =>
        CollieUtils.brokerClient(Some(response.brokerClusterName)).map {
          case (_, bkClient) =>
            if (bkClient.exist(response.id)) bkClient.deleteTopic(response.id)
            response
      },
      hookOfGet = (response: TopicInfo) => Future.successful(response),
      hookOfList = (responses: Seq[TopicInfo]) => Future.successful(responses)
    )
}
