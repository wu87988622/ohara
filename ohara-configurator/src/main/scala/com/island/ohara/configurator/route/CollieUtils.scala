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
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.agent.{BrokerCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.util.{CommonUtil, Releasable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * TODO: this is just a workaround in ohara 0.2. It handles the following trouble:
  * 1) UI doesn't support user to host zk and bk cluster so most request won't carry the information of broker. Hence
  *    we need to handle the request with "default" broker cluster
  * 2) make ops to cluster be "blocking"
  */
object CollieUtils {
  private[route] class AdminCleaner(timeout: Long) extends Releasable {
    private[this] val closed = new AtomicBoolean(false)
    private[this] val queue = new LinkedBlockingQueue[(Long, TopicAdmin)]
    private[route] val executor = {
      val exec = Executors.newSingleThreadExecutor()
      // close and remove timeout admin objects
      def cleanup(timeout: Long): Unit = {
        val buf = new util.LinkedList[(Long, TopicAdmin)]()
        try while (!queue.isEmpty) {
          val obj = queue.take()
          if (timeout > 0 && CommonUtil.current() - obj._1 <= timeout) buf.add(obj)
          else if (!obj._2.closed()) Releasable.close(obj._2)
        } finally queue.addAll(buf)
      }
      Future {
        try while (!closed.get()) {
          cleanup(timeout)
          TimeUnit.SECONDS.sleep(5)
        } finally cleanup(-1)
      }(ExecutionContext.fromExecutor(exec))
      exec
    }

    def add(topicAdmin: TopicAdmin): TopicAdmin = if (closed.get())
      throw new IllegalArgumentException("cleaner is closed")
    else {
      queue.put((CommonUtil.current(), topicAdmin))
      topicAdmin
    }
    override def close(): Unit = {
      closed.set(true)
      executor.shutdown()
      executor.awaitTermination(30, TimeUnit.SECONDS)
    }
  }

  /**
    * close the internal cleaner. This is used by configurator only.
    */
  def close(): Unit = Releasable.close(cleaner)

  private[this] lazy val cleaner: AdminCleaner = new AdminCleaner(30 * 1000)

  private[route] def topicAdmin[T](clusterName: Option[String])(
    implicit brokerCollie: BrokerCollie): Future[(BrokerClusterInfo, TopicAdmin)] = clusterName
    .map(brokerCollie.topicAdmin)
    .getOrElse(brokerCollie
      .clusters()
      .map { clusters =>
        clusters.size match {
          case 0 =>
            throw new IllegalArgumentException(
              s"we can't choose default broker cluster since there is no broker cluster")
          case 1 => clusters.keys.head
          case _ =>
            throw new IllegalArgumentException(
              s"we can't choose default broker cluster since there are too many broker cluster:${clusters.keys.map(_.name).mkString(",")}")
        }
      }
      .map(c => (c, cleaner.add(brokerCollie.topicAdmin(c)))))

  private[route] def workerClient[T](clusterName: Option[String])(
    implicit workerCollie: WorkerCollie): Future[(WorkerClusterInfo, WorkerClient)] = clusterName
    .map(workerCollie.workerClient)
    .getOrElse(workerCollie
      .clusters()
      .map { clusters =>
        clusters.size match {
          case 0 =>
            throw new IllegalArgumentException(
              s"we can't choose default worker cluster since there is no worker cluster")
          case 1 => clusters.keys.head
          case _ =>
            throw new IllegalArgumentException(
              s"we can't choose default worker cluster since there are too many worker cluster:${clusters.keys.map(_.name).mkString(",")}")
        }
      }
      .map(c => (c, workerCollie.workerClient(c))))

  private[route] def both[T](wkClusterName: Option[String])(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie): Future[(BrokerClusterInfo, TopicAdmin, WorkerClusterInfo, WorkerClient)] =
    workerClient(wkClusterName).flatMap {
      case (wkInfo, wkClient) =>
        brokerCollie.topicAdmin(wkInfo.brokerClusterName).map {
          case (bkInfo, topicAdmin) => (bkInfo, cleaner.add(topicAdmin), wkInfo, wkClient)
        }
    }
}
