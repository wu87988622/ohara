package com.island.ohara.hdfs

import com.island.ohara.hdfs.storage.Storage
import com.island.ohara.hdfs.text.{CSVRecordWriterOutput, RecordWriterOutput}
import com.island.ohara.kafka.connector.RowSinkRecord
import com.typesafe.scalalogging.Logger
import org.apache.hadoop.fs.Path
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.SinkTaskContext

/**
  * This class for process data
  * @param config
  * @param context
  * @param partition
  * @param storage
  */
class TopicPartitionWriter(config: HDFSSinkConnectorConfig,
                           context: SinkTaskContext, //TODO OHARA-98 for move offset position
                           partition: TopicPartition,
                           storage: Storage) {
  private[this] lazy val logger = Logger(getClass().getName())
  var recordWriterOutput: RecordWriterOutput = _
  var tmpFilePath: String = _
  val partitionName: String = s"partition${partition.partition()}"

  val tmpDir: String = s"${config.tmpDir()}/${partition.topic()}/${partitionName}"
  val dataDir: String = s"${config.dataDir()}/${partition.topic()}/${partitionName}"
  val offsetDir: String = s"${config.offsetDir()}/${partition.topic()}/${partitionName}"

  val filePrefixName: String = config.dataFilePrefixName()
  val flushLineCount: Int = config.flushLineCount()
  val rotateInterval: Long = config.rotateIntervalMS()
  var startTimeMS: Long = 0
  var processLineCount: Int = 0

  def open(): Unit = {
    //If tmp dir not exists to create tmp dir
    createTmpDirIfNotExists(s"$tmpDir")

    //If offset dir not exists to create offset dir
    createOffsetDirIfNotExists(s"$dataDir")

    //If data dir not exists to create data dir
    createDataDirIfNotExists(s"$offsetDir")

    //Execute offset recover
    recoveryOffset()
  }

  def write(rowSinkRecord: RowSinkRecord): Unit = {
    //Open Temp File
    openTempFile(recordWriterOutput, processLineCount)

    //Write Data to Temp File
    writeData(rowSinkRecord)
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
    * @param recordWriterProvider
    * @param processLineCount
    * @return
    */
  def openTempFile(recordWriterProvider: RecordWriterOutput, processLineCount: Int): Unit = {
    if (processLineCount == 0) {
      val tmpFilePath = s"${tmpDir}/${System.currentTimeMillis()}${FileUtils.FILENAME_ENDSWITH}"
      logger.info(s"create temp file path: ${tmpFilePath}")
      recordWriterOutput = new CSVRecordWriterOutput(config, storage, tmpFilePath)
      this.tmpFilePath = tmpFilePath
    }
  }

  def writeData(rowSinkRecord: RowSinkRecord): Unit = {
    recordWriterOutput.write(rowSinkRecord.value)
  }

  def commitFile(recordWriterProvider: RecordWriterOutput, tmpFilePath: String): Unit = {
    logger.info(s"running commit file tmpFileName: ${tmpFilePath}")
    recordWriterProvider.close()
    commit(tmpFilePath,
           flushFilePath(storage.list(s"${dataDir}").map(filePath => new Path(filePath).getName()).toList, dataDir))
  }

  def close(): Unit = {
    if (storage.exists(tmpDir))
      storage.delete(tmpDir, true)
  }

  def flushFilePath(fileList: List[String], dataDir: String): String = {
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
    val fileName = s"${dataDir}/${FileUtils.offsetFileName(filePrefixName, startOffset, stopOffset)}"
    logger.info(s"flush file path is: ${fileName}")
    fileName
  }

  protected[hdfs] def isDataCountCommit(processLineCount: Int, flushLineCount: Int): Boolean = {
    processLineCount >= flushLineCount
  }

  protected[hdfs] def isTimeCommit(startTimeMS: Long, rotateInterval: Long): Boolean = {
    (System.currentTimeMillis() - startTimeMS) >= rotateInterval
  }

  protected def recoveryOffset(): Unit = {
    //TODO Execute offset recver OHARA-98
  }

  protected def commit(sourcePath: String, destPath: String): Unit = {
    storage.renameFile(sourcePath, destPath)
  }

  protected def createTmpDirIfNotExists(tmpDirPath: String): Unit = {
    if (!storage.exists(tmpDirPath)) {
      storage.mkdirs(tmpDirPath)
    }
  }

  protected def createOffsetDirIfNotExists(dataDirPath: String): Unit = {
    if (!storage.exists(dataDirPath)) {
      storage.mkdirs(dataDirPath)
    }
  }

  protected def createDataDirIfNotExists(offsetDirPath: String): Unit = {
    if (!storage.exists(offsetDirPath)) {
      storage.mkdirs(offsetDirPath)
    }
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
