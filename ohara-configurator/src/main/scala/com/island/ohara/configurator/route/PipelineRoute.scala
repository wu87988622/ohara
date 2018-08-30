package com.island.ohara.configurator.route

import BasicRoute._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson.{Pipeline, _}
import com.island.ohara.configurator.Configurator.Store

private[configurator] object PipelineRoute {

  private[this] val ACCEPTED_TYPES = Seq(classOf[TopicInfo], classOf[Source], classOf[Sink])

  private[this] def toRes(uuid: String, request: PipelineRequest)(implicit store: Store) =
    Pipeline(uuid, request.name, Status.STOPPED, request.rules, abstracts(request), System.currentTimeMillis())

  private[this] def checkExist(uuids: Set[String])(implicit store: Store): Unit = {
    val existedUuids = store.raw().map(_.uuid)
    uuids.foreach(uuid =>
      if (!existedUuids.contains(uuid)) throw new IllegalArgumentException(s"the uuid:$uuid does not exist"))
  }

  private[this] def abstracts(request: PipelineRequest)(implicit store: Store): Seq[ComponentAbstract] = {
    val keys = request.rules.keys.toSet
    checkExist(keys)
    val values = request.rules.values.filterNot(_ == UNKNOWN).toSet
    checkExist(values)
    store
      .raw()
      .filter(data => keys.contains(data.uuid) || values.contains(data.uuid))
      .map(data => ComponentAbstract(data.uuid, data.name, data.kind))
      .toList // NOTED: we have to return a "serializable" list!!!
  }

  private[this] def assertNotRunning(pipeline: Pipeline): Unit = if (pipeline.status == Status.RUNNING)
    throw new IllegalArgumentException(s"the pipeline:${pipeline.uuid} is already running")

  private[this] def assertNotStopped(pipeline: Pipeline): Unit = if (pipeline.status == Status.STOPPED)
    throw new IllegalArgumentException(s"the pipeline:${pipeline.uuid} is already stopped")

  private[this] def verifyReady(pipeline: Pipeline): Unit =
    if (!pipeline.ready()) throw new IllegalArgumentException(s"pipeline has unready rules:${pipeline.rules}")

  private[this] def verifyRules(pipeline: Pipeline)(implicit store: Store): Unit = {
    def verify(uuid: String): Unit = {
      val data = store.raw(uuid)
      if (!ACCEPTED_TYPES.exists(_ == data.getClass))
        throw new IllegalArgumentException(
          s"the type:${data.getClass.getSimpleName} can't be applied to pipeline." +
            s" accepted type:${ACCEPTED_TYPES.map(_.getSimpleName).mkString(",")}")
    }
    pipeline.rules.keys.foreach(verify(_))
    pipeline.rules.values.filterNot(_ == UNKNOWN).foreach(verify(_))
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
            store.add(pipeline.uuid, pipeline)
            complete(pipeline)
          }
        } ~ get(complete(store.data[Pipeline].toSeq)) // list
      } ~ pathPrefix(Segment) { uuid =>
        pathEnd {
          // get
          get(complete(store.data[Pipeline](uuid))) ~
            // delete
            delete(complete {
              assertNotRunning(store.data[Pipeline](uuid))
              store.remove[Pipeline](uuid)
            }) ~
            // update
            put {
              entity(as[PipelineRequest]) { req =>
                val pipeline = toRes(uuid, req)
                assertNotRunning(store.data[Pipeline](uuid))
                verifyRules(pipeline)
                store.update(uuid, pipeline)
                complete(pipeline)
              }
            }
        } ~ path("start") {
          put {
            val oldPipeline = store.data[Pipeline](uuid)
            assertNotRunning(oldPipeline)
            verifyRules(oldPipeline)
            verifyReady(oldPipeline)
            val newPipeline = oldPipeline.copy(status = Status.RUNNING, lastModified = System.currentTimeMillis())
            store.update(uuid, newPipeline)
            complete(newPipeline)
          }
        } ~ path("stop") {
          put {
            val oldPipeline = store.data[Pipeline](uuid)
            assertNotStopped(oldPipeline)
            val newPipeline = oldPipeline.copy(status = Status.STOPPED, lastModified = System.currentTimeMillis())
            store.update(uuid, newPipeline)
            complete(newPipeline)
          }
        }

      }
    }
}
