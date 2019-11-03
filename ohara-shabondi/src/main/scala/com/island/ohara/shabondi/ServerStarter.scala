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

package com.island.ohara.shabondi

import com.island.ohara.shabondi.Model.{CmdArgs, HttpSink, HttpSource}

import scala.util.{Failure, Success, Try}

object ServerStarter {
  private val usageMessage = "Usage: [--source | --sink] [$$port]"

  def main(args: Array[String]): Unit = {

    parseArgs(args) match {
      case Success(cmdArgs) =>
        val webServer = new WebServer(cmdArgs.serverType)
        webServer.start(cmdArgs.interface, cmdArgs.port)
      case Failure(ex) =>
        println(ex.getMessage)
        sys.exit(1)
    }
  }

  private[shabondi] def parseArgs(args: Array[String]): Try[CmdArgs] = Try {
    if (args.size == 2) {
      try {
        val serverType = args(0) match {
          case "--source" => HttpSource
          case "--sink"   => HttpSink
          case _          => throw new IllegalArgumentException(usageMessage)
        }
        val port = args(1).toInt
        CmdArgs(serverType = serverType, port = port)
      } catch {
        case _: Exception => throw new IllegalArgumentException(usageMessage)
      }
    } else {
      throw new IllegalArgumentException(usageMessage)
    }
  }

}
