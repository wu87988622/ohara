package com.island.ohara.source.http

import java.util.concurrent.ConcurrentHashMap

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.island.ohara.data.Row
import com.island.ohara.kafka.Producer
import com.island.ohara.serialization.Serializer
import org.apache.kafka.clients.producer.ProducerConfig

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

/**
  * This actor uses for start or stop Akka-HTTP at runtime.
  * We will initialize Kafka Producer and Akka-HTTP server at this actor.
  */
class HttpConnectorActor extends Actor with ActorLogging {

  private var server: Future[ServerBinding] = _
  private var producer: Producer[String, Row] = _
  private var schemaMap: ConcurrentHashMap[String, (String, RowSchema)] = _
  private implicit val system = context.system
  private implicit var materializer: ActorMaterializer = _
  private implicit var executor: ExecutionContextExecutor = _
  private var start: Boolean = false

  override def receive: Receive = {
    case HttpCommand.START(config) => {
      log.info("COMMAND: Start HTTP Server")
      if (!start) {
        try {
          materializer = ActorMaterializer()
          executor = system.dispatcher

          producer = Producer
            .builder(Serializer.STRING, Serializer.ROW)
            .brokers(config.getStringList(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).asScala.mkString(","))
            .allAcks()
            .build()

          schemaMap = new ConcurrentHashMap[String, (String, RowSchema)]()

          val webRoute = new WebApp(producer, schemaMap).route

          server = Http().bindAndHandle(webRoute, config.getString("http.interface"), config.getInt("http.port"))
          start = true
        } catch {
          case e: Exception => {
            log.error(e.toString)
            start = false

            if (server != null) {
              Await.result(server.flatMap(_.unbind()), 3 seconds)
            }
            if (schemaMap != null) {
              schemaMap = null
            }
            if (producer != null) {
              producer.close()
            }
            if (executor != null) {
              executor = null
            }
            if (materializer != null) {
              materializer.shutdown()
              materializer = null
            }
          }
        }
      } else {
        log.warning("Http Server already started")
      }
    }

    case HttpCommand.STOP => {
      log.info("COMMAND: Stop HTTP Server")

      if (start) {
        try {
          Await.result(server.flatMap(_.unbind()), 3 seconds)
          materializer.shutdown()
        } finally {
          producer.flush()
          producer.close
        }
        server = null
        materializer = null
        producer = null
        schemaMap = null
        executor = null
        start = false
      } else {
        log.warning("Http Server has already been shutdown")
      }
    }
  }
}
