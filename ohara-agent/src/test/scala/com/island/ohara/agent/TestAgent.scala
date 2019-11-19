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

package com.island.ohara.agent

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.testing.service.SshdServer
import com.island.ohara.testing.service.SshdServer.CommandHandler
import org.junit.{After, Test}
import org.scalatest.Matchers._

class TestAgent extends OharaTest {
  private[this] val customCommands = Map(
    "hello"    -> Seq("world"),
    "chia7712" -> Seq("jellynina")
  )

  import scala.collection.JavaConverters._
  private[this] val handlers = customCommands.map {
    case (k, response) =>
      new CommandHandler {
        override def belong(command: String): Boolean = command == k
        override def execute(command: String): java.util.List[String] =
          if (belong(command)) response.asJava else throw new IllegalArgumentException(s"$k doesn't support")
      }
  }.toSeq
  private[this] val server = SshdServer.local(0, handlers.map(h => h.asInstanceOf[CommandHandler]).asJava)

  @Test
  def testJaveVersion(): Unit = {
    val agent =
      Agent.builder.hostname(server.hostname).port(server.port).user(server.user).password(server.password).build
    try {
      val result = agent.execute("java -version").get
      // You must have jdk 1.8+ if you want to run ohara...
      withClue(s"result:$result")(result.contains("1.8.0") shouldBe true)
    } finally agent.close()
  }

  @Test
  def testCustomCommand(): Unit = {
    customCommands.foreach {
      case (command, response) =>
        val agent =
          Agent.builder.hostname(server.hostname).port(server.port).user(server.user).password(server.password).build
        try agent.execute(command).get.split("\n") shouldBe response
        finally agent.close()
    }
  }

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy Agent.builder.hostname(null)

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy Agent.builder.hostname("")

  @Test
  def negativePort(): Unit = {
    an[IllegalArgumentException] should be thrownBy Agent.builder.port(0)
    an[IllegalArgumentException] should be thrownBy Agent.builder.port(-1)
  }

  @Test
  def nullUser(): Unit = an[NullPointerException] should be thrownBy Agent.builder.user(null)

  @Test
  def emptyUser(): Unit = an[IllegalArgumentException] should be thrownBy Agent.builder.user("")

  @Test
  def nullPassword(): Unit = an[NullPointerException] should be thrownBy Agent.builder.password(null)

  @Test
  def emptyPassword(): Unit = an[IllegalArgumentException] should be thrownBy Agent.builder.password("")

  @Test
  def nullCharset(): Unit = an[NullPointerException] should be thrownBy Agent.builder.charset(null)

  @After
  def tearDown(): Unit = Releasable.close(server)
}
