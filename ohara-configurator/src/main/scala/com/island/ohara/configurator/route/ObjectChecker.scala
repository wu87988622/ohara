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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.{StreamClusterInfo, StreamClusterStatus}
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.{WorkerClusterInfo, WorkerClusterStatus}
import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterInfo, ZookeeperClusterStatus}
import com.island.ohara.client.configurator.v0.{ClusterInfo, ClusterStatus}
import com.island.ohara.common.setting.{ConnectorKey, ObjectKey, TopicKey}
import com.island.ohara.common.util.Releasable
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
                         nodes: Seq[Node],
                         zookeeperClusterInfos: Map[ZookeeperClusterInfo, Condition],
                         brokerClusterInfos: Map[BrokerClusterInfo, Condition],
                         workerClusterInfos: Map[WorkerClusterInfo, Condition],
                         streamClusterInfos: Map[StreamClusterInfo, Condition]) {
    def runningTopics: Seq[TopicInfo] = topicInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningConnectors: Seq[ConnectorInfo] = connectorInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningZookeepers: Seq[ZookeeperClusterInfo] = zookeeperClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningBrokers: Seq[BrokerClusterInfo] = brokerClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningWorkers: Seq[WorkerClusterInfo] = workerClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningStreamApps: Seq[StreamClusterInfo] = streamClusterInfos.filter(_._2 == RUNNING).keys.toSeq
  }

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
      * check all topics. It invokes a loop to all topics and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allTopics(): ChickList

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

    /**
      * check whether input topics have been stored in Configurator
      * @param keys topic keys
      * @return this check list
      */
    def topics(keys: Set[TopicKey]): ChickList = topics(keys, None)

    /**
      * check whether input topics condition.
      * @param keys topic keys
      * @return this check list
      */
    def topics(keys: Set[TopicKey], condition: Condition): ChickList = topics(keys, Some(condition))
    protected def topics(keys: Set[TopicKey], condition: Option[Condition]): ChickList

    //---------------[connector]---------------//

    /**
      * check all connectors. It invokes a loop to all connectors and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allConnectors(): ChickList

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
      * check all files.
      * @return check list
      */
    def allFiles(): ChickList

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

    //---------------[node]---------------//

    /**
      * check all nodes.
      * @return check list
      */
    def allNodes(): ChickList

    /**
      * check the properties of node.
      * @param hostname hostname
      * @return this check list
      */
    def nodeName(hostname: String): ChickList = nodeNames(Set(hostname))

    /**
      * check the properties of nodes.
      * @param hostNames node names
      * @return this check list
      */
    def nodeNames(hostNames: Set[String]): ChickList = nodes(hostNames.map(n => ObjectKey.of(GROUP_DEFAULT, n)))

    /**
      * check the properties of nodes.
      * @param key node key
      * @return this check list
      */
    def node(key: ObjectKey): ChickList = nodes(Set(key))

    /**
      * check the properties of nodes.
      * @param keys nodes key
      * @return this check list
      */
    def nodes(keys: Set[ObjectKey]): ChickList

    //---------------[zookeeper]---------------//

    /**
      * check all zookeepers. It invokes a loop to all zookeepers and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allZookeepers(): ChickList

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
      * check all brokers. It invokes a loop to all brokers and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allBrokers(): ChickList

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
      * check all workers. It invokes a loop to all workers and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allWorkers(): ChickList

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
      * check all streamApps. It invokes a loop to all streamApps and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allStreamApps(): ChickList

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

  def apply()(implicit store: DataStore, serviceCollie: ServiceCollie, adminCleaner: AdminCleaner): ObjectChecker =
    new ObjectChecker {

      override def checkList: ChickList = new ChickList {
        private[this] var requireAllNodes = false
        private[this] val requiredNodes = mutable.Set[ObjectKey]()
        private[this] var requireAllFiles = false
        private[this] val requiredFiles = mutable.Set[ObjectKey]()
        private[this] var requireAllTopics = false
        private[this] val requiredTopics = mutable.Map[TopicKey, Option[Condition]]()
        private[this] var requireAllConnectors = false
        private[this] val requiredConnectors = mutable.Map[ConnectorKey, Option[Condition]]()
        private[this] var requireAllZookeepers = false
        private[this] val requiredZookeepers = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllBrokers = false
        private[this] val requiredBrokers = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllWorkers = false
        private[this] val requiredWorkers = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllStreamApps = false
        private[this] val requiredStreamApps = mutable.Map[ObjectKey, Option[Condition]]()

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

        private[this] def checkZookeepers()(
          implicit executionContext: ExecutionContext): Future[Map[ZookeeperClusterInfo, Condition]] =
          if (requireAllZookeepers)
            store
              .values[ZookeeperClusterInfo]()
              .map(_.map(_.key))
              .flatMap(keys =>
                checkClusters[ZookeeperClusterStatus, ZookeeperClusterInfo](serviceCollie.zookeeperCollie, keys.toSet))
          else
            checkClusters[ZookeeperClusterStatus, ZookeeperClusterInfo](serviceCollie.zookeeperCollie,
                                                                        requiredZookeepers.keys.toSet)

        private[this] def checkBrokers()(
          implicit executionContext: ExecutionContext): Future[Map[BrokerClusterInfo, Condition]] =
          if (requireAllBrokers)
            store
              .values[BrokerClusterInfo]()
              .map(_.map(_.key))
              .flatMap(keys =>
                checkClusters[BrokerClusterStatus, BrokerClusterInfo](serviceCollie.brokerCollie, keys.toSet))
          else
            checkClusters[BrokerClusterStatus, BrokerClusterInfo](serviceCollie.brokerCollie,
                                                                  requiredBrokers.keys.toSet)

        private[this] def checkWorkers()(
          implicit executionContext: ExecutionContext): Future[Map[WorkerClusterInfo, Condition]] =
          if (requireAllWorkers)
            store
              .values[WorkerClusterInfo]()
              .map(_.map(_.key))
              .flatMap(keys =>
                checkClusters[WorkerClusterStatus, WorkerClusterInfo](serviceCollie.workerCollie, keys.toSet))
          else
            checkClusters[WorkerClusterStatus, WorkerClusterInfo](serviceCollie.workerCollie,
                                                                  requiredWorkers.keys.toSet)

        private[this] def checkStreamApps()(
          implicit executionContext: ExecutionContext): Future[Map[StreamClusterInfo, Condition]] =
          if (requireAllStreamApps)
            store
              .values[StreamClusterInfo]()
              .map(_.map(_.key))
              .flatMap(keys =>
                checkClusters[StreamClusterStatus, StreamClusterInfo](serviceCollie.streamCollie, keys.toSet))
          else
            checkClusters[StreamClusterStatus, StreamClusterInfo](serviceCollie.streamCollie,
                                                                  requiredStreamApps.keys.toSet)

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
                      topicAdmin(brokerClusterInfo)(serviceCollie.brokerCollie, adminCleaner, executionContext)
                      // make sure the topic admin is closed!!!
                        .flatMap(
                          admin =>
                            admin
                              .topics()
                              .map(try _
                              finally Releasable.close(admin)))
                        .map(_.exists(_.name == key.topicNameOnKafka()))
                        .map(if (_) RUNNING else STOPPED)
                        .map(condition => Some(topicInfo -> condition))
                  }
              }
          }

        private[this] def checkTopics()(
          implicit executionContext: ExecutionContext): Future[Map[TopicInfo, Condition]] =
          if (requireAllTopics) store.values[TopicInfo]().map(_.map(_.key)).flatMap { keys =>
            Future.traverse(keys)(checkTopic).map(_.flatten.toMap)
          } else Future.traverse(requiredTopics.keySet)(checkTopic).map(_.flatten.toMap)

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

        private[this] def checkConnectors()(
          implicit executionContext: ExecutionContext): Future[Map[ConnectorInfo, Condition]] =
          if (requireAllConnectors) store.values[ConnectorInfo]().map(_.map(_.key)).flatMap { keys =>
            Future.traverse(keys)(checkConnector).map(_.flatten.toMap)
          } else Future.traverse(requiredConnectors.keySet)(checkConnector).map(_.flatten.toMap)

        private[this] def checkFiles()(implicit executionContext: ExecutionContext): Future[Seq[FileInfo]] =
          if (requireAllFiles) store.values[FileInfo]()
          else Future.traverse(requiredFiles)(store.value[FileInfo]).map(_.toSeq)

        private[this] def checkNodes()(implicit executionContext: ExecutionContext): Future[Seq[Node]] =
          if (requireAllNodes) store.values[Node]()
          else
            Future.traverse(requiredNodes)(store.get[Node]).map(_.flatten.toSeq)

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
          checkFiles()
            .map { passed =>
              compare("file", passed.map(_.key -> RUNNING).toMap, requiredFiles.map(_ -> Some(RUNNING)).toMap)
              ObjectInfos(
                topicInfos = Map.empty,
                connectorInfos = Map.empty,
                fileInfos = passed,
                nodes = Seq.empty,
                zookeeperClusterInfos = Map.empty,
                brokerClusterInfos = Map.empty,
                workerClusterInfos = Map.empty,
                streamClusterInfos = Map.empty
              )
            }
            .flatMap { report =>
              checkNodes.map { passed =>
                compare("node", passed.map(_.key -> RUNNING).toMap, requiredNodes.map(_ -> Some(RUNNING)).toMap)
                report.copy(nodes = passed)
              }
            }
            // check zookeepers
            .flatMap { report =>
              checkZookeepers().map { passed =>
                compare("zookeeper", passed.map(e => e._1.key -> e._2), requiredZookeepers.toMap)
                report.copy(zookeeperClusterInfos = passed)
              }
            }
            // check brokers
            .flatMap { report =>
              checkBrokers().map { passed =>
                compare("broker", passed.map(e => e._1.key -> e._2), requiredBrokers.toMap)
                report.copy(brokerClusterInfos = passed)
              }
            }
            // check streamApps
            .flatMap { report =>
              checkStreamApps().map { passed =>
                compare("streamApp", passed.map(e => e._1.key -> e._2), requiredStreamApps.toMap)
                report.copy(streamClusterInfos = passed)
              }
            }
            // check workers
            .flatMap { report =>
              checkWorkers().map { passed =>
                compare("worker", passed.map(e => e._1.key -> e._2), requiredWorkers.toMap)
                report.copy(workerClusterInfos = passed)
              }
            }
            // check topics
            .flatMap { report =>
              checkTopics().map { passed =>
                compare("topic", passed.map(e => e._1.key -> e._2).toMap, requiredTopics.toMap)
                report.copy(topicInfos = passed)
              }
            }
            // check connectors
            .flatMap { report =>
              checkConnectors().map { passed =>
                compare("connector", passed.map(e => e._1.key -> e._2).toMap, requiredConnectors.toMap)
                report.copy(connectorInfos = passed)
              }
            }

        override protected def topics(keys: Set[TopicKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => requiredTopics += (key -> condition))
          this
        }

        override protected def connectors(keys: Set[ConnectorKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => requiredConnectors += (key -> condition))
          this
        }

        override def nodes(keys: Set[ObjectKey]): ChickList = {
          requiredNodes ++= keys
          this
        }

        override def files(keys: Set[ObjectKey]): ChickList = {
          requiredFiles ++= keys
          this
        }

        override protected def zookeeperClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => requiredZookeepers += (key -> condition))
          this
        }

        override protected def brokerClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => requiredBrokers += (key -> condition))
          this
        }

        override protected def workerClusters(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => requiredWorkers += (key -> condition))
          this
        }

        override protected def streamApps(keys: Set[ObjectKey], condition: Option[Condition]): ChickList = {
          keys.foreach(key => requiredStreamApps += (key -> condition))
          this
        }

        override def allTopics(): ChickList = {
          this.requireAllTopics = true
          this
        }

        override def allConnectors(): ChickList = {
          this.requireAllConnectors = true
          this
        }

        override def allFiles(): ChickList = {
          this.requireAllFiles = true
          this
        }

        override def allNodes(): ChickList = {
          this.requireAllNodes = true
          this
        }

        override def allZookeepers(): ChickList = {
          this.requireAllZookeepers = true
          this
        }

        override def allBrokers(): ChickList = {
          this.requireAllBrokers = true
          this
        }

        override def allWorkers(): ChickList = {
          this.requireAllWorkers = true
          this
        }

        override def allStreamApps(): ChickList = {
          this.requireAllStreamApps = true
          this
        }
      }
    }
}
