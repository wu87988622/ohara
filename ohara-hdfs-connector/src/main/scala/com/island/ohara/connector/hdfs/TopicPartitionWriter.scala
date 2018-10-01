package com.island.ohara.connector.hdfs

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.connector.hdfs.storage.Storage
import com.island.ohara.connector.hdfs.text.{CSVRecordWriterOutput, RecordWriterOutput}
import com.island.ohara.kafka.connector.{RowSinkContext, RowSinkRecord, TopicPartition}
import com.typesafe.scalalogging.Logger

/**
  * This class for process data
  * @param config
  * @param context
  * @param partition
  * @param storage
  */
class TopicPartitionWriter(config: HDFSSinkConnectorConfig,
                           context: RowSinkContext,
                           partition: TopicPartition,
                           storage: Storage) {
  private[this] lazy val logger = Logger(getClass.getName)
  var recordWriterOutput: RecordWriterOutput = _
  var tmpFilePath: String = _
  val partitionName: String = s"partition${partition.partition}"

  val tmpDir: String = s"${config.tmpDir()}/${partition.topic}/$partitionName"
  val dataDir: String = s"${config.dataDir()}/${partition.topic}/$partitionName"

  val filePrefixName: String = config.dataFilePrefixName()
  val flushLineCount: Int = config.flushLineCount()
  val rotateInterval: Long = config.rotateIntervalMS()
  val dataFileNeedHeader: Boolean = config.dataFileNeedHeader()
  var startTimeMS: Long = 0
  var processLineCount: Int = 0

  def open(): Unit = {
    //If tmp dir not exists to create tmp dir
    createTmpDirIfNotExists(s"$tmpDir")

    //If data dir not exists to create data dir
    createDataDirIfNotExists(s"$dataDir")

    //Execute offset recover
    recoveryOffset()
  }

  def write(schema: Seq[Column], rowSinkRecord: RowSinkRecord): Unit = {
    //Open Temp File
    openTempFile(processLineCount)

    //Write Data to Temp File
    var needHeader: Boolean = processLineCount == 0 && dataFileNeedHeader

    writeData(needHeader, schema, rowSinkRecord)
    processLineCount = processLineCount + 1

    //Temp file commit to data dir
    if (isDataCountCommit(processLineCount, flushLineCount)) {
      runningCommitFile()
    }
  }

  def writer(): Unit = {
    //Use time commit from tmp file to data dir
    if (startTimeMS == TopicPartitionWriter.START_TIME_MILLIS_ZERO) {
      startTimeMS = System.currentTimeMillis()
    }

    //Time is up and have the tmp file then running commit file
    if (isTimeCommit(startTimeMS, rotateInterval) && storage.exists(tmpFilePath)) {
      runningCommitFile()
    }
  }

  /**
    * Create temp file
    * @param processLineCount
    * @return
    */
  def openTempFile(processLineCount: Int): Unit = {
    if (processLineCount == 0) {
      val tmpFilePath = s"$tmpDir/${System.currentTimeMillis()}${FileUtils.FILENAME_ENDSWITH}"
      logger.info(s"create temp file path: $tmpFilePath")
      recordWriterOutput = new CSVRecordWriterOutput(storage, tmpFilePath)
      this.tmpFilePath = tmpFilePath
    }
  }

  def writeData(needHeader: Boolean, schema: Seq[Column], rowSinkRecord: RowSinkRecord): Unit = {
    recordWriterOutput.write(needHeader, schema, rowSinkRecord.row)
  }

  def commitFile(recordWriterProvider: RecordWriterOutput, tmpFilePath: String): Unit = {
    logger.info(s"running commit file tmpFileName: $tmpFilePath")
    recordWriterProvider.close()
    commit(tmpFilePath, flushFilePath(storage.list(s"$dataDir").map(filePath => FileUtils.fileName(filePath)), dataDir))
  }

  def close(): Unit = {
    if (storage.exists(tmpDir))
      storage.delete(tmpDir, true)
  }

  def flushFilePath(fileList: Iterator[String], dataDir: String): String = {
    var startOffset: Long = FileUtils.getStopOffset(fileList)

    //if flush size = 10
    //first  commit startOffset=0  and endOffset=9
    //second commit startOffset=10 and endOffset=19 (startOffset=first commit endOffset + 1, so startOffset=9+1)
    //third  commit startOffset=20 and endOffset=29
    if (startOffset > 1) {
      startOffset = startOffset + 1
    }

    var stopOffset: Long = startOffset + processLineCount - 1
    if (stopOffset < 0) {
      stopOffset = 0
    }
    val fileName = s"$dataDir/${FileUtils.offsetFileName(filePrefixName, startOffset, stopOffset)}"
    logger.info(s"flush file path is: $fileName")
    fileName
  }

  protected[hdfs] def isDataCountCommit(processLineCount: Int, flushLineCount: Int): Boolean = {
    processLineCount >= flushLineCount
  }

  protected[hdfs] def isTimeCommit(startTimeMS: Long, rotateInterval: Long): Boolean = {
    (System.currentTimeMillis() - startTimeMS) >= rotateInterval
  }

  protected def commit(sourcePath: String, destPath: String): Unit = {
    storage.renameFile(sourcePath, destPath)
  }

  protected def createTmpDirIfNotExists(tmpDirPath: String): Unit = {
    if (!storage.exists(tmpDirPath)) {
      storage.mkdirs(tmpDirPath)
    }
  }

  protected def createDataDirIfNotExists(offsetDirPath: String): Unit = {
    if (!storage.exists(offsetDirPath)) {
      storage.mkdirs(offsetDirPath)
    }
  }

  /**
    * Kafka Topic Partition recovery to offset position
    */
  private def recoveryOffset(): Unit = {
    logger.info("recovery offset")
    val previousStopOffset: Long =
      FileUtils.getStopOffset(storage.list(s"$dataDir").map(filePath => FileUtils.fileName(filePath)))

    //Move kafka topic partition offset to not commit to data dir position for HDFSSink connector fault or connect worker fault
    context.offset(partition, previousStopOffset)
  }

  private def runningCommitFile(): Unit = {
    commitFile(recordWriterOutput, tmpFilePath)
    processLineCount = 0
    startTimeMS = 0
  }
}

object TopicPartitionWriter {
  val START_TIME_MILLIS_ZERO: Long = 0
}
