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

package com.island.ohara.client.configurator.v0
import com.island.ohara.common.setting.ObjectKey
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

/**
  * the cluster-related data is different from normal data so we need another type of access.
  * @param prefixPath path to remote resource
  */
private[v0] abstract class ClusterAccess[Creation <: ClusterCreationRequest,
                                         Update <: ClusterUpdateRequest,
                                         Res <: ClusterInfo](prefixPath: String,
                                                             //TODO: remove this in #2570
                                                             defaultGroup: String)(
  implicit rm1: OharaJsonFormat[Creation],
  rm2: OharaJsonFormat[Update],
  rm3: OharaJsonFormat[Res])
    extends BasicAccess(prefixPath) {

  /**
    * generate a key for cluster
    * @param group cluster name
    * @param name cluster group
    * @return key of this cluster
    */
  private[v0] def key(group: String, name: String): ObjectKey = ObjectKey.of(group, name)

  final def post(creation: Creation)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.post[Creation, Res, ErrorApi.Error](url, creation)
  final def put(objectKey: ObjectKey, update: Update)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.put[Update, Res, ErrorApi.Error](url(objectKey), update)
  //TODO: remove this stale method in #2570
  final def get(clusterName: String)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.get[Res, ErrorApi.Error](url(key(defaultGroup, clusterName)))
  final def get(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.get[Res, ErrorApi.Error](url(objectKey))
  //TODO: remove this stale method in #2570
  final def delete(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](url(key(defaultGroup, clusterName)))
  final def delete(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](url(objectKey))
  final def list()(implicit executionContext: ExecutionContext): Future[Seq[Res]] =
    exec.get[Seq[Res], ErrorApi.Error](url)
  //TODO: remove this stale method in #2570
  final def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](url(key(defaultGroup, clusterName), nodeName))
  final def addNode(objectKey: ObjectKey, nodeName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](url(objectKey, nodeName))
  //TODO: remove this stale method in #2570
  final def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](url(key(defaultGroup, clusterName), nodeName))
  final def removeNode(objectKey: ObjectKey, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](url(objectKey, nodeName))

  /**
    *  start a cluster
    *
    * @param objectKey object key
    * @param executionContext execution context
    * @return none
    */
  final def start(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    put(objectKey, START_COMMAND)

  //TODO: remove this stale method in #2570
  final def start(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    put(key(defaultGroup, clusterName), START_COMMAND)

  /**
    * stop a cluster gracefully.
    *
    * @param objectKey object key
    * @param executionContext execution context
    * @return none
    */
  final def stop(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    put(objectKey, STOP_COMMAND)

  //TODO: remove this stale method in #2570
  final def stop(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    put(key(defaultGroup, clusterName), STOP_COMMAND)

  /**
    * force to stop a cluster.
    * This action may cause some data loss if cluster was still running.
    *
    * @param objectKey object key
    * @param executionContext execution context
    * @return none
    */
  final def forceStop(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](url(key = objectKey, postFix = STOP_COMMAND, params = Map(FORCE_KEY -> "true")))

  //TODO: remove this stale method in #2570
  final def forceStop(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](
      url(key = key(defaultGroup, clusterName), postFix = STOP_COMMAND, params = Map(FORCE_KEY -> "true")))
}
