package com.island.ohara.io
import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.net.InetAddress
import java.nio.charset.Charset

import scala.collection.mutable.ArrayBuffer

object IoUtil {
  def hostname: String = InetAddress.getLocalHost.getHostName

  def readLines(f: File, filter: String => Boolean): Seq[String] =
    readLines(new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8"))), filter)

  def readLines(reader: BufferedReader, filter: String => Boolean): Seq[String] = try {
    val buf = new ArrayBuffer[String]()
    var line: String = reader.readLine()
    while (line != null) {
      if (!line.isEmpty && filter(line)) buf += line
      line = reader.readLine()
    }
    buf
  } finally reader.close()
}
