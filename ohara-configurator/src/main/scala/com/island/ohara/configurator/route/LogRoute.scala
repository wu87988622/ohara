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

package com.island.ohara.configurator.route

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.nio.charset.Charset
import java.nio.file.Files
import java.text.SimpleDateFormat

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.container.ContainerName
import com.island.ohara.agent.{NoSuchClusterException, ServiceCollie}
import com.island.ohara.client.configurator.v0.BrokerApi._
import com.island.ohara.client.configurator.v0.LogApi._
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.client.configurator.v0.ZookeeperApi._
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Used to take log from specified cluster. We haven't log infra to provide UI to get log from specified "connector".
  * However, users need to "see" what happen on failed connectors. We don't implement the LogApi (client library) since
  * this is just a workaround.
  */
object LogRoute {
  private[this] def route(clusterKey: ObjectKey, data: Future[Map[ContainerName, String]])(
    implicit executionContext: ExecutionContext
  ): server.Route =
    complete(data.map { d =>
      if (d.isEmpty) throw new NoSuchClusterException(s"cluster:$clusterKey does not exist")
      else
        ClusterLog(
          clusterKey = clusterKey,
          logs = d.map {
            case (container, log) => NodeLog(container.nodeName, log)
          }.toSeq
        )
    })

  private[this] def seekLogByTimestamp(file: File, sinceSecondsOption: Option[Long]): String =
    sinceSecondsOption match {
      case None => new String(Files.readAllBytes(file.toPath), Charset.forName("UTF-8"))
      case Some(sinceSeconds) =>
        val fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")))
        try {
          seekLogByTimestamp(
            new Iterator[String] {
              private[this] var line: String = fileReader.readLine()
              override def hasNext: Boolean  = line != null
              override def next(): String =
                try line
                finally line = fileReader.readLine()
            },
            CommonUtils.current() - sinceSeconds
          )
        } finally Releasable.close(fileReader)
    }

  @VisibleForTesting
  private[route] def seekLogByTimestamp(strings: Iterator[String], minTime: Long): String = {
    val df   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
    val buf  = mutable.ArrayBuffer[String]()
    var pass = false
    // 2019-12-17 03:20:30,350
    while (strings.hasNext) {
      val line = strings.next()
      if (pass) buf += line
      else
        try {
          val items = line.split(" ")
          if (items.size >= 2 && df.parse(s"${items(0)} ${items(1)}").getTime >= minTime) pass = true
        } catch {
          case _: Throwable =>
          // just skip this incorrect log message
        }
    }
    buf.mkString("\n")
  }

  def apply(implicit collie: ServiceCollie, executionContext: ExecutionContext): server.Route =
    pathPrefix(LOG_PREFIX_PATH) {
      path(CONFIGURATOR_PREFIX_PATH) {
        parameters(SINCE_SECONDS_KEY.as[Long] ?) { sinceSeconds =>
          // the log folder is kept by ../conf/log4j.properties
          val folder   = new File("../logs")
          val logFiles = folder.listFiles()
          complete(
            ClusterLog(
              clusterKey = ObjectKey.of("N/A", CommonUtils.hostname()),
              logs =
                if (logFiles == null || logFiles.isEmpty) Seq.empty
                else
                  logFiles.filter(_.getName.endsWith(".log")).map { file =>
                    NodeLog(
                      CommonUtils.hostname(),
                      seekLogByTimestamp(file, sinceSeconds)
                    )
                  }
            )
          )
        }
      } ~ path(ZOOKEEPER_PREFIX_PATH / Segment) { clusterName =>
        parameter((GROUP_KEY ? GROUP_DEFAULT, SINCE_SECONDS_KEY.as[Long] ?)) {
          case (group, sinceSeconds) =>
            val clusterKey =
              ObjectKey.of(group, clusterName)
            route(clusterKey, collie.zookeeperCollie.logs(clusterKey, sinceSeconds))
        }
      } ~ path(BROKER_PREFIX_PATH / Segment) { clusterName =>
        parameter((GROUP_KEY ? GROUP_DEFAULT, SINCE_SECONDS_KEY.as[Long] ?)) {
          case (group, sinceSeconds) =>
            val clusterKey = ObjectKey.of(group, clusterName)
            route(clusterKey, collie.brokerCollie.logs(clusterKey, sinceSeconds))
        }
      } ~ path(WORKER_PREFIX_PATH / Segment) { clusterName =>
        parameter((GROUP_KEY ? GROUP_DEFAULT, SINCE_SECONDS_KEY.as[Long] ?)) {
          case (group, sinceSeconds) =>
            val clusterKey = ObjectKey.of(group, clusterName)
            route(clusterKey, collie.workerCollie.logs(clusterKey, sinceSeconds))
        }
      } ~ path(STREAM_PREFIX_PATH / Segment) { clusterName =>
        parameter((GROUP_KEY ? GROUP_DEFAULT, SINCE_SECONDS_KEY.as[Long] ?)) {
          case (group, sinceSeconds) =>
            val clusterKey = ObjectKey.of(group, clusterName)
            route(clusterKey, collie.streamCollie.logs(clusterKey, sinceSeconds))
        }
      }
    }
}
