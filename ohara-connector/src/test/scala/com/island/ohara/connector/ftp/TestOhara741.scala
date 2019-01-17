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
import com.island.ohara.client.FtpClient
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.integration.FtpServer
import com.island.ohara.kafka.connector.TaskConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestOhara741 extends SmallTest with Matchers {

  private[this] val ftpServer = FtpServer.local(0, Array(0))

  @Test
  def testAutoCreateOutput(): Unit = {
    val props = FtpSinkProps(
      output = "/output",
      needHeader = false,
      user = ftpServer.user,
      password = ftpServer.password,
      hostname = ftpServer.hostname,
      port = ftpServer.port,
      encode = Some("UTF-8")
    )

    val taskConfig = TaskConfig.builder().name("aa").options(props.toMap.asJava).build()

    val sink = new FtpSink
    sink._start(taskConfig)

    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build()
    try ftpClient.exist("/output") shouldBe true
    finally ftpClient.close()
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(ftpServer)
  }
}
