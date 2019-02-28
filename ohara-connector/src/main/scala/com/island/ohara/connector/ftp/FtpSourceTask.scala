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

package com.island.ohara.connector.ftp

import java.util

import com.island.ohara.client.FtpClient
import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.connector.ftp.FtpSource.LOG
import com.island.ohara.connector.ftp.FtpSourceTask._
import com.island.ohara.kafka.connector.{RowSourceContext, RowSourceRecord, RowSourceTask, TaskConfig}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Move files from FTP server to Kafka topics. The file format must be csv file, and element in same line must be separated
  * by comma. The offset is (path, line index). It means each line is stored as a "message" in connector topic. For example:
  * a file having 100 lines has 100 message in connector topic. If the file is processed correctly, the TestFtpSource
  */
class FtpSourceTask extends RowSourceTask {

  private[this] var props: FtpSourceTaskProps = _
  private[this] var topics: Seq[String] = _
  private[this] var schema: Seq[Column] = _
  private[this] var ftpClient: FtpClient = _
  private[ftp] var cache: OffsetCache = _

  /**
    * list the files from input folder. NOTED: the returned value is full path.
    * @return files from input folder
    */
  private[ftp] def listInputFiles(): Seq[String] = try ftpClient
    .listFileNames(props.inputFolder)
    .map(CommonUtil.path(props.inputFolder, _))
    .filter(_.hashCode % props.total == props.hash)
  catch {
    case e: Throwable =>
      LOG.error(s"failed to list ${props.inputFolder}", e)
      Seq.empty
  }

  /**
    * move the file from input folder to error folder.
    * @param path file under input folder
    */
  private[ftp] def handleErrorFile(path: String): Unit = try {
    val outputPath = CommonUtil.replaceParent(props.errorFolder, path)
    if (ftpClient.exist(outputPath)) {
      val newPath = outputPath + s".${CommonUtil.uuid()}"
      if (ftpClient.exist(newPath)) throw new IllegalStateException(s"duplicate file $path??")
      else ftpClient.moveFile(path, newPath)
    } else ftpClient.moveFile(path, outputPath)
  } catch {
    case e: Throwable => LOG.error(s"failed to move $path to ${props.errorFolder}", e)
  }

  /**
    * move the file from input folder to completed folder.
    * @param path file under input folder
    */
  private[ftp] def handleCompletedFile(path: String): Unit = try {
    props.completedFolder
      .map(folder =>
        () => {
          val outputPath = CommonUtil.replaceParent(folder, path)
          if (ftpClient.exist(outputPath)) {
            val newPath = outputPath + s".${CommonUtil.uuid()}"
            if (ftpClient.exist(newPath)) throw new IllegalStateException(s"duplicate file $path??")
            else ftpClient.moveFile(path, newPath)
          } else ftpClient.moveFile(path, outputPath)
      })
      .getOrElse(() => ftpClient.delete(path))
      .apply()
  } catch {
    case e: Throwable =>
      if (props.completedFolder.isDefined) LOG.error(s"failed to move $path to ${props.completedFolder.get}", e)
      else LOG.error(s"failed to remove $path", e)
  }

  /**
    * read all lines from a ftp file, and then convert them to cells.
    * @param path ftp file
    * @return a map from (offset, cells)
    */
  private[ftp] def toRow(path: String): Map[Int, Seq[Cell[String]]] = {
    val lineAndIndex =
      ftpClient.readLines(path, props.encode.getOrElse("UTF-8")).filter(_.nonEmpty).zipWithIndex.filter {
        // first line is "header" so it is unnecessary to skip it
        case (_, index) => if (index == 0) true else cache.predicate(path, index)
      }
    if (lineAndIndex.length > 1) {
      val header: Map[Int, String] = lineAndIndex.head._1
        .split(CSV_REGEX)
        .zipWithIndex
        .map {
          case (item, index) => (index, item)
        }
        .toMap
      lineAndIndex
        .filter(_._2 >= 1)
        .map {
          case (line, rowIndex) =>
            (rowIndex,
             line
               .split(CSV_REGEX)
               .zipWithIndex
               .map {
                 case (item, index) => Cell.of(header(index), item)
               }
               .toSeq)
        }
        .toMap
    } else Map.empty
  }

  /**
    * transform the input cells to rows as stated by the schema. This method does the following works.
    * 1) filter out the unused cell
    * 2) replace the name by new one
    * 3) convert the string to specified type
    *
    * @param input input cells
    * @return map from (order, row)
    */
  import scala.collection.JavaConverters._
  private[ftp] def transform(input: Map[Int, Seq[Cell[String]]]): Map[Int, Row] = if (schema.isEmpty) input.map {
    case (index, cells) => (index, Row.of(cells: _*))
  } else
    input.map {
      case (index, cells) =>
        (index,
         Row.of(
           schema
             .sortBy(_.order)
             .map(column => {
               val value = cells.find(_.name == column.name).get.value
               Cell.of(
                 column.newName,
                 column.dataType match {
                   case DataType.BOOLEAN => value.toBoolean
                   case DataType.BYTE    => value.toByte
                   case DataType.SHORT   => value.toShort
                   case DataType.INT     => value.toInt
                   case DataType.LONG    => value.toLong
                   case DataType.FLOAT   => value.toFloat
                   case DataType.DOUBLE  => value.toDouble
                   case DataType.STRING  => value
                   case DataType.OBJECT  => value
                   // TODO: should we convert bytes?
                   case _ => throw new IllegalArgumentException("Unsupported type...")
                 }
               )
             }): _*))
    }

  override protected def _stop(): Unit = {
    Releasable.close(ftpClient)
  }

  override protected def _poll(): util.List[RowSourceRecord] = listInputFiles().headOption
    .map(path => {
      try {
        // update cache
        cache.update(rowContext, path)
        val rows = toRow(path)
        val records = transform(rows).flatMap {
          case (index, row) =>
            val p = partition(path).asJava
            val o = offset(index).asJava
            topics.map(t => RowSourceRecord.builder().sourcePartition(p).sourceOffset(o).row(row).topic(t).build())
        }.toSeq
        // ok. all data are prepared. let update the cache
        rows.foreach {
          case (index, _) => cache.update(path, index)
        }
        if (records.isEmpty) handleCompletedFile(path) // all lines in this file are processed
        records
      } catch {
        case e: Throwable =>
          LOG.error(s"failed to handle $path", e)
          handleErrorFile(path)
          Seq.empty
      }
    })
    .getOrElse(Seq.empty)
    .asJava

  override protected[ftp] def _start(config: TaskConfig): Unit = {
    this.props = FtpSourceTaskProps(config.options.asScala.toMap)
    this.schema = config.schema.asScala
    if (props.inputFolder.isEmpty)
      throw new IllegalArgumentException(s"invalid input:${props.inputFolder.mkString(",")}")
    topics = config.topics.asScala
    ftpClient =
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build()
    cache = OffsetCache()
  }
}

/**
  * used to manage the offset from files
  */
trait OffsetCache {

  /**
    * lode the latest offset from  RowSourceContext
    * @param context kafka's cache
    * @param path file path
    */
  def update(context: RowSourceContext, path: String): Unit

  /**
    * add (index, path) to the cache
    * @param path file path
    * @param index index from line
    */
  def update(path: String, index: Int): Unit

  /**
    * check whether the index from path is processed.
    * @param path file path
    * @param index index from line
    * @return true if the index from line isn't processed. otherwise, false
    */
  def predicate(path: String, index: Int): Boolean
}

object OffsetCache {
  def apply(): OffsetCache = new OffsetCache {
    private[this] val cache = new mutable.HashMap[String, Int]()

    override def update(context: RowSourceContext, path: String): Unit = {
      val map = context.offset(partition(path).asJava)
      if (!map.isEmpty) update(path, offset(map.asScala.toMap))
    }

    override def update(path: String, index: Int): Unit = {
      val previous = cache.getOrElseUpdate(path, index)
      if (index > previous) cache.update(path, index)
    }

    override def predicate(path: String, index: Int): Boolean = cache.get(path).forall(index > _)
  }
}
object FtpSourceTask {
  val CSV_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"
  private[this] val PATH_KEY = "ftp.file.path"
  private[this] val LINE_KEY = "ftp.file.offset"
  def partition(path: String): Map[String, _] = Map(PATH_KEY -> path)
  def offset(index: Int): Map[String, _] = Map(LINE_KEY -> index.toString)
  def offset(o: Map[String, _]): Int = o(LINE_KEY).asInstanceOf[String].toInt
}
