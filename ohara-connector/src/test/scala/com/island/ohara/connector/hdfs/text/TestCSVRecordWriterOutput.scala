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

package com.island.ohara.connector.hdfs.text

import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.connector.hdfs.{FLUSH_LINE_COUNT, HDFSSinkConnectorConfig, HDFS_URL}
import com.island.ohara.testing.WithTestUtil
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.Test
import org.scalatest.Matchers

class TestCSVRecordWriterOutput extends WithTestUtil with Matchers {
  val fileSystem: FileSystem = testUtil.hdfs.fileSystem
  val HDFS_URL_VALUE = "hdfs://test:9000"
  val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, FLUSH_LINE_COUNT -> "2000"))

  @Test
  def testWriteData(): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/file1.txt"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema = Seq(Column.of("column1", DataType.STRING, 0), Column.of("column2", DataType.STRING, 1))
    val row = Row.of(Cell.of("column1", "value1"), Cell.of("column2", "value2"))
    csvRecordWriter.write(false, schema, row)
    csvRecordWriter.close()
    storage.exist(tempFilePath) shouldBe true

    val inputStream = fileSystem.open(new Path(tempFilePath))
    try {
      val result: StringBuilder = new StringBuilder()
      Stream
        .continually(inputStream.read())
        .takeWhile(_ != -1)
        .foreach(x => {
          result.append(x.toChar)
        })
      result.toString shouldBe "value1,value2\n"
    } finally inputStream.close()
  }

  @Test
  def testNeedHeader(): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtil.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema = Seq(Column.of("column1", DataType.STRING, 0), Column.of("column2", DataType.STRING, 1))
    val row = Row.of(Cell.of("column1", "value1"), Cell.of("column2", "value2"))
    csvRecordWriter.write(true, schema, row)
    csvRecordWriter.close()
    storage.exist(tempFilePath) shouldBe true

    val inputStream = fileSystem.open(new Path(tempFilePath))
    try {
      val result: StringBuilder = new StringBuilder()
      Stream
        .continually(inputStream.read())
        .takeWhile(_ != -1)
        .foreach(x => {
          result.append(x.toChar)
        })
      result.toString shouldBe "column1,column2\nvalue1,value2\n"
    } finally inputStream.close()
  }

  @Test
  def testNeedHeaderNewColumn(): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtil.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema =
      Seq(Column.of("column1", "COLUMN100", DataType.STRING, 0), Column.of("column2", "COLUMN200", DataType.STRING, 1))
    val row = Row.of(Cell.of("column1", "value1"), Cell.of("column2", "value2"))
    csvRecordWriter.write(true, schema, row)
    csvRecordWriter.close()
    storage.exist(tempFilePath) shouldBe true

    val inputStream = fileSystem.open(new Path(tempFilePath))
    try {
      val result: StringBuilder = new StringBuilder()
      Stream
        .continually(inputStream.read())
        .takeWhile(_ != -1)
        .foreach(x => {
          result.append(x.toChar)
        })
      result.toString shouldBe "COLUMN100,COLUMN200\nvalue1,value2\n"
    } finally inputStream.close()
  }

  @Test
  def testNeedHeaderSchemaEmpty(): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtil.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema = Seq()
    val row = Row.of(Cell.of("column1", "value1"), Cell.of("column2", "value2"))
    csvRecordWriter.write(true, schema, row)
    csvRecordWriter.close()
    storage.exist(tempFilePath) shouldBe true

    val inputStream = fileSystem.open(new Path(tempFilePath))
    try {
      val result: StringBuilder = new StringBuilder()
      Stream
        .continually(inputStream.read())
        .takeWhile(_ != -1)
        .foreach(x => {
          result.append(x.toChar)
        })
      result.toString shouldBe "column1,column2\nvalue1,value2\n"
    } finally inputStream.close()
  }

  @Test
  def testOrder(): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtil.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema = Seq(Column.of("column1", "COL1", DataType.STRING, 0),
                     Column.of("column2", "COL2", DataType.STRING, 2),
                     Column.of("column3", "COL3", DataType.STRING, 1))
    val row = Row.of(Cell.of("column1", "value1"), Cell.of("column2", "value2"), Cell.of("column3", "value3"))
    csvRecordWriter.write(true, schema, row)
    csvRecordWriter.close()
    storage.exist(tempFilePath) shouldBe true

    val inputStream = fileSystem.open(new Path(tempFilePath))
    try {
      val result: StringBuilder = new StringBuilder()
      Stream
        .continually(inputStream.read())
        .takeWhile(_ != -1)
        .foreach(x => {
          result.append(x.toChar)
        })
      result.toString shouldBe "COL1,COL3,COL2\nvalue1,value3,value2\n"
    } finally inputStream.close()
  }

  @Test
  def testEmptyLineWithHeader(): Unit = {
    testEmptyLine(true)
  }

  @Test
  def testEmptyLineWithoutHeader(): Unit = {
    testEmptyLine(false)
  }

  private[this] def testEmptyLine(needHeader: Boolean): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/$methodName"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    try {
      val row = Row.of(Cell.of("cf0", 123), Cell.of("cf1", false))
      csvRecordWriter.write(needHeader, Seq(Column.of("c", "c", DataType.DOUBLE, 0)), row)
    } finally csvRecordWriter.close()
    println("tempfilepath:" + tempFilePath)
    storage.exist(tempFilePath) shouldBe false
  }

}
