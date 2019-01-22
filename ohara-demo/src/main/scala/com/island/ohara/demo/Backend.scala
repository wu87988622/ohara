/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.demo
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.DatabaseClient
import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.integration._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
  * run a configurator based on 3 brokers and 3 workers.
  */
object Backend {
  final case class Creation(name: String, schema: Seq[RdbColumn])
  implicit val CREATION_JSON_FORMAT: RootJsonFormat[Creation] = jsonFormat2(Creation)

  final case class DbInformation(url: String, user: String, password: String)
  implicit val DB_INFO_JSON_FORMAT: RootJsonFormat[DbInformation] = jsonFormat3(DbInformation)

  final case class FtpServerInformation(hostname: String, port: Int, dataPort: Seq[Int], user: String, password: String)
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
                                ftpDataPorts: Seq[Int],
                                workersPort: Seq[Int],
                                brokersPort: Seq[Int],
                                zkPort: Int)

  object ServicePorts {
    val default = ServicePorts(
      configuratorPort = 0,
      dbPort = 0,
      ftpPort = 0,
      ftpDataPorts = Seq.fill(1)(0),
      workersPort = Seq.fill(1)(0),
      brokersPort = Seq.fill(1)(0),
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
    val (ttl, ports) = parse(args)
    run(
      ports,
      (configurator, _, _, _, _, _) => {
        println(s"run a configurator at ${configurator.hostname}:${configurator.port}")
        println(
          s"enter ctrl+c to terminate all processes (or all processes will be terminated after ${ttl.toSeconds} seconds")
        TimeUnit.SECONDS.sleep(ttl.toSeconds)
      }
    )
  }

  private[demo] def parse(args: Array[String]): (Duration, ServicePorts) = {
    // TODO: make the parse more friendly
    var ttl: Duration = 365 days
    var configuratorPort: Int = 0
    var zkPort: Int = 0
    var brokersPort: Seq[Int] = Seq.fill(3)(0)
    var workersPort: Seq[Int] = Seq.fill(3)(0)
    var dbPort: Int = 0
    var ftpPort: Int = 0
    var ftpDataPorts: Seq[Int] = Seq.fill(3)(0)

    def parsePortRange(s: String): Seq[Int] = if (s.contains("-")) {
      val min = s.split("-").head.toInt
      val max = s.split("-").last.toInt
      min to max
    } else s.split(",").map(_.toInt)

    args.sliding(2, 2).foreach {
      case Array(CONFIGURATOR_PORT_KEY, value) => configuratorPort = value.toInt
      case Array(ZOOKEEPER_PORT_KEY, value)    => zkPort = value.toInt
      case Array(BROKERS_PORT_KEY, value)      => brokersPort = parsePortRange(value)
      case Array(WORKERS_PORT_KEY, value)      => workersPort = parsePortRange(value)
      case Array(DB_PORT_KEY, value)           => dbPort = value.toInt
      case Array(FTP_PORT_KEY, value)          => ftpPort = value.toInt
      case Array(FTP_DATA_PORT_KEY, value)     => ftpDataPorts = parsePortRange(value)
      case Array(TTL_KEY, value)               => ttl = value.toInt seconds
      case _                                   => throw new IllegalArgumentException(USAGE)
    }
    (ttl,
     ServicePorts(
       configuratorPort = configuratorPort,
       zkPort = zkPort,
       ftpDataPorts = ftpDataPorts,
       brokersPort = brokersPort,
       workersPort = workersPort,
       dbPort = dbPort,
       ftpPort = ftpPort
     ))
  }
  private[this] def resources(ports: ServicePorts): (Zookeepers, Brokers, Workers, Database, FtpServer) = {
    val rs = new ArrayBuffer[Releasable]
    try {
      val zk = Zookeepers.local(ports.zkPort)
      rs += zk
      val brokers = Brokers.local(zk, ports.brokersPort.toArray)
      rs += brokers
      val workers = Workers.local(brokers, ports.workersPort.toArray)
      rs += workers
      val database = Database.local(ports.dbPort)
      rs += database
      val ftpServer = FtpServer.local(ports.ftpPort, ports.ftpDataPorts.toArray)
      rs += ftpServer
      (zk, brokers, workers, database, ftpServer)
    } catch {
      case e: Throwable =>
        rs.foreach(Releasable.close)
        throw e
    }
  }

  private[demo] def run(ports: ServicePorts,
                        stopped: (Configurator, Zookeepers, Brokers, Workers, Database, FtpServer) => Unit): Unit = {
    val (zk, brokers, workers, dataBase, ftpServer) = resources(ports)
    println("wait for the mini kafka cluster")
    TimeUnit.SECONDS.sleep(5)
    println(s"Succeed to run the mini brokers: ${brokers.connectionProps} and workers:${workers.connectionProps}")
    println(s"Succeed to run a database url:${dataBase.url} user:${dataBase.user} password:${dataBase.password}")
    println(
      s"Succeed to run a ftp server hostname:${ftpServer.hostname} port:${ftpServer.port} user:${ftpServer.user} password:${ftpServer.password}")
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
              ftpServer = FtpServerInformation(
                hostname = ftpServer.hostname,
                port = ftpServer.port,
                dataPort = ftpServer.dataPort().asScala.map(x => x.toInt),
                user = ftpServer.user,
                password = ftpServer.password
              ),
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
    val configurator = Configurator
      .builder()
      .fake(brokers.connectionProps, workers.connectionProps)
      .hostname(CommonUtil.anyLocalAddress)
      .port(ports.configuratorPort)
      .extraRoute(dbRoute ~ servicesRoute)
      .build()
    try stopped(configurator, zk, brokers, workers, dataBase, ftpServer)
    finally {
      Releasable.close(configurator)
      Releasable.close(ftpServer)
      Releasable.close(dataBase)
      Releasable.close(workers)
      Releasable.close(brokers)
      Releasable.close(zk)
    }
  }
}
