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

package oharastream.ohara.shabondi

import java.time.{Duration => JDuration}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.shabondi.DefaultDefinitions.{SERVER_TYPE_SINK, SERVER_TYPE_SOURCE}
import oharastream.ohara.shabondi.sink.SinkRouteHandler
import oharastream.ohara.shabondi.source.SourceRouteHandler
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

object Boot {
  private val log                              = Logger(Boot.getClass)
  implicit val actorSystem: ActorSystem        = ActorSystem("shabondi")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private def parseArgs(args: Array[String]): Map[String, String] =
    CommonUtils
      .parse(args.toSeq.asJava)
      .asScala
      .toMap
      .map {
        case (k, v) =>
          (k, Config.unescape(v))
      }

  private def dumpArgs(args: Map[String, String]): Unit = {
    log.info("Arguments:")
    args.foreach { case (k, v) => log.info(s"    $k=$v") }
  }

  def main(args: Array[String]): Unit = {
    try {
      val newArgs = parseArgs(args)
      dumpArgs(newArgs)

      val config    = Config(newArgs)
      val webServer = new WebServer(config, newRouteHandler(config))
      webServer.start()
    } catch {
      case ex: Throwable =>
        actorSystem.terminate()
        throw ex
    }
  }

  private def newRouteHandler(config: Config): RouteHandler =
    config.serverType match {
      case SERVER_TYPE_SOURCE => SourceRouteHandler(config)
      case SERVER_TYPE_SINK =>
        val handler = SinkRouteHandler(config)
        handler.scheduleFreeIdleGroups(JDuration.ofSeconds(10), config.sinkGroupIdleTime)
        handler
      case t => throw new RuntimeException(s"Invalid server type: $t")
    }
}
