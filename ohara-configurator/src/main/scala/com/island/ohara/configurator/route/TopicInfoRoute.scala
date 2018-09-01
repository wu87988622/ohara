package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._

private[configurator] object TopicInfoRoute {
  private[this] def toRes(uuid: String, request: TopicInfoRequest) =
    TopicInfo(uuid, request.name, request.numberOfPartitions, request.numberOfReplications, System.currentTimeMillis())

  def apply(implicit store: Store, uuidGenerator: () => String, kafkaClient: KafkaClient): server.Route =
    pathPrefix(TOPIC_INFO_PATH) {
      pathEnd {
        // add
        post {
          entity(as[TopicInfoRequest]) { req =>
            val topicInfo = toRes(uuidGenerator(), req)
            if (kafkaClient.exist(topicInfo.uuid))
              // this should be impossible....
              throw new IllegalArgumentException(s"The topic:${topicInfo.uuid} exists")
            else {
              kafkaClient.topicCreator
              // NOTED: we use the uuid to create topic since we allow user to change the topic name arbitrary
                .topicName(topicInfo.uuid)
                .numberOfPartitions(topicInfo.numberOfPartitions)
                .numberOfReplications(topicInfo.numberOfReplications)
                .build()
              store.add(topicInfo.uuid, topicInfo)
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
            kafkaClient.deleteTopic(uuid)
            complete(d)
          } ~
          // update
          put {
            entity(as[TopicInfoRequest]) { req =>
              val newTopicInfo = toRes(uuid, req)
              assertNotRelated2RunningPipeline(uuid)
              val oldData = store.data[TopicInfo](uuid)
              if (oldData.numberOfReplications != newTopicInfo.numberOfReplications)
                throw new IllegalArgumentException("Non-support to change the number of replications")
              if (oldData.numberOfPartitions != newTopicInfo.numberOfPartitions)
                kafkaClient.addPartition(uuid, newTopicInfo.numberOfPartitions)
              store.update(uuid, newTopicInfo)
              complete(newTopicInfo)
            }
          }
      }
    }
}
