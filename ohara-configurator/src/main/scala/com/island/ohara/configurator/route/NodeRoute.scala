package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.NodeCollie
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.CommonUtil
import spray.json.DefaultJsonProtocol._
object NodeRoute {

  private[this] def toRes(request: NodeRequest): Node = Node(
    name = request.name.get,
    port = request.port,
    user = request.user,
    password = request.password,
    lastModified = CommonUtil.current()
  )
  def apply(implicit nodeCollie: NodeCollie): server.Route = pathPrefix(NODE_PATH) {
    pathEnd {
      // add
      post {
        entity(as[NodeRequest]) { req =>
          val node = toRes(req)
          nodeCollie.add(node)
          complete(node)
        }
      } ~ get(complete(nodeCollie.iterator.toSeq)) // list
    } ~ path(Segment) { name =>
      // get
      get(complete(nodeCollie.node(name))) ~
        // delete
        delete(complete(nodeCollie.remove(name))) ~
        // update
        put {
          entity(as[NodeRequest]) { req =>
            val oldNode = nodeCollie.node(name)
            if (req.name.exists(_ != name))
              throw new IllegalArgumentException(
                s"the name from request is conflict with previous setting:${oldNode.name}")
            val newNode = Node(
              name = oldNode.name,
              port = req.port,
              user = req.user,
              password = req.password,
              lastModified = CommonUtil.current()
            )
            nodeCollie.update(newNode)
            complete(newNode)
          }
        }
    }
  }
}
