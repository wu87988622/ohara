package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._

private[configurator] object TopicInfoRoute {
  private[this] def toRes(uuid: String, request: TopicInfoRequest) =
    TopicInfo(uuid, request.name, request.numberOfPartitions, request.numberOfReplications, CommonUtil.current())

  def apply(implicit store: Store, uuidGenerator: () => String, kafkaClient: KafkaClient): server.Route =
    pathPrefix(TOPIC_INFO_PATH) {
      pathEnd {
        // add
        post {
          entity(as[TopicInfoRequest]) { req =>
            val topicInfo = toRes(uuidGenerator(), req)
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
              store.add(topicInfo)
              complete(topicInfo)
            }
          }
        } ~ get(complete(store.data[TopicInfo].toSeq)) // list
      } ~ path(Segment) { uuid =>
        // get
        get(complete(store.data[TopicInfo](uuid))) ~
          // delete
          delete {
            assertNotRelated2Pipeline(uuid)
            val d = store.remove[TopicInfo](uuid)
            if (kafkaClient.exist(uuid)) kafkaClient.deleteTopic(uuid)
            complete(d)
          } ~
          // update
          put {
            entity(as[TopicInfoRequest]) { req =>
              val newTopicInfo = toRes(uuid, req)
              val oldData = store.data[TopicInfo](uuid)
              if (oldData.numberOfReplications != newTopicInfo.numberOfReplications)
                throw new IllegalArgumentException("Non-support to change the number from replications")
              if (oldData.numberOfPartitions != newTopicInfo.numberOfPartitions)
                kafkaClient.addPartitions(uuid, newTopicInfo.numberOfPartitions)
              store.update(newTopicInfo)
              complete(newTopicInfo)
            }
          }
      }
    }
}
