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

package com.island.ohara.agent.wharf.stream
import java.util.Objects

import com.island.ohara.agent.wharf.{Crane, Warehouse}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.annotations.VisibleForTesting

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

final class StreamCrane(nodes: Seq[Node], props: StreamCraneProps) extends Crane {

  /**
    * warehouse list, group by warehouse name
    */
  private[this] val warehouses = new mutable.HashMap[String, StreamWarehouse]
  private[this] var isFake: Boolean = false

  override def list()(implicit executionContext: ExecutionContext): Future[Seq[Warehouse]] = {
    Future.successful(warehouses.values.toSeq)
  }

  override def add(size: Int)(implicit executionContext: ExecutionContext): Future[(String, Warehouse)] = {
    Future
      .successful(nodes)
      .map { nodes =>
        if (nodes.size < size || size < 1)
          throw new RuntimeException(s"the required size is illegal : ${nodes.size}")
        Random.shuffle(nodes).take(size)
      }
      .flatMap(takeNodes =>
        // appId is unique for each streamApp component (i.e., warehouse)
        warehouses.find(_._1 == props.appId) match {
          case Some(c) => Future.successful(c)
          case None =>
            val warehouse = StreamWarehouse(
              nodes = takeNodes,
              warehouseName = props.appId,
              imageName = props.imageName,
              jarUrl = props.jarUrl,
              appId = props.appId,
              brokerList = props.brokerList,
              fromTopics = props.fromTopics,
              toTopics = props.toTopics,
              isFake = isFake
            )
            warehouse
              .add(takeNodes.map(_.name))
              .map(_ => {
                warehouses += (props.appId -> warehouse)
                props.appId -> warehouse
              })
      })
  }

  override def delete(name: String)(implicit executionContext: ExecutionContext): Future[Warehouse] = {
    Future.successful(warehouses).flatMap { warehouses =>
      warehouses
        .getOrElseUpdate(name, throw new RuntimeException(s"required warehouse not exists : $name"))
        // remove warehouse first
        .deleteAll()
        .map { _ =>
          // then remove element from collection
          warehouses.remove(name).get
        }
    }
  }

  /**
    * Use to test container operations without docker environment
    *
    * @return this StreamCrane
    */
  @VisibleForTesting
  private[stream] def fake(): StreamCrane = {
    isFake = true
    this
  }
}

object StreamCrane {

  def builder(): Builder = new Builder

  private[stream] class Builder {
    private[this] var _props: StreamCraneProps = _
    private[this] var _nodes: Seq[Node] = Seq.empty

    def props(props: StreamCraneProps): Builder = {
      this._props = Objects.requireNonNull(props)
      this
    }

    def nodes(nodes: Seq[Node]): Builder = {
      this._nodes = Objects.requireNonNull(nodes)
      this
    }

    def build(): StreamCrane = new StreamCrane(_nodes, _props)
  }
}

sealed case class StreamCraneProps(imageName: String,
                                   jarUrl: String,
                                   appId: String,
                                   brokerList: Seq[String],
                                   fromTopics: Seq[String],
                                   toTopics: Seq[String])
