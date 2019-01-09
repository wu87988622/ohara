package com.island.ohara.configurator.route

import java.io.File
import java.nio.file.FileAlreadyExistsException

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.StreamClient
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.StreamApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.island.ohara.configurator.route.RouteUtil._
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._

import scala.sys.process._

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

  private[this] def assertParameters[T <: Data](req: T): Boolean = {
    req match {
      case data: StreamData =>
        data.name.nonEmpty &&
          data.filePath.nonEmpty &&
          data.fromTopics.nonEmpty &&
          data.toTopics.nonEmpty &&
          data.id.nonEmpty &&
          data.pipeline_id.nonEmpty &&
          data.jarName.nonEmpty &&
          data.instances >= 1
    }
  }

  def apply(implicit store: Store, kafkaClient: KafkaClient): server.Route =
    pathPrefix(STREAM_PREFIX_PATH) {
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
                    val streamApp_id = CommonUtil.uuid()
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
            onSuccess(store.values[StreamData]) { values =>
              complete(
                values
                  .filter(f => f.pipeline_id.equals(id)) //note : this id is given by UI (pipeline_id)
                  .map(data => StreamJar(data.id, data.jarName, data.lastModified)))
            }
          } ~ delete {
            //TODO : check streamapp is not at running state
            assertNotRelated2Pipeline(id)
            onSuccess(store.remove[StreamData](id)) { data =>
              val f: File = new File(data.filePath)
              f.delete()
              complete(StreamJar(data.id, data.jarName, data.lastModified))
            }
          } ~ put {
            entity(as[StreamListRequest]) { req =>
              if (req.jarName == null || req.jarName.isEmpty) throw new IllegalArgumentException(s"Require jarName")
              onSuccess(
                store.update[StreamData](id,
                                         previous =>
                                           toStore(previous.pipeline_id,
                                                   req.jarName,
                                                   previous.name,
                                                   Seq.empty,
                                                   Seq.empty,
                                                   1,
                                                   id,
                                                   previous.filePath,
                                                   CommonUtil.current()))) { newData =>
                complete(StreamJar(newData.id, newData.jarName, newData.lastModified))
              }
            }
          }
        }
      } ~ pathPrefix(STREAM_PROPERTY_PREFIX_PATH) {
        //TODO: It supports "update" but exclude "delete" and "add"...WHY? by chia
        path(Segment) { id =>
          // get
          get {
            onSuccess(store.value[StreamData](id)) { data =>
              complete(
                StreamPropertyResponse(id,
                                       data.jarName,
                                       data.name,
                                       data.fromTopics,
                                       data.toTopics,
                                       data.instances,
                                       data.lastModified))
            }
          } ~
            // update
            put {
              entity(as[StreamPropertyRequest]) { req =>
                if (req.instances < 1)
                  throw new IllegalArgumentException(s"Require instances bigger or equal to 1")
                onSuccess(
                  store.update[StreamData](id,
                                           oldData =>
                                             toStore(oldData.pipeline_id,
                                                     oldData.jarName,
                                                     req.name,
                                                     req.fromTopics,
                                                     req.toTopics,
                                                     req.instances,
                                                     id,
                                                     oldData.filePath,
                                                     CommonUtil.current()))) { newData =>
                  complete(
                    StreamPropertyResponse(id,
                                           newData.jarName,
                                           newData.name,
                                           newData.fromTopics,
                                           newData.toTopics,
                                           newData.instances,
                                           newData.lastModified))
                }
              }
            }
        }
      } ~ pathPrefix(Segment) { id =>
        path(START_COMMAND) {
          put {
            onSuccess(store.value[StreamData](id)) { data =>
              if (!assertParameters(data))
                throw new IllegalArgumentException(
                  s"StreamData with id : ${data.id} not match the parameter requirement.")
              val checkDocker = "which docker" !!

              if (checkDocker.toLowerCase.contains("not found"))
                throw new RuntimeException(s"This machine is not support docker command !")

              //TODO : we hard code here currently. This should be called dynamically and run async ...by Sam
              val dockerCmd =
                s"""docker run -d -h "${data.name}" -v /home/docker/streamapp:/opt/ohara/streamapp --rm --name "${data.name}"
                     | -e STREAMAPP_SERVERS=${kafkaClient.brokers()}
                     | -e STREAMAPP_APPID=${data.name}
                     | -e STREAMAPP_FROMTOPIC=${data.fromTopics.head}
                     | -e STREAMAPP_TOTOPIC=${data.toTopics.head}
                     | ${StreamClient.STREAMAPP_IMAGE}
                     | "example.MyApp"
                          """.stripMargin

              // TODO: use LOG instead...by chia
              System.out.println(s"command : $dockerCmd")
              complete(if (Process(dockerCmd).run.exitValue() == 0) StatusCodes.OK else StatusCodes.BadRequest)
            }
          }
        } ~ path(STOP_COMMAND) {
          put {
            onSuccess(store.value[StreamData](id)) { data =>
              val checkDocker = "which docker" !!

              if (checkDocker.toLowerCase.contains("not found"))
                throw new RuntimeException(s"This machine is not support docker command !")

              //TODO : we hard code here currently. This should be called dynamically and run async ...by Sam
              val dockerCmd =
                s"""docker stop ${data.name}
               """.stripMargin
              complete(if (Process(dockerCmd).run.exitValue() == 0) StatusCodes.OK else StatusCodes.BadRequest)
            }
          }
        }
      }
    }
}
