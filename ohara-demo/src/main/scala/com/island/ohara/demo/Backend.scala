package com.island.ohara.demo
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.{ConnectorClient, DatabaseClient}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.{LocalDataBase, OharaTestUtil}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration._

/**
  * run a configurator based on 3 brokers and 3 workers.
  */
object Backend {
  final case class Creation(name: String, schema: Seq[RdbColumn])
  implicit val CREATION_JSON_FORMAT: RootJsonFormat[Creation] = jsonFormat2(Creation)
  val HELP_KEY = "--help"
  val PORT_KEY = "--port"
  val TTL_KEY = "--ttl"
  val USAGE = s"[Usage] $TTL_KEY $PORT_KEY"
  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0) == HELP_KEY) {
      println(USAGE)
      System.exit(1)
    }
    if (args.length % 2 != 0) {
      println(USAGE)
      System.exit(1)
    }
    // TODO: make the parse more friendly
    var ttl: Duration = 365 days
    var port: Int = 0
    args.sliding(2, 2).foreach {
      case Array(PORT_KEY, value) => port = value.toInt
      case Array(TTL_KEY, value)  => ttl = value.toInt seconds
      case _                      => throw new IllegalArgumentException(USAGE)
    }
    run(
      port,
      (configurator, _) => {
        println(s"run a configurator at ${configurator.hostname}:${configurator.port}")
        println(
          s"enter ctrl+c to terminate all processes (or all processes will be terminated after ${ttl.toSeconds} seconds")
        TimeUnit.SECONDS.sleep(ttl.toSeconds)
      }
    )
  }

  def run(port: Int, stopped: (Configurator, LocalDataBase) => Unit): Unit = {
    doClose(OharaTestUtil.localWorkers(3, 3)) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokers} and workers:${util.workers}")
      println(
        s"Succeed to run a database url:${util.dataBase.url} user:${util.dataBase.user} password:${util.dataBase.password}")
      val dbRoute: server.Route = path("creation" / "rdb") {
        pathEnd {
          post {
            entity(as[Creation]) { creation =>
              complete {
                val client = DatabaseClient(util.dataBase.url, util.dataBase.user, util.dataBase.password)
                try {
                  client.createTable(creation.name, creation.schema)
                } finally client.close()
                StatusCodes.OK
              }
            }
          }
        }
      }
      val topicName = s"demo-${System.currentTimeMillis()}"
      val configurator = Configurator
        .builder()
        .store(
          Store
            .builder()
            .brokers(util.brokers)
            .topicName(topicName)
            .numberOfReplications(1)
            .numberOfPartitions(1)
            .build[String, Any])
        .kafkaClient(KafkaClient(util.brokers))
        .connectClient(ConnectorClient(util.workers))
        .hostname("0.0.0.0")
        .port(port)
        .extraRoute(dbRoute)
        .build()
      try stopped(configurator, util.dataBase)
      finally configurator.close()
    }
  }
}
