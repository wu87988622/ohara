package com.island.ohara.source.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cakesolutions.kafka.KafkaProducer
import cakesolutions.kafka.KafkaProducer.Conf
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringSerializer

/**
  * The entry point of Ohara-HTTP
  */
object OharaHttp {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem()
    implicit val executor = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val config = ConfigFactory.load

    val producer = KafkaProducer(
      Conf(
        new StringSerializer, new StringSerializer,
        // TODO: move to config file
        bootstrapServers = "jimin-cdk-30-1.is-land.taipei:9092, jimin-cdk-30-2.is-land.taipei:9092, jimin-cdk-30-3.is-land.taipei:9092, jimin-cdk-30-4.is-land.taipei:9092"
      )
    )

    Http().bindAndHandle(new WebService(system, producer).route, config.getString("http.interface"), config.getInt("http.port"))

    sys.addShutdownHook(producer.close)
  }
}
