package com.island.ohara.connector.hdfs

import org.apache.commons.io.FilenameUtils

/**
  * This class for the process the data file name related
  */
object FileUtils {
  val FILENAME_ENDSWITH = ".csv"
  val FILENAME_SEPARATOR: String = "-"
  val COMMITTED_FILENAME_PATTERN: String = s"[a-zA-Z0-9]*$FILENAME_SEPARATOR[0-9]{9}$FILENAME_SEPARATOR[0-9]{9}\\..*"
  val STOP_OFFSET_INDEX
    : Int = 2 //Index from stop CAN'T be changed in the future since the change to index cause us fail to handle the older offset files.

  /**
    * Combine the file name
    * format is:
    * ${prefix name}-${start offset}-${stop offset}.csv
    * @param prefixName
    * @param startOffset
    * @param stopOffset
    * @return
    */
  def offsetFileName(prefixName: String, startOffset: Long, stopOffset: Long): String = {
    val pattern: String = "%09d"
    val appendFileName: StringBuilder = new StringBuilder()
    appendFileName.append(prefixName)
    appendFileName.append(FILENAME_SEPARATOR)
    appendFileName.append(pattern.format(startOffset))
    appendFileName.append(FILENAME_SEPARATOR)
    appendFileName.append(pattern.format(stopOffset))
    appendFileName.append(FILENAME_ENDSWITH)

    val fileName = appendFileName.toString
    if (checkFileNameFormat(fileName))
      fileName
    else
      throw new IllegalArgumentException(s"$fileName does not match $COMMITTED_FILENAME_PATTERN pattern")
  }

  /**
    * Get stop offset
    * @param fileNames
    * @return
    */
  def getStopOffset(fileNames: Iterator[String]): Long = {
    if (fileNames.nonEmpty)
      fileNames
        .map(fileName => {
          if (checkFileNameFormat(fileName)) {
            fileName.split(FILENAME_SEPARATOR)(STOP_OFFSET_INDEX).replace(FILENAME_ENDSWITH, "").toLong
          } else {
            throw new IllegalArgumentException(s"$fileName does not match $COMMITTED_FILENAME_PATTERN pattern")
          }
        })
        .max
    else
      0
  }

  /**
    * Validation file name format
    * @param fileName
    * @return
    */
  def checkFileNameFormat(fileName: String): Boolean = {
    fileName.matches(COMMITTED_FILENAME_PATTERN)
  }

  /**
    * Get path simple file name
    * @param path
    * @return
    */
  def fileName(path: String): String = {
    FilenameUtils.getName(path)
  }
}
