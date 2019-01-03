package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.CommonUtil
import spray.json.DefaultJsonProtocol._
object NodeRoute {

  private[this] def update(res: Node)(implicit clusterCollie: ClusterCollie): Node = res.copy(
    services = Seq(
      NodeService(
        name = "zookeeper",
        clusterNames = clusterCollie.zookeepersCollie().filter(_.nodeNames.contains(res.name)).map(_.name).toSeq
      ),
      NodeService(
        name = "broker",
        // TODO: see OHARA-1063
        clusterNames = Seq.empty
      ),
      NodeService(
        name = "connect-worker",
        // TODO: see OHARA-1064
        clusterNames = Seq.empty
      )
    )
  )
  def apply(implicit nodeCollie: NodeCollie, clusterCollie: ClusterCollie): server.Route = pathPrefix(NODE_PATH) {
    pathEnd {
      // add
      post {
        entity(as[NodeRequest]) { req =>
          val node = update(
            Node(
              name = req.name.get,
              port = req.port,
              user = req.user,
              password = req.password,
              services = Seq.empty,
              lastModified = CommonUtil.current()
            ))
          nodeCollie.add(node)
          complete(node)
        }
      } ~ get(complete(nodeCollie.iterator.map(update).toSeq)) // list
    } ~ path(Segment) { name =>
      // get
      get(complete(update(nodeCollie.node(name)))) ~
        // delete
        delete(complete(update(nodeCollie.remove(name)))) ~
        // update
        put {
          entity(as[NodeRequest]) { req =>
            val oldNode = nodeCollie.node(name)
            if (req.name.exists(_ != name))
              throw new IllegalArgumentException(
                s"the name from request is conflict with previous setting:${oldNode.name}")
            val newNode = update(
              Node(
                name = oldNode.name,
                port = req.port,
                user = req.user,
                password = req.password,
                services = Seq.empty,
                lastModified = CommonUtil.current()
              ))
            nodeCollie.update(newNode)
            complete(newNode)
          }
        }
    }
  }
}
