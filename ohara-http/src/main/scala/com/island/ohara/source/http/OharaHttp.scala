package com.island.ohara.source.http

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * The entry point of Ohara-HTTP
  */
object OharaHttp {

  val logger: Logger = Logger[this.type]

  implicit val system: ActorSystem = ActorSystem("OharaHttpDaemon")

  def main(args: Array[String]): Unit = {

    val httpActor = system.actorOf(Props[HttpConnectorActor], "HttpConnectorActor")
    system.scheduler.schedule(5 seconds, 5 seconds) {
      // TODO: pull csv schema information to local
      // TODO: pull command to start or stop HTTP server

      // TODO: implement receiving real Ohara HTTP Command

      // This is just a example.
      // After knowing how to pull the command of start and stop, it can implement the rest.
      val httpCommand = "start"
      httpCommand match {
        case "start" =>
          httpActor ! HttpCommand.START(ConfigFactory.load())
        case "stop" =>
          httpActor ! HttpCommand.STOP
      }
    }
  }
}
