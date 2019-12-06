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

package com.island.ohara.it.performance

import java.io.{BufferedWriter, OutputStreamWriter}
import java.util.concurrent.atomic.{AtomicBoolean, LongAdder}
import java.util.concurrent.{ArrayBlockingQueue, Executors, TimeUnit}

import com.island.ohara.client.filesystem.ftp.FtpClient
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.service.FtpServer
import org.junit.{After, AssumptionViolatedException}
import spray.json.{JsNumber, JsString, JsValue}

import scala.collection.JavaConverters._

abstract class BasicTestPerformance4Ftp extends BasicTestPerformance {
  private[this] val ftpHostname = value("ohara.it.performance.ftp.hostname")
    .getOrElse(throw new AssumptionViolatedException("ohara.it.performance.ftp.hostname is required"))

  /**
    * add the route for ftp hostname to avoid the hostname error from remote services...
    * @return routes routes added to all services
    */
  override def routes: Map[String, String] = Map(ftpHostname -> CommonUtils.address(ftpHostname))

  /**
    * TODO: we should use individual ftp server rather than embedded ftp
    * (fix by https://github.com/oharastream/ohara/issues/3030)
    */
  private[this] val ftpServer = {
    val key         = "ohara.it.performance.public.ports"
    val publicPorts = value(key).map(_.split(",").map(_.toInt).toSet).getOrElse(Set.empty)
    if (publicPorts.size < 2)
      throw new AssumptionViolatedException(s"$key is required, and the size of ports must be bigger than 2")

    FtpServer
      .builder()
      .advertisedHostname(ftpHostname)
      .controlPort(publicPorts.head)
      .dataPorts(publicPorts.slice(1, publicPorts.size).map(new Integer(_)).toSeq.asJava)
      .build()
  }

  /**
    * generate the default settings according to the ftp server. It includes
    * 1) hostname
    * 2) port
    * 3) user name
    * 4) user password
    */
  protected val ftpSettings: Map[String, JsValue] = Map(
    // convert the hostname to IP address
    com.island.ohara.connector.ftp.FTP_HOSTNAME_KEY  -> JsString(ftpHostname),
    com.island.ohara.connector.ftp.FTP_PORT_KEY      -> JsNumber(ftpServer.port()),
    com.island.ohara.connector.ftp.FTP_USER_NAME_KEY -> JsString(ftpServer.user()),
    com.island.ohara.connector.ftp.FTP_PASSWORD_KEY  -> JsString(ftpServer.password())
  )

  private[this] val csvInputFolderKey       = "ohara.it.performance.csv.input"
  private[this] val csvOutputFolder: String = value(csvInputFolderKey).getOrElse("/input")

  private[this] val cleanupTestDataKey   = "ohara.it.performance.cleanup"
  protected val cleanupTestData: Boolean = value(cleanupTestDataKey).forall(_.toBoolean)

  private[this] def ftpClient() =
    FtpClient
      .builder()
      .hostname(ftpServer.hostname())
      .port(ftpServer.port())
      .user(ftpServer.user())
      .password(ftpServer.password())
      .build

  protected def setupInputData(): (String, Long, Long) = {
    val cellNames: Set[String] = (0 until 10).map(index => s"c$index").toSet

    /**
      * if the number of threads is bigger than the number of data ports, it produces the error since no available data
      * port for extra threads :(
      */
    val numberOfProducerThread = ftpServer.dataPorts().size()
    val numberOfRowsToFlush    = 1000
    val pool                   = Executors.newFixedThreadPool(numberOfProducerThread)
    val closed                 = new AtomicBoolean(false)
    val count                  = new LongAdder()
    val sizeInBytes            = new LongAdder()

    val client = ftpClient()
    try if (client.exist(csvOutputFolder)) throw new IllegalArgumentException(s"$csvOutputFolder exists!!!")
    else client.mkdir(csvOutputFolder)
    finally Releasable.close(client)

    try {
      (0 until numberOfProducerThread).foreach { _ =>
        pool.execute(() => {
          val client = ftpClient()
          try while (!closed.get() && sizeInBytes.longValue() <= sizeOfInputData) {
            val file   = s"$csvOutputFolder/${CommonUtils.randomString()}"
            val writer = new BufferedWriter(new OutputStreamWriter(client.create(file)))
            try {
              writer
                .append(cellNames.mkString(","))
                .append("\n")
              (0 until numberOfRowsToFlush).foreach { _ =>
                val content = cellNames.map(_ => CommonUtils.randomString()).mkString(",")
                count.increment()
                sizeInBytes.add(content.length)
                writer
                  .append(content)
                  .append("\n")
              }
            } finally Releasable.close(writer)
          } finally Releasable.close(client)
        })
      }
    } finally {
      pool.shutdown()
      pool.awaitTermination(durationOfPerformance.toMillis * 10, TimeUnit.MILLISECONDS)
      closed.set(true)
    }
    (csvOutputFolder, count.longValue(), sizeInBytes.longValue())
  }

  protected def createFtpFolder(path: String): String = {
    val client = ftpClient()
    try client.mkdir(path)
    finally Releasable.close(client)
    path
  }

  /**
    * remove all data in the path.
    * @param path file path on the remote ftp server
    */
  protected def removeFtpFolder(path: String): Unit = {
    val count     = ftpServer.dataPorts().size()
    val executors = Executors.newFixedThreadPool(4)
    val client    = ftpClient()
    try {
      val files = {
        val fs    = client.listFileNames(path).map(name => s"$path/$name")
        val queue = new ArrayBlockingQueue[String](fs.size)
        queue.addAll(fs.asJava)
        queue
      }
      (0 until count).foreach { _ =>
        executors.execute(() => {
          val client = ftpClient()
          try {
            var file = files.poll()
            while (file != null) {
              client.delete(file)
              file = files.poll()
            }
          } finally Releasable.close(client)
        })
      }
    } finally try {
      executors.shutdown()
      // we delete the folder only if all threads are completed
      if (executors.awaitTermination(60, TimeUnit.SECONDS)) client.delete(path)
      else throw new IllegalArgumentException(s"failed to remove folder:$path due to timeout")
    } finally Releasable.close(client)
  }

  @After
  def closeFtpServer(): Unit = Releasable.close(ftpServer)
}
