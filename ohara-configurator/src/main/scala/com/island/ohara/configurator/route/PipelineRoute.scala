package com.island.ohara.configurator.route

import BasicRoute._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson.{Pipeline, _}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.util.SystemUtil
import spray.json.DefaultJsonProtocol._

private[configurator] object PipelineRoute {

  private[this] val ACCEPTED_TYPES_FROM = Seq(classOf[TopicInfo], classOf[Source])
  private[this] val ACCEPTED_TYPES_TO = Seq(classOf[TopicInfo], classOf[Sink])

  private[this] def toRes(uuid: String, request: PipelineRequest)(implicit store: Store) =
    Pipeline(uuid, request.name, request.rules, abstracts(request), SystemUtil.current())

  private[this] def checkExist(uuids: Set[String])(implicit store: Store): Unit = {
    uuids.foreach(uuid => if (!store.exist(uuid)) throw new IllegalArgumentException(s"the uuid:$uuid does not exist"))
  }

  private[this] def abstracts(request: PipelineRequest)(implicit store: Store): Seq[ObjectAbstract] = {
    val keys = request.rules.keys.toSet
    checkExist(keys)
    val values = request.rules.values.filterNot(_ == UNKNOWN).toSet
    checkExist(values)
    store
      .raw()
      .filter(data => keys.contains(data.uuid) || values.contains(data.uuid))
      .map {
        case statableData: StatableData =>
          ObjectAbstract(statableData.uuid, statableData.name, statableData.kind, statableData.state)
        case data => ObjectAbstract(data.uuid, data.name, data.kind, None)
      }
      .toList // NOTED: we have to return a "serializable" list!!!
  }

  private[this] def verifyRules(pipeline: Pipeline)(implicit store: Store): Unit = {
    def verifyFrom(uuid: String): Unit = {
      val data = store.raw(uuid)
      if (!ACCEPTED_TYPES_FROM.contains(data.getClass))
        throw new IllegalArgumentException(
          s"the type:${data.getClass.getSimpleName} can't be applied to pipeline." +
            s" accepted type:${ACCEPTED_TYPES_FROM.map(_.getSimpleName).mkString(",")}")
    }
    def verifyTo(uuid: String): Unit = {
      val data = store.raw(uuid)
      if (!ACCEPTED_TYPES_TO.contains(data.getClass))
        throw new IllegalArgumentException(
          s"the type:${data.getClass.getSimpleName} can't be applied to pipeline." +
            s" accepted type:${ACCEPTED_TYPES_TO.map(_.getSimpleName).mkString(",")}")
    }
    pipeline.rules.keys.foreach(verifyFrom)
    pipeline.rules.values.filterNot(_ == UNKNOWN).foreach(verifyTo)
    pipeline.rules.foreach {
      case (k, v) => if (k == v) throw new IllegalArgumentException(s"the from:$k can't be equals to to:$v")
    }
  }

  def apply(implicit store: Store, uuidGenerator: () => String): server.Route =
    pathPrefix(PIPELINE_PATH) {
      pathEnd {
        // add
        post {
          entity(as[PipelineRequest]) { req =>
            val pipeline = toRes(uuidGenerator(), req)
            verifyRules(pipeline)
            store.add(pipeline)
            complete(pipeline)
          }
        } ~ get(complete(store.data[Pipeline].toSeq)) // list
      } ~ pathPrefix(Segment) { uuid =>
        pathEnd {
          // get
          get(complete(store.data[Pipeline](uuid))) ~
            // delete
            delete(complete {
              store.remove[Pipeline](uuid)
            }) ~
            // update
            put {
              entity(as[PipelineRequest]) { req =>
                val pipeline = toRes(uuid, req)
                verifyRules(pipeline)
                store.update(pipeline)
                complete(pipeline)
              }
            }
        }
      }
    }
}
