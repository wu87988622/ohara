package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object HadoopApi {
  val HDFS_PREFIX_PATH: String = "hdfs"
  final case class HdfsInfoRequest(name: String, uri: String)
  implicit val HDFS_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[HdfsInfoRequest] = jsonFormat2(HdfsInfoRequest)

  final case class HdfsInfo(id: String, name: String, uri: String, lastModified: Long) extends Data {
    override def kind: String = "hdfs"
  }
  implicit val HDFS_INFO_JSON_FORMAT: RootJsonFormat[HdfsInfo] = jsonFormat4(HdfsInfo)

  def access(): Access[HdfsInfoRequest, HdfsInfo] =
    new Access[HdfsInfoRequest, HdfsInfo](HDFS_PREFIX_PATH)
}
