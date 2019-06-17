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

package com.island.ohara.agent
import com.island.ohara.client.configurator.v0.NodeApi.Node

import scala.concurrent.{ExecutionContext, Future}

/**
  * manager of nodes. All nodes are managed by ssh. It means the communication between nodes are through ssh.
  * However, we have developed another manager implementation based on k8s. Hence, the structure of node will be changed
  * in the future...
  */
trait NodeCollie {
  def nodes()(implicit executionContext: ExecutionContext): Future[Seq[Node]]
  def node(name: String)(implicit executionContext: ExecutionContext): Future[Node]
  def nodes(names: Set[String])(implicit executionContext: ExecutionContext): Future[Seq[Node]] =
    Future.traverse(names)(node).map(_.toSeq)
  def exist(name: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    node(name).map(_ => true).recover {
      case _: Throwable => false
    }
  def exist(names: Set[String])(implicit executionContext: ExecutionContext): Future[Boolean] =
    nodes(names).map(_ => true).recover {
      case _: Throwable => false
    }
}

object NodeCollie {

  /**
    * create a node collie based on fixed nodes
    * @param _nodes input nodes
    * @return node collie implementation
    */
  def apply(_nodes: Seq[Node]): NodeCollie = new NodeCollie {
    override def node(name: String)(implicit executionContext: ExecutionContext): Future[Node] =
      nodes().map(_.find(_.name == name).getOrElse(throw new NoSuchElementException(s"$name doesn't exist")))
    override def nodes()(implicit executionContext: ExecutionContext): Future[Seq[Node]] = Future.successful(_nodes)
  }
}
