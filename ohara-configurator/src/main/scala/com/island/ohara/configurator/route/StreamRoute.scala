package com.island.ohara.configurator.route

import java.io.File
import java.nio.file.{FileAlreadyExistsException, Paths}

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.StreamClient
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import spray.json.DefaultJsonProtocol._

private[configurator] object StreamRoute {

  private val TMP_ROOT = System.getProperty("java.io.tmpdir")
  private val JARS_ROOT = Paths.get(TMP_ROOT, "ohara_streams")

  private[this] def toRes(uuid: String, req: StreamJarUpdateRequest) =
    StreamObj(uuid, req.jarName, CommonUtil.current())

  def apply(implicit store: Store): server.Route =
    pathPrefix(STREAM_PATH) {
      pathPrefix(JARS_STREAM_PATH) {
        pathEnd {
          //add jars
          post {
            storeUploadedFiles("java", StreamClient.saveTmpFile) { files =>
              val objs = files.map {
                case (metadata, file) =>
                  val baseDir: File = new File(JARS_ROOT.toUri)
                  if (!baseDir.exists()) baseDir.mkdir()
                  val des: File = new File(JARS_ROOT.resolve(metadata.fileName).toUri)
                  val success = file.renameTo(des)
                  if (success) {
                    StreamObj(CommonUtil.uuid(), metadata.fileName, CommonUtil.current())
                  } else {
                    throw new FileAlreadyExistsException(s"The file: ${metadata.fileName} already exists")
                  }
              }
              objs.foreach(store.add)
              complete(StreamResponse(objs))
            }
          } ~ get(complete(store.data[StreamObj].toSeq))
        } ~ path(Segment) { uuid =>
          // delete
          delete {
            assertNotRelated2StreamApp(uuid)
            val d = store.remove[StreamObj](uuid)
            complete(d)
          } ~
            // update
            put {
              entity(as[StreamJarUpdateRequest]) { req =>
                val newData = toRes(uuid, req)
                store.update(newData)
                complete(newData)
              }
            }
        }
      }
    }
}
