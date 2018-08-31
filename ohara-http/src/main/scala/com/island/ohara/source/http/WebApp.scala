package com.island.ohara.source.http

import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import com.island.ohara.data.Row
import com.island.ohara.kafka.Producer

/**
  * Use for creating the center of the routing directive
  *
  * @param producer Producer
  * @param schemaMap
  * @param actorSystem
  */
class WebApp(producer: Producer[String, Row], schemaMap: ConcurrentHashMap[String, (String, RowSchema)])(
  implicit actorSystem: ActorSystem)
    extends KafkaRoute {

  implicit def system: ActorSystem = actorSystem

  private def healthyCheck = pathSingleSlash {
    get {
      complete("Alive")
    }
  }

  def route: Route = healthyCheck ~ kafkaRoute(producer, schemaMap)

}
