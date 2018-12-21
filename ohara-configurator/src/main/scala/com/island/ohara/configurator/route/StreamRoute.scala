package com.island.ohara.configurator.route

import java.io.File
import java.nio.file.FileAlreadyExistsException

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.StreamClient
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import spray.json.DefaultJsonProtocol._

private[configurator] object StreamRoute {

  private[this] def toStore(pipeline_id: String,
                            jarName: String,
                            name: String,
                            fromTopics: Seq[String],
                            toTopics: Seq[String],
                            instances: Int,
                            id: String,
                            filePath: String,
                            lastModified: Long): StreamData =
    StreamData(pipeline_id, jarName, name, fromTopics, toTopics, instances, id, filePath, lastModified)

  def apply(implicit store: Store, idGenerator: () => String): server.Route =
    pathPrefix(STREAM_PATH) {
      pathPrefix(JARS_STREAM_PATH) {
        path(Segment) { id =>
          //add jars
          post {
            val baseDir: File = new File(StreamClient.JARS_ROOT.toUri)
            if (!baseDir.exists()) baseDir.mkdir()
            storeUploadedFiles(StreamClient.INPUT_KEY, StreamClient.saveTmpFile) { files =>
              val jars = files.map {
                case (metadata, file) =>
                  if (file.length() > StreamClient.MAX_FILE_SIZE) {
                    file.deleteOnExit()
                    throw new IllegalArgumentException(
                      s"The file : ${metadata.fileName} size is bigger than ${StreamClient.MAX_FILE_SIZE / 1024 / 1024} MB.")
                  }
                  val des: File = new File(StreamClient.JARS_ROOT.resolve(metadata.fileName).toUri)
                  val success = file.renameTo(des)
                  if (success) {
                    val time = CommonUtil.current()
                    val streamApp_id = idGenerator()
                    val data = toStore(id, //note : this id is given by UI (pipeline_id)
                                       metadata.fileName,
                                       "",
                                       Seq.empty,
                                       Seq.empty,
                                       1,
                                       streamApp_id,
                                       des.getAbsolutePath,
                                       time)
                    store.add(data)
                    StreamJar(streamApp_id, metadata.fileName, time)
                  } else {
                    throw new FileAlreadyExistsException(s"The file: ${metadata.fileName} already exists")
                  }
              }
              complete(StreamListResponse(jars))
            }
          } ~ get {
            val jars = store
              .data[StreamData]
              .filter(f => f.pipeline_id.equals(id)) //note : this id is given by UI (pipeline_id)
              .map(data => StreamJar(data.id, data.jarName, data.lastModified))
              .toSeq
            complete(jars)
          } ~ delete {
            //TODO : check streamapp is not at running state
            assertNotRelated2Pipeline(id)
            if (!store.exist[StreamData](id))
              throw new NoSuchElementException(s"The require element : $id does not exist.")
            val data = store.remove[StreamData](id)
            val f: File = new File(data.filePath)
            f.delete()
            complete(StreamJar(data.id, data.jarName, data.lastModified))
          } ~ put {
            entity(as[StreamListRequest]) { req =>
              if (req.jarName == null || req.jarName.isEmpty) throw new IllegalArgumentException(s"Require jarName")
              if (!store.exist[StreamData](id))
                throw new NoSuchElementException(s"The require element : $id does not exist.")
              val oldData = store.data[StreamData](id)
              val newData =
                toStore(oldData.pipeline_id,
                        req.jarName,
                        oldData.name,
                        Seq.empty,
                        Seq.empty,
                        1,
                        id,
                        oldData.filePath,
                        CommonUtil.current())
              store.update[StreamData](newData)
              complete(StreamJar(newData.id, newData.jarName, newData.lastModified))
            }
          }
        }
      } ~
        pathPrefix(PROPERTY_STREAM_PATH) {
          path(Segment) { id =>
            // get
            get {
              if (!store.exist[StreamData](id))
                throw new NoSuchElementException(s"The require element : $id does not exist.")
              val data = store.data[StreamData](id)
              val res = StreamPropertyResponse(id,
                                               data.jarName,
                                               data.name,
                                               data.fromTopics,
                                               data.toTopics,
                                               data.instances,
                                               data.lastModified)
              complete(res)
            } ~
              // update
              put {
                entity(as[StreamPropertyRequest]) { req =>
                  if (req.instances < 1)
                    throw new IllegalArgumentException(s"Require instances bigger or equal to 1")
                  if (!store.exist[StreamData](id))
                    throw new NoSuchElementException(s"The require element : $id does not exist.")
                  val oldData = store.data[StreamData](id)
                  val newData = toStore(oldData.pipeline_id,
                                        oldData.jarName,
                                        req.name,
                                        req.fromTopics,
                                        req.toTopics,
                                        req.instances,
                                        id,
                                        oldData.filePath,
                                        CommonUtil.current())
                  val res = StreamPropertyResponse(id,
                                                   newData.jarName,
                                                   newData.name,
                                                   newData.fromTopics,
                                                   newData.toTopics,
                                                   newData.instances,
                                                   newData.lastModified)
                  store.update[StreamData](newData)
                  complete(res)
                }
              }
          }
        }
    }
}
