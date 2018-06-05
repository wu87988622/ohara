package com.island.ohara.source.http

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cakesolutions.kafka.KafkaProducer
import cakesolutions.kafka.KafkaProducer.Conf
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.serialization.RowSerializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.serialization.StringSerializer

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * The entry point of Ohara-HTTP
  * TODO: Based on the requirement, this needs to be need to be rewritten.
  * TODO: OharaHTTP main process will ack like a daemon.
  * TODO: KafkaRoute need to stay with other instance.
  * KafkaRoute need to be at the other class to be run at multiple HTTP instances
  */
object OharaHttp extends KafkaRoute {

  val logger = Logger[this.type]

  implicit val system: ActorSystem = ActorSystem("OharaHttpConnector")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private def isFileExist(path: String): Boolean = {
    val file = new File(path)
    if (file.exists() && file.isFile)
      true
    else false
  }

  def main(args: Array[String]): Unit = {
    val pathStr = args(0)

    val config =
      if (Option(args(0)).forall(_.isEmpty))
        ConfigFactory.load
      else {
        if (!isFileExist(pathStr)) {
          throw new RuntimeException(s"$pathStr does not exist or it's a directory!")
        }
        ConfigFactory.load.atPath(pathStr)
      }

    val producer = KafkaProducer(
      Conf(
        new StringSerializer,
        KafkaUtil.wrapSerializer(RowSerializer),
        bootstrapServers = config.getStringList("kafka-broker").asScala.mkString(",")
      )
    )

    def healthyCheck = pathSingleSlash {
      get {
        complete("Alive")
      }
    }

    val schemaMap = new ConcurrentHashMap[String, (String, RowSchema)]()

    val route = healthyCheck ~ kafkaRoute(producer, schemaMap)

    system.scheduler.schedule(5 seconds, 5 seconds) {
      // TODO: pull schema information to local
    }

    // TODO: Based on requirement, this will be load at runtime instead of startup time.
    val server = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))

    sys.addShutdownHook(
      server.flatMap(_.unbind()).onComplete {
        case Success(_) => {
          materializer.shutdown()
          system.terminate()
          producer.close
        }
        case Failure(e) => logger.error(s"Didn't close resources successfully: $e")
      }
    )
  }
}
