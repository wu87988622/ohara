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

package oharastream.ohara.configurator.route

import oharastream.ohara.agent.{ClusterStatus, Collie, ServiceCollie}
import oharastream.ohara.client.Enum
import oharastream.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import oharastream.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import oharastream.ohara.client.configurator.v0.FileInfoApi.FileInfo
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.client.configurator.v0.ShabondiApi.ShabondiClusterInfo
import oharastream.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import oharastream.ohara.client.configurator.v0.TopicApi.TopicInfo
import oharastream.ohara.client.configurator.v0.VolumeApi.Volume
import oharastream.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import oharastream.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import oharastream.ohara.client.configurator.v0.{ClusterInfo, OBJECT_KEY_FORMAT}
import oharastream.ohara.common.setting.{ConnectorKey, ObjectKey, SettingDef, TopicKey}
import oharastream.ohara.configurator.route.ObjectChecker.CheckList
import oharastream.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import oharastream.ohara.configurator.store.DataStore
import spray.json.{JsArray, JsObject, JsString, JsValue}

import scala.collection.mutable
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Most routes do a great job - check the resource availability before starting it.
  * It means there are a lot of duplicate code used to check same resources in our routes. So this class is used to
  * unify all resource checks and produces unified error message.
  */
trait ObjectChecker {
  def checkList: CheckList
}

object ObjectChecker {
  case class ObjectInfos(
    topicInfos: Map[TopicInfo, Condition],
    volumes: Map[Volume, Condition],
    connectorInfos: Map[ConnectorInfo, Condition],
    fileInfos: Seq[FileInfo],
    nodes: Seq[Node],
    zookeeperClusterInfos: Map[ZookeeperClusterInfo, Condition],
    brokerClusterInfos: Map[BrokerClusterInfo, Condition],
    workerClusterInfos: Map[WorkerClusterInfo, Condition],
    streamClusterInfos: Map[StreamClusterInfo, Condition],
    shabondiClusterInfos: Map[ShabondiClusterInfo, Condition]
  ) {
    def runningTopics: Seq[TopicInfo]                = topicInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningConnectors: Seq[ConnectorInfo]        = connectorInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningZookeepers: Seq[ZookeeperClusterInfo] = zookeeperClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningBrokers: Seq[BrokerClusterInfo]       = brokerClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningWorkers: Seq[WorkerClusterInfo]       = workerClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningStreams: Seq[StreamClusterInfo]       = streamClusterInfos.filter(_._2 == RUNNING).keys.toSeq
    def runningShabondis: Seq[ShabondiClusterInfo]   = shabondiClusterInfos.filter(_._2 == RUNNING).keys.toSeq
  }

  trait CheckList {
    //---------------[generic]---------------//

    /**
      * check the value having reference to Ohara object.
      * Noted that this method check only the existent key-value. If the key is NOT existent, it still pass even if the
      * reference is required.
      * TODO: we should check all definitions in Creation phase
      * https://github.com/oharastream/ohara/issues/4506
      * @param settings raw setting
      * @param definitions definitions
      * @return this check list
      */
    def references(settings: Map[String, JsValue], definitions: Seq[SettingDef]): CheckList = {
      def add(obj: JsValue, definition: SettingDef): Unit = {
        obj match {
          case JsString(name) => reference(ObjectKey.of(GROUP_DEFAULT, name), definition.reference())
          case obj: JsObject  => reference(OBJECT_KEY_FORMAT.read(obj), definition.reference())
          case JsArray(objs)  => objs.foreach(obj => add(obj, definition))
          case _              => // nothing
        }
      }
      definitions
        .foreach(
          definition =>
            settings.get(definition.key()).foreach { obj =>
              definition.reference() match {
                case SettingDef.Reference.NONE => // skip
                case SettingDef.Reference.NODE if definition.valueType() == SettingDef.Type.ARRAY =>
                  add(obj, definition)
                case _
                    if definition.valueType() == SettingDef.Type.OBJECT_KEY || definition
                      .valueType() == SettingDef.Type.OBJECT_KEYS =>
                  add(obj, definition)
              }
            }
        )
      this
    }

    /**
      * make sure the object key is referenced to correct object.
      * @param objectKey object key
      * @param reference object reference
      * @return this check list
      */
    private[this] def reference(objectKey: ObjectKey, reference: SettingDef.Reference): CheckList =
      reference match {
        case SettingDef.Reference.NONE      => this
        case SettingDef.Reference.NODE      => node(objectKey)
        case SettingDef.Reference.TOPIC     => topic(TopicKey.of(objectKey.group(), objectKey.name()))
        case SettingDef.Reference.ZOOKEEPER => zookeeperCluster(objectKey)
        case SettingDef.Reference.BROKER    => brokerCluster(objectKey)
        case SettingDef.Reference.WORKER    => workerCluster(objectKey)
        case SettingDef.Reference.FILE      => file(objectKey)
        case SettingDef.Reference.VOLUME    => volume(objectKey)
        case _                              => throw new RuntimeException(s"$reference is NOT added to related check!!!")
      }

    //---------------[volume]---------------//
    /**
      * check all volumes. It invokes a loop to all volumes and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allVolumes(): CheckList

    /**
      * check the properties of volume.
      * @param key volume key
      * @return this check list
      */
    def volume(key: ObjectKey): CheckList = volumes(Set(key), None)

    /**
      * check both properties and status of volume.
      * @param key volume key
      * @return this check list
      */
    def volume(key: ObjectKey, condition: Condition): CheckList = volumes(Set(key), Some(condition))

    /**
      * check whether input volumes have been stored in Configurator
      * @param keys volume keys
      * @return this check list
      */
    def volumes(keys: Set[ObjectKey]): CheckList = volumes(keys, None)

    /**
      * check whether input volumes condition.
      * @param keys volume keys
      * @return this check list
      */
    def volumes(keys: Set[ObjectKey], condition: Condition): CheckList = volumes(keys, Some(condition))

    /**
      * set the volumes and condition to check.
      * @param keys volume keys
      * @param condition condition
      * @return check list
      */
    protected def volumes(keys: Set[ObjectKey], condition: Option[Condition]): CheckList
    //---------------[topic]---------------//

    /**
      * check all topics. It invokes a loop to all topics and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allTopics(): CheckList

    /**
      * check the properties of topic.
      * @param key topic key
      * @return this check list
      */
    def topic(key: TopicKey): CheckList = topics(Set(key), None)

    /**
      * check both properties and status of topic.
      * @param key topic key
      * @return this check list
      */
    def topic(key: TopicKey, condition: Condition): CheckList = topics(Set(key), Some(condition))

    /**
      * check whether input topics have been stored in Configurator
      * @param keys topic keys
      * @return this check list
      */
    def topics(keys: Set[TopicKey]): CheckList = topics(keys, None)

    /**
      * check whether input topics condition.
      * @param keys topic keys
      * @return this check list
      */
    def topics(keys: Set[TopicKey], condition: Condition): CheckList = topics(keys, Some(condition))

    /**
      * set the topics and condition to check.
      * @param keys topic keys
      * @param condition condition
      * @return check list
      */
    protected def topics(keys: Set[TopicKey], condition: Option[Condition]): CheckList

    //---------------[connector]---------------//

    /**
      * check all connectors. It invokes a loop to all connectors and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allConnectors(): CheckList

    /**
      * check the properties of connector.
      * @param key connector key
      * @return this check list
      */
    def connector(key: ConnectorKey): CheckList = connectors(Set(key), None)

    /**
      * check both properties and status of connector.
      * @param key connector key
      * @return this check list
      */
    def connector(key: ConnectorKey, condition: Condition): CheckList = connectors(Set(key), Some(condition))
    protected def connectors(keys: Set[ConnectorKey], condition: Option[Condition]): CheckList

    //---------------[file]---------------//
    /**
      * check all files.
      * @return check list
      */
    def allFiles(): CheckList

    /**
      * check the properties of file.
      * @param key file key
      * @return this check list
      */
    def file(key: ObjectKey): CheckList = files(Set(key))

    /**
      * check the properties of files.
      * @param keys files key
      * @return this check list
      */
    def files(keys: Set[ObjectKey]): CheckList

    //---------------[node]---------------//

    /**
      * check all nodes.
      * @return check list
      */
    def allNodes(): CheckList

    /**
      * check the properties of node.
      * @param hostname hostname
      * @return this check list
      */
    def nodeName(hostname: String): CheckList = nodeNames(Set(hostname))

    /**
      * check the properties of nodes.
      * @param hostNames node names
      * @return this check list
      */
    def nodeNames(hostNames: Set[String]): CheckList = nodes(hostNames.map(n => ObjectKey.of(GROUP_DEFAULT, n)))

    /**
      * check the properties of nodes.
      * @param key node key
      * @return this check list
      */
    def node(key: ObjectKey): CheckList = nodes(Set(key))

    /**
      * check the properties of nodes.
      * @param keys nodes key
      * @return this check list
      */
    def nodes(keys: Set[ObjectKey]): CheckList

    //---------------[zookeeper]---------------//

    /**
      * check all zookeepers. It invokes a loop to all zookeepers and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allZookeepers(): CheckList

    /**
      * check the properties of zookeeper cluster.
      * @param key zookeeper cluster key
      * @return this check list
      */
    def zookeeperCluster(key: ObjectKey): CheckList = zookeeperClusters(Set(key), None)

    /**
      * check both properties and status of zookeeper cluster.
      * @param key zookeeper cluster key
      * @return this check list
      */
    def zookeeperCluster(key: ObjectKey, condition: Condition): CheckList = zookeeperClusters(Set(key), Some(condition))

    protected def zookeeperClusters(keys: Set[ObjectKey], condition: Option[Condition]): CheckList

    //---------------[broker]---------------//

    /**
      * check all brokers. It invokes a loop to all brokers and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allBrokers(): CheckList

    /**
      * check the properties of broker cluster.
      * @param key broker cluster key
      * @return this check list
      */
    def brokerCluster(key: ObjectKey): CheckList = brokerClusters(Set(key), None)

    /**
      * check both properties and status of broker cluster.
      * @param key broker cluster key
      * @return this check list
      */
    def brokerCluster(key: ObjectKey, condition: Condition): CheckList = brokerClusters(Set(key), Some(condition))

    protected def brokerClusters(keys: Set[ObjectKey], condition: Option[Condition]): CheckList

    //---------------[worker]---------------//

    /**
      * check all workers. It invokes a loop to all workers and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allWorkers(): CheckList

    /**
      * check the properties of worker cluster.
      * @param key worker cluster key
      * @return this check list
      */
    def workerCluster(key: ObjectKey): CheckList = workerClusters(Set(key), None)

    /**
      * check both properties and status of worker cluster.
      * @param key worker cluster key
      * @return this check list
      */
    def workerCluster(key: ObjectKey, condition: Condition): CheckList = workerClusters(Set(key), Some(condition))

    protected def workerClusters(keys: Set[ObjectKey], condition: Option[Condition]): CheckList

    //---------------[stream app]---------------//

    /**
      * check all streams. It invokes a loop to all streams and then fetch their state - a expensive operation!!!
      * @return check list
      */
    def allStreams(): CheckList

    /**
      * check the properties of stream cluster.
      * @param key stream cluster key
      * @return this check list
      */
    def stream(key: ObjectKey): CheckList = streams(Set(key), None)

    /**
      * check both properties and status of stream cluster.
      * @param key stream cluster key
      * @return this check list
      */
    def stream(key: ObjectKey, condition: Condition): CheckList = streams(Set(key), Some(condition))

    protected def streams(keys: Set[ObjectKey], condition: Option[Condition]): CheckList

    //---------------[shabondi]---------------//

    def allShabondis(): CheckList

    def shabondi(key: ObjectKey): CheckList = shabondis(Set(key), None)

    def shabondi(key: ObjectKey, condition: Condition): CheckList = shabondis(Set(key), Some(condition))

    protected def shabondis(keys: Set[ObjectKey], condition: Option[Condition]): CheckList

    /**
      * throw exception if the input assurances don't pass. Otherwise, return the resources.
      * @param executionContext thread pool
      * @throws oharastream.ohara.configurator.route.ObjectCheckException it contains the first unmatched objects.
      *                                                                   You can seek the related information to address more follow-up actions.
      * @return resource
      */
    @throws(classOf[ObjectCheckException])
    def check()(implicit executionContext: ExecutionContext): Future[ObjectInfos]
  }

  sealed abstract class Condition
  object Condition extends Enum[Condition] {
    case object RUNNING extends Condition
    case object STOPPED extends Condition
  }

  def apply()(implicit store: DataStore, serviceCollie: ServiceCollie, adminCleaner: AdminCleaner): ObjectChecker =
    new ObjectChecker {
      override def checkList: CheckList = new CheckList {
        private[this] var requireAllNodes      = false
        private[this] val requiredNodes        = mutable.Set[ObjectKey]()
        private[this] var requireAllFiles      = false
        private[this] val requiredFiles        = mutable.Set[ObjectKey]()
        private[this] var requireAllTopics     = false
        private[this] val requiredTopics       = mutable.Map[TopicKey, Option[Condition]]()
        private[this] var requireAllVolumes    = false
        private[this] val requiredVolumes      = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllConnectors = false
        private[this] val requiredConnectors   = mutable.Map[ConnectorKey, Option[Condition]]()
        private[this] var requireAllZookeepers = false
        private[this] val requiredZookeepers   = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllBrokers    = false
        private[this] val requiredBrokers      = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllWorkers    = false
        private[this] val requiredWorkers      = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllStreams    = false
        private[this] val requiredStreams      = mutable.Map[ObjectKey, Option[Condition]]()
        private[this] var requireAllShabondis  = false
        private[this] val requiredShabondis    = mutable.Map[ObjectKey, Option[Condition]]()

        private[this] def checkCluster[C <: ClusterInfo: ClassTag](
          collie: Collie,
          key: ObjectKey
        )(implicit executionContext: ExecutionContext): Future[Option[(C, Condition)]] =
          store.get[C](key).flatMap {
            case None          => Future.successful(None)
            case Some(cluster) =>
              // TODO: currently the existence of cluster implies the cluster is running. However, it would be better
              // to check the state of cluster as well.
              collie.exist(key).map(if (_) RUNNING else STOPPED).map(condition => Some(cluster -> condition))
          }

        private[this] def checkClusters[S <: ClusterStatus, C <: ClusterInfo: ClassTag](
          collie: Collie,
          keys: Set[ObjectKey]
        )(implicit executionContext: ExecutionContext): Future[Map[C, Condition]] =
          Future
            .traverse(keys) { key =>
              checkCluster[C](collie, key)
            }
            .map(_.flatten.toMap)

        private[this] def checkZookeepers()(
          implicit executionContext: ExecutionContext
        ): Future[Map[ZookeeperClusterInfo, Condition]] =
          if (requireAllZookeepers)
            store
              .values[ZookeeperClusterInfo]()
              .map(_.map(_.key))
              .flatMap(
                keys => checkClusters[ClusterStatus, ZookeeperClusterInfo](serviceCollie.zookeeperCollie, keys.toSet)
              )
          else
            checkClusters[ClusterStatus, ZookeeperClusterInfo](
              serviceCollie.zookeeperCollie,
              requiredZookeepers.keys.toSet
            )

        private[this] def checkBrokers()(
          implicit executionContext: ExecutionContext
        ): Future[Map[BrokerClusterInfo, Condition]] =
          if (requireAllBrokers)
            store
              .values[BrokerClusterInfo]()
              .map(_.map(_.key))
              .flatMap(
                keys => checkClusters[ClusterStatus, BrokerClusterInfo](serviceCollie.brokerCollie, keys.toSet)
              )
          else
            checkClusters[ClusterStatus, BrokerClusterInfo](
              serviceCollie.brokerCollie,
              requiredBrokers.keys.toSet
            )

        private[this] def checkWorkers()(
          implicit executionContext: ExecutionContext
        ): Future[Map[WorkerClusterInfo, Condition]] =
          if (requireAllWorkers)
            store
              .values[WorkerClusterInfo]()
              .map(_.map(_.key))
              .flatMap(
                keys => checkClusters[ClusterStatus, WorkerClusterInfo](serviceCollie.workerCollie, keys.toSet)
              )
          else
            checkClusters[ClusterStatus, WorkerClusterInfo](
              serviceCollie.workerCollie,
              requiredWorkers.keys.toSet
            )

        private[this] def checkStreams()(
          implicit executionContext: ExecutionContext
        ): Future[Map[StreamClusterInfo, Condition]] =
          if (requireAllStreams)
            store
              .values[StreamClusterInfo]()
              .map(_.map(_.key))
              .flatMap(
                keys => checkClusters[ClusterStatus, StreamClusterInfo](serviceCollie.streamCollie, keys.toSet)
              )
          else
            checkClusters[ClusterStatus, StreamClusterInfo](
              serviceCollie.streamCollie,
              requiredStreams.keys.toSet
            )

        private[this] def checkShabondis()(
          implicit executionContext: ExecutionContext
        ): Future[Map[ShabondiClusterInfo, Condition]] =
          if (requireAllShabondis)
            store
              .values[ShabondiClusterInfo]()
              .map(_.map(_.key))
              .flatMap(
                keys => checkClusters[ClusterStatus, ShabondiClusterInfo](serviceCollie.shabondiCollie, keys.toSet)
              )
          else
            checkClusters[ClusterStatus, ShabondiClusterInfo](
              serviceCollie.shabondiCollie,
              requiredShabondis.keys.toSet
            )

        private[this] def checkTopic(
          key: TopicKey
        )(implicit executionContext: ExecutionContext): Future[Option[(TopicInfo, Condition)]] =
          store.get[TopicInfo](key).flatMap {
            case None => Future.successful(None)
            case Some(topicInfo) =>
              checkCluster[BrokerClusterInfo](
                serviceCollie.brokerCollie,
                topicInfo.brokerClusterKey
              ).flatMap {
                case None => Future.successful(Some(topicInfo -> STOPPED))
                case Some((brokerClusterInfo, condition)) =>
                  condition match {
                    case STOPPED => Future.successful(Some(topicInfo -> STOPPED))
                    case RUNNING =>
                      topicAdmin(brokerClusterInfo)(serviceCollie.brokerCollie, adminCleaner, executionContext)
                      // make sure the topic admin is closed!!!
                        .flatMap(_.exist(topicInfo.key).toScala)
                        .map(existent => if (existent) Some(topicInfo -> RUNNING) else Some(topicInfo -> STOPPED))
                  }
              }
          }

        private[this] def checkTopics()(
          implicit executionContext: ExecutionContext
        ): Future[Map[TopicInfo, Condition]] =
          if (requireAllTopics) store.values[TopicInfo]().map(_.map(_.key)).flatMap { keys =>
            Future.traverse(keys)(checkTopic).map(_.flatten.toMap)
          } else Future.traverse(requiredTopics.keySet)(checkTopic).map(_.flatten.toMap)

        private[this] def checkVolumes()(
          implicit executionContext: ExecutionContext
        ): Future[Map[Volume, Condition]] =
          if (requireAllVolumes) store.values[Volume]().map(_.map(_.key)).flatMap { keys =>
            Future.traverse(keys)(checkVolume).map(_.flatten.toMap)
          } else Future.traverse(requiredVolumes.keySet)(checkVolume).map(_.flatten.toMap)

        private[this] def checkVolume(
          key: ObjectKey
        )(implicit executionContext: ExecutionContext): Future[Option[(Volume, Condition)]] =
          store.get[Volume](key).flatMap {
            case None => Future.successful(None)
            case Some(volume) =>
              serviceCollie
                .volumes()
                .map(_.filter(_.key == volume.key))
                .map(existentVolumes => Some(volume -> (if (existentVolumes.isEmpty) STOPPED else RUNNING)))
          }

        private[this] def checkConnector(
          key: ConnectorKey
        )(implicit executionContext: ExecutionContext): Future[Option[(ConnectorInfo, Condition)]] =
          store.get[ConnectorInfo](key).flatMap {
            case None => Future.successful(None)
            case Some(connectorInfo) =>
              checkCluster[WorkerClusterInfo](
                serviceCollie.workerCollie,
                connectorInfo.workerClusterKey
              ).flatMap {
                case None => Future.successful(Some(connectorInfo -> STOPPED))
                case Some((workerClusterInfo, condition)) =>
                  condition match {
                    case STOPPED => Future.successful(Some(connectorInfo -> STOPPED))
                    case RUNNING =>
                      serviceCollie.workerCollie
                        .connectorAdmin(workerClusterInfo)
                        .flatMap(_.activeConnectors())
                        .map(_.contains(key))
                        .map(if (_) RUNNING else STOPPED)
                        .map(condition => Some(connectorInfo -> condition))
                  }
              }
          }

        private[this] def checkConnectors()(
          implicit executionContext: ExecutionContext
        ): Future[Map[ConnectorInfo, Condition]] =
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

        private[this] def compare(
          name: String,
          result: Map[ObjectKey, Condition],
          required: Map[ObjectKey, Option[Condition]]
        ): Unit = {
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
                volumes = Map.empty,
                connectorInfos = Map.empty,
                fileInfos = passed,
                nodes = Seq.empty,
                zookeeperClusterInfos = Map.empty,
                brokerClusterInfos = Map.empty,
                workerClusterInfos = Map.empty,
                streamClusterInfos = Map.empty,
                shabondiClusterInfos = Map.empty
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
            // check streams
            .flatMap { report =>
              checkStreams().map { passed =>
                compare("stream", passed.map(e => e._1.key -> e._2), requiredStreams.toMap)
                report.copy(streamClusterInfos = passed)
              }
            }
            // check shabondis
            .flatMap { report =>
              checkShabondis().map { passed =>
                compare("shabondi", passed.map(e => e._1.key -> e._2), requiredShabondis.toMap)
                report.copy(shabondiClusterInfos = passed)
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
            // check volumes
            .flatMap { report =>
              checkVolumes().map { passed =>
                compare("volume", passed.map(e => e._1.key -> e._2), requiredVolumes.toMap)
                report.copy(volumes = passed)
              }
            }
            // check connectors
            .flatMap { report =>
              checkConnectors().map { passed =>
                compare("connector", passed.map(e => e._1.key -> e._2).toMap, requiredConnectors.toMap)
                report.copy(connectorInfos = passed)
              }
            }

        override protected def topics(keys: Set[TopicKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredTopics += (key -> condition))
          this
        }

        override protected def volumes(keys: Set[ObjectKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredVolumes += (key -> condition))
          this
        }

        override protected def connectors(keys: Set[ConnectorKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredConnectors += (key -> condition))
          this
        }

        override def nodes(keys: Set[ObjectKey]): CheckList = {
          requiredNodes ++= keys
          this
        }

        override def files(keys: Set[ObjectKey]): CheckList = {
          requiredFiles ++= keys
          this
        }

        override protected def zookeeperClusters(keys: Set[ObjectKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredZookeepers += (key -> condition))
          this
        }

        override protected def brokerClusters(keys: Set[ObjectKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredBrokers += (key -> condition))
          this
        }

        override protected def workerClusters(keys: Set[ObjectKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredWorkers += (key -> condition))
          this
        }

        override protected def streams(keys: Set[ObjectKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredStreams += (key -> condition))
          this
        }

        override protected def shabondis(keys: Set[ObjectKey], condition: Option[Condition]): CheckList = {
          keys.foreach(key => requiredShabondis += (key -> condition))
          this
        }

        override def allTopics(): CheckList = {
          this.requireAllTopics = true
          this
        }

        override def allConnectors(): CheckList = {
          this.requireAllConnectors = true
          this
        }

        override def allFiles(): CheckList = {
          this.requireAllFiles = true
          this
        }

        override def allNodes(): CheckList = {
          this.requireAllNodes = true
          this
        }

        override def allZookeepers(): CheckList = {
          this.requireAllZookeepers = true
          this
        }

        override def allBrokers(): CheckList = {
          this.requireAllBrokers = true
          this
        }

        override def allWorkers(): CheckList = {
          this.requireAllWorkers = true
          this
        }

        override def allStreams(): CheckList = {
          this.requireAllStreams = true
          this
        }

        override def allShabondis(): CheckList = {
          this.requireAllShabondis = true
          this
        }

        override def allVolumes(): CheckList = {
          this.requireAllVolumes = true
          this
        }
      }
    }
}
