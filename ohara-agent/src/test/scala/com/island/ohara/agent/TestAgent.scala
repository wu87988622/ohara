package com.island.ohara.agent

import com.island.ohara.agent.SshdServer.CommandHandler
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestAgent extends SmallTest with Matchers {

  private[this] val customCommands = Map(
    "hello" -> Seq("world"),
    "chia7712" -> Seq("jellynina")
  )

  private[this] val handlers = customCommands.map {
    case (k, response) =>
      new CommandHandler {
        override def belong(command: String): Boolean = command == k
        override def execute(command: String): Seq[String] =
          if (belong(command)) response else throw new IllegalArgumentException(s"$k doesn't support")
      }
  }.toSeq

  private[this] val server = SshdServer.local(0, handlers)

  @Test
  def testJaveVersion(): Unit = {
    val agent =
      Agent.builder().hostname(server.hostname).port(server.port).user(server.user).password(server.password).build()
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
        val agent = Agent
          .builder()
          .hostname(server.hostname)
          .port(server.port)
          .user(server.user)
          .password(server.password)
          .build()
        try agent.execute(command).get.split("\n") shouldBe response
        finally agent.close()
    }
  }
  @After
  def tearDown(): Unit = ReleaseOnce.close(server)
}
