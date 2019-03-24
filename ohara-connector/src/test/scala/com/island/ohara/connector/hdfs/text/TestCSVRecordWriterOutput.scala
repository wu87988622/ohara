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
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.connector.hdfs.{FLUSH_LINE_COUNT, HDFSSinkConnectorConfig, HDFS_URL}
import com.island.ohara.testing.WithTestUtils
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.Test
import org.scalatest.Matchers

class TestCSVRecordWriterOutput extends WithTestUtils with Matchers {
  val fileSystem: FileSystem = testUtil.hdfs.fileSystem
  val HDFS_URL_VALUE = "hdfs://test:9000"
  val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> HDFS_URL_VALUE, FLUSH_LINE_COUNT -> "2000"))

  @Test
  def testWriteData(): Unit = {
    val storage: Storage = new HDFSStorage(fileSystem)
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/file1.txt"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema = Seq(
      Column.builder().name("column1").dataType(DataType.STRING).order(0).build(),
      Column.builder().name("column2").dataType(DataType.STRING).order(1).build()
    )
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
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema = Seq(
      Column.builder().name("column1").dataType(DataType.STRING).order(0).build(),
      Column.builder().name("column2").dataType(DataType.STRING).order(1).build()
    )
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
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)

    val schema =
      Seq(
        Column.builder().name("column1").newName("COLUMN100").dataType(DataType.STRING).order(0).build(),
        Column.builder().name("column2").newName("COLUMN200").dataType(DataType.STRING).order(1).build()
      )
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
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(5)}"
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
    val tempFilePath: String = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(5)}"
    val csvRecordWriter: RecordWriterOutput =
      new CSVRecordWriterOutput(hdfsSinkConnectorConfig, storage, tempFilePath)
    val schema = Seq(
      Column.builder().name("column1").newName("COL1").dataType(DataType.STRING).order(0).build(),
      Column.builder().name("column2").newName("COL2").dataType(DataType.STRING).order(2).build(),
      Column.builder().name("column3").newName("COL3").dataType(DataType.STRING).order(1).build()
    )
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
      csvRecordWriter.write(needHeader, Seq(Column.builder().name("c").dataType(DataType.DOUBLE).order(0).build()), row)
    } finally csvRecordWriter.close()
    println("tempfilepath:" + tempFilePath)
    storage.exist(tempFilePath) shouldBe false
  }

}
