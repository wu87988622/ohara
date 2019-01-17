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
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._
import com.island.ohara.kafka.BrokerClient

private[configurator] object TopicsRoute {
  private[this] def toRes(id: String, request: TopicCreationRequest) =
    TopicInfo(id, request.name, request.numberOfPartitions, request.numberOfReplications, CommonUtil.current())

  def apply(implicit store: Store, brokerClient: BrokerClient): server.Route =
    RouteUtil.basicRoute[TopicCreationRequest, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      hookOfAdd = (id: String, request: TopicCreationRequest) => {
        val topicInfo = toRes(id, request)
        if (brokerClient.exist(topicInfo.id))
          // this should be impossible....
          throw new IllegalArgumentException(s"The topic:${topicInfo.id} exists")
        else {
          brokerClient
            .topicCreator()
            .numberOfPartitions(topicInfo.numberOfPartitions)
            .numberOfReplications(topicInfo.numberOfReplications)
            // NOTED: we use the uuid to create topic since we allow user to change the topic name arbitrary
            .create(topicInfo.id)
          topicInfo
        }
      },
      hookOfUpdate = (id: String, request: TopicCreationRequest, previous: TopicInfo) => {
        if (previous.numberOfReplications != request.numberOfReplications)
          throw new IllegalArgumentException("Non-support to change the number from replications")
        if (previous.numberOfPartitions != request.numberOfPartitions)
          brokerClient.addPartitions(id, request.numberOfPartitions)
        toRes(id, request)
      },
      hookOfDelete = (response: TopicInfo) => response,
      hookOfGet = (response: TopicInfo) => response,
      hookBeforeDelete = (id: String) => {
        assertNotRelated2Pipeline(id)
        if (brokerClient.exist(id)) brokerClient.deleteTopic(id)
        id
      },
      hookOfList = (responses: Seq[TopicInfo]) => responses
    )
}
