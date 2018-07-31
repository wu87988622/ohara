package com.island.ohara.source.http

import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import com.island.ohara.data.Row
import org.apache.kafka.clients.producer.KafkaProducer

/**
  * Use for creating the center of the routing directive
  *
  * @param producer KafkaProducer
  * @param schemaMap
  * @param actorSystem
  */
class WebApp(producer: KafkaProducer[String, Row], schemaMap: ConcurrentHashMap[String, (String, RowSchema)])(
  implicit actorSystem: ActorSystem)
    extends KafkaRoute {

  implicit def system = actorSystem

  private def healthyCheck = pathSingleSlash {
    get {
      complete("Alive")
    }
  }

  def route = healthyCheck ~ kafkaRoute(producer, schemaMap)

}
