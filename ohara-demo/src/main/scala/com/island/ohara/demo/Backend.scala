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
import com.island.ohara.integration.{Database, FtpServer, OharaTestUtil}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.util.SystemUtil
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration._

/**
  * run a configurator based on 3 brokers and 3 workers.
  */
object Backend {
  final case class Creation(name: String, schema: Seq[RdbColumn])
  implicit val CREATION_JSON_FORMAT: RootJsonFormat[Creation] = jsonFormat2(Creation)

  final case class DbInformation(url: String, user: String, password: String)
  implicit val DB_INFO_JSON_FORMAT: RootJsonFormat[DbInformation] = jsonFormat3(DbInformation)

  final case class FtpServerInformation(host: String, port: Int, user: String, password: String)
  implicit val FTP_SERVER_JSON_FORMAT: RootJsonFormat[FtpServerInformation] = jsonFormat4(FtpServerInformation)

  final case class Services(ftpServer: FtpServerInformation, database: DbInformation)
  implicit val SERVICES_JSON_FORMAT: RootJsonFormat[Services] = jsonFormat2(Services)

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
      (configurator, _, _) => {
        println(s"run a configurator at ${configurator.hostname}:${configurator.port}")
        println(
          s"enter ctrl+c to terminate all processes (or all processes will be terminated after ${ttl.toSeconds} seconds")
        TimeUnit.SECONDS.sleep(ttl.toSeconds)
      }
    )
  }

  def run(port: Int, stopped: (Configurator, Database, FtpServer) => Unit): Unit = {
    doClose(OharaTestUtil.workers()) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokersConnProps} and workers:${util.workersConnProps}")
      println(
        s"Succeed to run a database url:${util.dataBase.url} user:${util.dataBase.user} password:${util.dataBase.password}")
      println(
        s"Succeed to run a ftp server host:${util.ftpServer.host} port:${util.ftpServer.port} user:${util.ftpServer.user} password:${util.ftpServer.password}")
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
      val servicesRoute: server.Route = path("services") {
        pathEnd {
          get {
            complete {
              Services(
                ftpServer = FtpServerInformation(host = util.ftpServer.host,
                                                 port = util.ftpServer.port,
                                                 user = util.ftpServer.user,
                                                 password = util.ftpServer.password),
                database = DbInformation(
                  url = util.dataBase.url,
                  user = util.dataBase.user,
                  password = util.dataBase.password
                )
              )
            }
          }
        }
      }
      val topicName = s"demo-${SystemUtil.current()}"
      doClose(KafkaClient(util.brokersConnProps))(
        _.topicCreator().numberOfPartitions(3).numberOfReplications(3).compacted().create(topicName)
      )
      val configurator = Configurator
        .builder()
        .store(Store.builder().brokers(util.brokersConnProps).topicName(topicName).buildBlocking[String, Any])
        .kafkaClient(KafkaClient(util.brokersConnProps))
        .connectClient(ConnectorClient(util.workersConnProps))
        .hostname("0.0.0.0")
        .port(port)
        .extraRoute(dbRoute ~ servicesRoute)
        .build()
      try stopped(configurator, util.dataBase, util.ftpServer)
      finally configurator.close()
    }
  }
}
