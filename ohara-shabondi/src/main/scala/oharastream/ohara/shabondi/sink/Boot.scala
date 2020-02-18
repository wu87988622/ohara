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

package oharastream.ohara.shabondi.sink

import com.typesafe.scalalogging.Logger
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.shabondi.common.ShabondiUtils

object Boot {
  private val log = Logger(Boot.getClass)

  def main(args: Array[String]): Unit = {
    val newArgs = ShabondiUtils.parseArgs(args)
    log.info("Arguments:")
    newArgs.foreach { case (k, v) => log.info(s"    $k=$v") }

    val config    = new SinkConfig(newArgs)
    val webServer = new WebServer(config)
    try {
      webServer.start(CommonUtils.anyLocalAddress(), config.port)
    } finally {
      webServer.close()
    }
  }
}
