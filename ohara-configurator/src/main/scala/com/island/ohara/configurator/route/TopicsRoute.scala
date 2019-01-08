package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._
import com.island.ohara.kafka.KafkaClient

private[configurator] object TopicsRoute {
  private[this] def toRes(id: String, request: TopicCreationRequest) =
    TopicInfo(id, request.name, request.numberOfPartitions, request.numberOfReplications, CommonUtil.current())

  def apply(implicit store: Store, kafkaClient: KafkaClient): server.Route =
    RouteUtil.basicRoute[TopicCreationRequest, TopicInfo](
      root = TOPICS_PREFIX_PATH,
      hookOfAdd = (id: String, request: TopicCreationRequest) => {
        val topicInfo = toRes(id, request)
        if (kafkaClient.exist(topicInfo.id))
          // this should be impossible....
          throw new IllegalArgumentException(s"The topic:${topicInfo.id} exists")
        else {
          kafkaClient
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
          kafkaClient.addPartitions(id, request.numberOfPartitions)
        toRes(id, request)
      },
      hookOfDelete = (response: TopicInfo) => response,
      hookOfGet = (response: TopicInfo) => response,
      hookBeforeDelete = (id: String) => {
        assertNotRelated2Pipeline(id)
        if (kafkaClient.exist(id)) kafkaClient.deleteTopic(id)
        id
      },
      hookOfList = (responses: Seq[TopicInfo]) => responses
    )
}
