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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.island.ohara.common.util.CommonUtils
import scala.collection.JavaConverters._

object Boot {
  implicit val actorSystem: ActorSystem        = ActorSystem("shabondi")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    try {
      val rawConfig = CommonUtils.parse(args.toSeq.asJava).asScala.toMap
      val config    = Config(rawConfig)
      val webServer = new WebServer(config)
      webServer.start()
    } catch {
      case ex: Throwable =>
        actorSystem.terminate()
        throw ex
    }
  }
}
