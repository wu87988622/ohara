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

import com.island.ohara.agent.{Collie, ServiceCollie}
import com.island.ohara.client.Enum
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterInfo, BrokerClusterStatus}
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.StreamApi.{StreamClusterInfo, StreamClusterStatus}
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.{WorkerClusterInfo, WorkerClusterStatus}
import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterInfo, ZookeeperClusterStatus}
import com.island.ohara.client.configurator.v0.{ClusterInfo, ClusterStatus}
import com.island.ohara.common.setting.{ConnectorKey, ObjectKey, TopicKey}
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.route.ObjectChecker.ChickList
import com.island.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import com.island.ohara.configurator.store.DataStore

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Most routes do a great job - check the resource availability before starting it.
  * It means there are a lot of duplicate code used to check same resources in our routes. So this class is used to
  * unify all resource checks and produces unified error message.
  */
trait ObjectChecker {
  def checkList: ChickList
}

object ObjectChecker {

  case class ObjectInfos(topicInfos: Map[TopicInfo, Condition],
                         connectorInfos: Map[ConnectorInfo, Condition],
                         fileInfos: Seq[FileInfo],
                         zookeeperClusterInfos: Map[ZookeeperClusterInfo, Condition],
                         brokerClusterInfos: Map[BrokerClusterInfo, Condition],
                         workerClusterInfos: Map[WorkerClusterInfo, Condition],
                         streamClusterInfos: Map[StreamClusterInfo, Condition])

  final class ObjectCheckException(val objectType: String,
                                   val nonexistent: Set[ObjectKey],
                                   val illegalObjs: Map[ObjectKey, Condition])
      extends RuntimeException(
        s"type:$objectType ${nonexistent.map(k => s"$k does not exist").mkString(",")} ${illegalObjs
          .map {
            case (key, condition) =>
              condition match {
                case STOPPED => s"$key MUST be stopped"
                case RUNNING => s"$key MUST be running"
              }
          }
          .mkString(",")}")

  trait ChickList {

    //---------------[topic]---------------//

    /**
      * check the properties of topic.
      * @param key topic key
      * @return this check list
      */
    def topic(key: TopicKey): ChickList = topics(Set(key), None)

    /**
      * check both properties and status of topic.
      * @param key topic key
      * @return this check list
      */
    def topic(key: TopicKey, condition: Condition): ChickList = topics(Set(key), Some(condition))
    def topics(keys: Set[TopicKey], condition: Condition): ChickList = topics(keys, Some(condition))
    protected def topics(keys: Set[TopicKey], condition: Option[Condition]): ChickList

    //---------------[connector]---------------//

    /**
      * check the properties of connector.
      * @param key connector key
      * @return this check list
      */
    def connector(key: ConnectorKey): ChickList = connectors(Set(key), None)

    /**
      * check both properties and status of connector.
      * @param key connector key
      * @return this check list
      */
    def connector(key: ConnectorKey, condition: Condition): ChickList = connectors(Set(key), Some(condition))
    protected def connectors(keys: Set[ConnectorKey], condition: Option[Condition]): ChickList

    //---------------[file]---------------//

    /**
      * check the properties of file.
      * @param key file key
      * @return this check list
      */
    def file(key: ObjectKey): ChickList = files(Set(key))

    /**
      * check the properties of files.
      * @param keys files key
      * @return this check list
      */
    def files(keys: Set[ObjectKey]): ChickList

    //---------------[zookeeper]---------------//

    /**
      * check the properties of zookeeper cluster.
      * @param key zookeeper cluster key
      * @return this check list
      */
    def zookeeperCluster(key: ObjectKey): ChickList = zookeeperClusters(Set(key), None)

    /**
      * check both properties and status of zookeeper cluster.
      * @param key zookeeper cluster key
      * @return this check list
      */
    def zookeeperCluster(key: ObjectKey, condition: Condition): ChickList = zookeeperClusters(Set(key), Some(condition))

    protected def zookeeperClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList

    //---------------[broker]---------------//

    /**
      * check the properties of broker cluster.
      * @param key broker cluster key
      * @return this check list
      */
    def brokerCluster(key: ObjectKey): ChickList = brokerClusters(Set(key), None)

    /**
      * check both properties and status of broker cluster.
      * @param key broker cluster key
      * @return this check list
      */
    def brokerCluster(key: ObjectKey, condition: Condition): ChickList = brokerClusters(Set(key), Some(condition))

    protected def brokerClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList

    //---------------[worker]---------------//

    /**
      * check the properties of worker cluster.
      * @param key worker cluster key
      * @return this check list
      */
    def workerCluster(key: ObjectKey): ChickList = workerClusters(Set(key), None)

    /**
      * check both properties and status of worker cluster.
      * @param key worker cluster key
      * @return this check list
      */
    def workerCluster(key: ObjectKey, condition: Condition): ChickList = workerClusters(Set(key), Some(condition))

    protected def workerClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList

    //---------------[stream app]---------------//

    /**
      * check the properties of streamApp cluster.
      * @param key streamApp cluster key
      * @return this check list
      */
    def streamApp(key: ObjectKey): ChickList = streamApps(Set(key), None)

    /**
      * check both properties and status of streamApp cluster.
      * @param key streamApp cluster key
      * @return this check list
      */
    def streamApp(key: ObjectKey, condition: Condition): ChickList = streamApps(Set(key), Some(condition))

    protected def streamApps(keys: Set[ObjectKey], condition: Option[Condition]): ChickList
    //---------------[final check]---------------//
    /**
      * throw exception if the input assurances don't pass. Otherwise, return the resources.
      * @param executionContext thread pool
      * @return resource
      * @throws ObjectCheckException: it contains the first unmatched objects. You can seek the related information
      *                             to address more follow-up actions.
      */
    def check()(implicit executionContext: ExecutionContext): Future[ObjectInfos]
  }

  sealed abstract class Condition
  object Condition extends Enum[Condition] {
    case object RUNNING extends Condition
    case object STOPPED extends Condition
  }

  def apply()(implicit store: DataStore,
              serviceCollie: ServiceCollie,
              fileStore: FileStore,
              adminCleaner: AdminCleaner): ObjectChecker =
    new ObjectChecker {

      override def checkList: ChickList = new ChickList {
        private[this] val files: mutable.Set[ObjectKey] = mutable.Set[ObjectKey]()
        private[this] val topics: mutable.Map[TopicKey, Option[Condition]] = mutable.Map[TopicKey, Option[Condition]]()
        private[this] val connectors: mutable.Map[ConnectorKey, Option[Condition]] =
          mutable.Map[ConnectorKey, Option[Condition]]()
        private[this] val zookeepers: mutable.Map[ObjectKey, Option[Condition]] =
          mutable.Map[ObjectKey, Option[Condition]]()
        private[this] val brokers: mutable.Map[ObjectKey, Option[Condition]] =
          mutable.Map[ObjectKey, Option[Condition]]()
        private[this] val workers: mutable.Map[ObjectKey, Option[Condition]] =
          mutable.Map[ObjectKey, Option[Condition]]()
        private[this] val streamApps: mutable.Map[ObjectKey, Option[Condition]] =
          mutable.Map[ObjectKey, Option[Condition]]()

        private[this] def checkCluster[S <: ClusterStatus, C <: ClusterInfo: ClassTag](
          collie: Collie[S],
          key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Option[(C, Condition)]] =
          store.get[C](key).flatMap {
            case None => Future.successful(None)
            case Some(cluster) =>
              collie.exist(key).map(if (_) RUNNING else STOPPED).map(condition => Some(cluster -> condition))
          }

        private[this] def checkClusters[S <: ClusterStatus, C <: ClusterInfo: ClassTag](
          collie: Collie[S],
          keys: Set[ObjectKey])(implicit executionContext: ExecutionContext): Future[Map[C, Condition]] =
          Future
            .traverse(keys) { key =>
              checkCluster[S, C](collie, key)
            }
            .map(_.flatten.toMap)

        private[this] def checkTopic(key: TopicKey)(
          implicit executionContext: ExecutionContext): Future[Option[(TopicInfo, Condition)]] =
          store.get[TopicInfo](key).flatMap {
            case None => Future.successful(None)
            case Some(topicInfo) =>
              checkCluster[BrokerClusterStatus, BrokerClusterInfo](serviceCollie.brokerCollie,
                                                                   topicInfo.brokerClusterKey).flatMap {
                case None => Future.successful(Some(topicInfo -> STOPPED))
                case Some((brokerClusterInfo, condition)) =>
                  condition match {
                    case STOPPED => Future.successful(Some(topicInfo -> STOPPED))
                    case RUNNING =>
                      serviceCollie.brokerCollie
                        .topicAdmin(brokerClusterInfo)
                        // make sure the topic admin is closed!!!
                        .flatMap(
                          admin =>
                            adminCleaner
                              .add(admin)
                              .topics()
                              .map(try _
                              finally Releasable.close(admin)))
                        .map(_.exists(_.name == key.topicNameOnKafka()))
                        .map(if (_) RUNNING else STOPPED)
                        .map(condition => Some(topicInfo -> condition))
                  }
              }
          }

        private[this] def checkConnector(key: ConnectorKey)(
          implicit executionContext: ExecutionContext): Future[Option[(ConnectorInfo, Condition)]] =
          store.get[ConnectorInfo](key).flatMap {
            case None => Future.successful(None)
            case Some(connectorInfo) =>
              checkCluster[WorkerClusterStatus, WorkerClusterInfo](serviceCollie.workerCollie,
                                                                   connectorInfo.workerClusterKey).flatMap {
                case None => Future.successful(Some(connectorInfo -> STOPPED))
                case Some((workerClusterInfo, condition)) =>
                  condition match {
                    case STOPPED => Future.successful(Some(connectorInfo -> STOPPED))
                    case RUNNING =>
                      serviceCollie.workerCollie
                        .workerClient(workerClusterInfo)
                        .flatMap(_.activeConnectors())
                        .map(_.contains(key.connectorNameOnKafka()))
                        .map(if (_) RUNNING else STOPPED)
                        .map(condition => Some(connectorInfo -> condition))
                  }
              }
          }

        private[this] def checkFile(key: ObjectKey)(
          implicit executionContext: ExecutionContext): Future[Option[FileInfo]] =
          fileStore.fileInfo(key).map(Some(_)).recover {
            case _: NoSuchElementException => None
          }

        private[this] def compare(name: String,
                                  result: Map[ObjectKey, Condition],
                                  required: Map[ObjectKey, Option[Condition]]): Unit = {
          val nonexistent = required.keys.filterNot(key => result.exists(_._1 == key)).toSet
          val illegal = required
          // this key exists and it does not care for condition.
            .filter(_._2.isDefined)
            // the nonexistent keys is handled already (see nonexistent)
            .filter(e => result.exists(_._1 == e._1))
            .map(e => e._1 -> e._2.get)
            .filter {
              case (key, requiredCondition) => result(key) != requiredCondition
            }
          if (nonexistent.nonEmpty || illegal.nonEmpty) throw new ObjectCheckException(name, nonexistent, illegal)
        }

        override def check()(implicit executionContext: ExecutionContext): Future[ObjectInfos] =
          // check files
          Future
            .traverse(files)(checkFile)
            .map(_.flatten)
            .map { passed =>
              compare("file", passed.map(_.key -> RUNNING).toMap, files.map(_ -> Some(RUNNING)).toMap)
              ObjectInfos(
                topicInfos = Map.empty,
                connectorInfos = Map.empty,
                fileInfos = passed.toSeq,
                zookeeperClusterInfos = Map.empty,
                brokerClusterInfos = Map.empty,
                workerClusterInfos = Map.empty,
                streamClusterInfos = Map.empty
              )
            }
            // check zookeepers
            .flatMap { report =>
              checkClusters[ZookeeperClusterStatus, ZookeeperClusterInfo](serviceCollie.zookeeperCollie,
                                                                          zookeepers.keys.toSet).map { passed =>
                compare("zookeeper", passed.map(e => e._1.key -> e._2), zookeepers.toMap)
                report.copy(zookeeperClusterInfos = passed)
              }
            }
            // check brokers
            .flatMap { report =>
              checkClusters[BrokerClusterStatus, BrokerClusterInfo](serviceCollie.brokerCollie, brokers.keys.toSet)
                .map { passed =>
                  compare("broker", passed.map(e => e._1.key -> e._2), brokers.toMap)
                  report.copy(brokerClusterInfos = passed)
                }
            }
            // check streamApps
            .flatMap { report =>
              checkClusters[StreamClusterStatus, StreamClusterInfo](serviceCollie.streamCollie, streamApps.keys.toSet)
                .map { passed =>
                  compare("streamApp", passed.map(e => e._1.key -> e._2), streamApps.toMap)
                  report.copy(streamClusterInfos = passed)
                }
            }
            // check workers
            .flatMap { report =>
              checkClusters[WorkerClusterStatus, WorkerClusterInfo](serviceCollie.workerCollie, workers.keys.toSet)
                .map { passed =>
                  compare("worker", passed.map(e => e._1.key -> e._2), workers.toMap)
                  report.copy(workerClusterInfos = passed)
                }
            }
            // check topics
            .flatMap { report =>
              Future.traverse(topics.keySet)(checkTopic).map(_.flatten.toMap).map { passed =>
                compare("topic", passed.map(e => e._1.key -> e._2).toMap, topics.toMap)
                report.copy(topicInfos = passed)
              }
            }
            // check connectors
            .flatMap { report =>
              Future.traverse(connectors.keys)(checkConnector).map(_.flatten.toMap).map { passed =>
                compare("connector", passed.map(e => e._1.key -> e._2).toMap, connectors.toMap)
                report.copy(connectorInfos = passed)
              }
            }

        override protected def topics(keys: Set[TopicKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => topics += (key -> condition))
          this
        }

        override protected def connectors(keys: Set[ConnectorKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => connectors += (key -> condition))
          this
        }

        override def files(keys: Set[ObjectKey]): ChickList = {
          files ++= keys
          this
        }

        override protected def zookeeperClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => zookeepers += (key -> condition))
          this
        }

        override protected def brokerClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => brokers += (key -> condition))
          this
        }

        override protected def workerClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => workers += (key -> condition))
          this
        }

        override protected def streamApps(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => streamApps += (key -> condition))
          this
        }
      }
    }
}
