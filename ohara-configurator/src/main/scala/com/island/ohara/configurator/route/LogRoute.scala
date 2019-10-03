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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.{NoSuchClusterException, ServiceCollie}
import com.island.ohara.client.configurator.v0.BrokerApi._
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.LogApi._
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.client.configurator.v0.ZookeeperApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

/**
  * Used to take log from specified cluster. We haven't log infra to provide UI to get log from specified "connector".
  * However, users need to "see" what happen on failed connectors. We don't implement the LogApi (client library) since
  * this is just a workaround.
  */
object LogRoute {

  private[this] def route(clusterKey: ObjectKey, data: Future[Map[ContainerInfo, String]])(
    implicit executionContext: ExecutionContext): server.Route =
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

  def apply(implicit collie: ServiceCollie, executionContext: ExecutionContext): server.Route =
    pathPrefix(LOG_PREFIX_PATH) {
      path(CONFIGURATOR_PREFIX_PATH) {
        // docker id appear in following files
        // 1) /proc/1/cpuset
        // 2) hostname
        // however, the hostname of container is overridable so we pick up first file.
        val containerId: String = try {
          val output = "cat /proc/1/cpuset".!!
          val index = output.lastIndexOf("/")
          if (index >= 0) output.substring(index + 1) else output
        } catch {
          case _: Throwable => CommonUtils.hostname()
        }
        complete(
          collie
            .containerNames()
            .map(names =>
              names
              // docker accept a part of id in querying so we "may" get a part of id
              // Either way, we don't want to miss the container so the "startWith" is our solution to compare the "sub" id
                .find(cn => cn.id.startsWith(containerId) || containerId.startsWith(cn.id))
                .getOrElse(throw new NoSuchElementException(
                  s"failed to find out the Configurator:$containerId from hosted nodes:${names.map(_.nodeName).mkString(".")}." +
                    s" Noted: Your Configurator MUST run on docker container and the host node must be added." +
                    s" existent containers:${names.map(n => s"${n.id}/${n.imageName}}").mkString(",")}")))
            .map(_.name)
            .flatMap(collie.log)
            .map {
              case (containerName, log) =>
                ClusterLog(
                  clusterKey = ObjectKey.of("N/A", containerName.name),
                  logs = Seq(NodeLog(hostname = containerName.nodeName, value = log))
                )
            })
      } ~ path(ZOOKEEPER_PREFIX_PATH / Segment) { clusterName =>
        parameter(GROUP_KEY ?) { groupOption =>
          val clusterKey =
            ObjectKey.of(groupOption.getOrElse(com.island.ohara.client.configurator.v0.GROUP_DEFAULT), clusterName)
          route(clusterKey, collie.zookeeperCollie.logs(clusterKey))
        }
      } ~ path(BROKER_PREFIX_PATH / Segment) { clusterName =>
        parameter(GROUP_KEY ?) { groupOption =>
          val clusterKey =
            ObjectKey.of(groupOption.getOrElse(com.island.ohara.client.configurator.v0.GROUP_DEFAULT), clusterName)
          route(clusterKey, collie.brokerCollie.logs(clusterKey))
        }
      } ~ path(WORKER_PREFIX_PATH / Segment) { clusterName =>
        parameter(GROUP_KEY ?) { groupOption =>
          val clusterKey =
            ObjectKey.of(groupOption.getOrElse(com.island.ohara.client.configurator.v0.GROUP_DEFAULT), clusterName)
          route(clusterKey, collie.workerCollie.logs(clusterKey))
        }
      } ~ path(STREAMS_PREFIX_PATH / Segment) { clusterName =>
        parameter(GROUP_KEY ?) { groupOption =>
          val clusterKey =
            ObjectKey.of(groupOption.getOrElse(com.island.ohara.client.configurator.v0.GROUP_DEFAULT), clusterName)
          route(clusterKey, collie.streamCollie.logs(clusterKey))
        }
      }
    }
}
