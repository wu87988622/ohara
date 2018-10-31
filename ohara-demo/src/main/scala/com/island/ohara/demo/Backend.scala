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
import com.island.ohara.integration._
import com.island.ohara.io.CloseOnce._
import com.island.ohara.io.IoUtil
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

  final case class FtpServerInformation(hostname: String, port: Int, dataPort: Int, user: String, password: String)
  implicit val FTP_SERVER_JSON_FORMAT: RootJsonFormat[FtpServerInformation] = jsonFormat5(FtpServerInformation)

  final case class Services(zookeeper: String,
                            brokers: String,
                            workers: String,
                            ftpServer: FtpServerInformation,
                            database: DbInformation)
  implicit val SERVICES_JSON_FORMAT: RootJsonFormat[Services] = jsonFormat5(Services)

  val HELP_KEY = "--help"
  val CONFIGURATOR_PORT_KEY = "--configuratorPort"
  val ZOOKEEPER_PORT_KEY = "--zkPort"
  val BROKERS_PORT_KEY = "--brokersPort"
  val WORKERS_PORT_KEY = "--workersPort"
  val DB_PORT_KEY = "--dbPort"
  val FTP_PORT_KEY = "--ftpPort"
  val FTP_DATA_PORT_KEY = "--ftpDataPort"
  val TTL_KEY = "--ttl"
  val USAGE =
    s"[Usage] $TTL_KEY $CONFIGURATOR_PORT_KEY $ZOOKEEPER_PORT_KEY $BROKERS_PORT_KEY $WORKERS_PORT_KEY $DB_PORT_KEY $FTP_PORT_KEY $FTP_DATA_PORT_KEY"

  final case class ServicePorts(configuratorPort: Int,
                                dbPort: Int,
                                ftpPort: Int,
                                ftpDataPort: Int,
                                workersPort: Seq[Int],
                                brokersPort: Seq[Int],
                                zkPort: Int)

  object ServicePorts {
    val default = ServicePorts(
      configuratorPort = 0,
      dbPort = 0,
      ftpPort = 0,
      ftpDataPort = 0,
      workersPort = Seq.fill(3)(0),
      brokersPort = Seq.fill(3)(0),
      zkPort = 0
    )
  }

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
    var configuratorPort: Int = 0
    var zkPort: Int = 0
    var brokersPort: Seq[Int] = Seq.fill(3)(0)
    var workersPort: Seq[Int] = Seq.fill(3)(0)
    var dbPort: Int = 0
    var ftpPort: Int = 0
    var ftpDataPort: Int = 0
    args.sliding(2, 2).foreach {
      case Array(CONFIGURATOR_PORT_KEY, value) => configuratorPort = value.toInt
      case Array(ZOOKEEPER_PORT_KEY, value)    => zkPort = value.toInt
      case Array(BROKERS_PORT_KEY, value)      => brokersPort = value.split(",").map(_.toInt)
      case Array(WORKERS_PORT_KEY, value)      => workersPort = value.split(",").map(_.toInt)
      case Array(DB_PORT_KEY, value)           => dbPort = value.toInt
      case Array(FTP_PORT_KEY, value)          => ftpPort = value.toInt
      case Array(FTP_DATA_PORT_KEY, value)     => ftpDataPort = value.toInt
      case Array(TTL_KEY, value)               => ttl = value.toInt seconds
      case _                                   => throw new IllegalArgumentException(USAGE)
    }
    run(
      ServicePorts(
        configuratorPort = configuratorPort,
        zkPort = zkPort,
        ftpDataPort = ftpDataPort,
        brokersPort = brokersPort,
        workersPort = workersPort,
        dbPort = dbPort,
        ftpPort = ftpPort
      ),
      (configurator, _, _, _, _, _) => {
        println(s"run a configurator at ${configurator.hostname}:${configurator.port}")
        println(
          s"enter ctrl+c to terminate all processes (or all processes will be terminated after ${ttl.toSeconds} seconds")
        TimeUnit.SECONDS.sleep(ttl.toSeconds)
      }
    )
  }

  def run(ports: ServicePorts,
          stopped: (Configurator, Zookeepers, Brokers, Workers, Database, FtpServer) => Unit): Unit = {
    doClose5(Zookeepers.local(ports.zkPort))(Brokers.local(_, ports.brokersPort))(Workers.local(_, ports.workersPort))(
      _ => Database.local(ports.dbPort))(_ => FtpServer.local(ports.ftpPort, ports.ftpDataPort)) {
      case (zk, brokers, workers, dataBase, ftpServer) =>
        println("wait for the mini kafka cluster")
        TimeUnit.SECONDS.sleep(5)
        println(s"Succeed to run the mini brokers: ${brokers.connectionProps} and workers:${workers.connectionProps}")
        println(s"Succeed to run a database url:${dataBase.url} user:${dataBase.user} password:${dataBase.password}")
        println(
          s"Succeed to run a ftp server host:${ftpServer.host} port:${ftpServer.port} user:${ftpServer.user} password:${ftpServer.password}")
        val dbRoute: server.Route = path("creation" / "rdb") {
          pathEnd {
            post {
              entity(as[Creation]) { creation =>
                complete {
                  val client = DatabaseClient(dataBase.url, dataBase.user, dataBase.password)
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
                  zookeeper = zk.connectionProps,
                  brokers = brokers.connectionProps,
                  workers = workers.connectionProps,
                  ftpServer = FtpServerInformation(hostname = ftpServer.host,
                                                   port = ftpServer.port,
                                                   dataPort = ftpServer.dataPort,
                                                   user = ftpServer.user,
                                                   password = ftpServer.password),
                  database = DbInformation(
                    url = dataBase.url,
                    user = dataBase.user,
                    password = dataBase.password
                  )
                )
              }
            }
          }
        }
        val topicName = s"demo-${SystemUtil.current()}"
        doClose(KafkaClient(brokers.connectionProps))(
          _.topicCreator().numberOfPartitions(3).numberOfReplications(3).compacted().create(topicName)
        )
        val configurator = Configurator
          .builder()
          .store(Store.builder().brokers(brokers.connectionProps).topicName(topicName).buildBlocking[String, Any])
          .kafkaClient(KafkaClient(brokers.connectionProps))
          .connectClient(ConnectorClient(workers.connectionProps))
          .hostname(IoUtil.anyLocalAddress)
          .port(ports.configuratorPort)
          .extraRoute(dbRoute ~ servicesRoute)
          .build()
        try stopped(configurator, zk, brokers, workers, dataBase, ftpServer)
        finally configurator.close()
    }
  }
}
