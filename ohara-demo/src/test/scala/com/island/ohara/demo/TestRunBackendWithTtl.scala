package com.island.ohara.demo

import com.island.ohara.rule.LargeTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestRunBackendWithTtl extends LargeTest with Matchers {

  @Test
  def testTtl(): Unit = {
    val f = Future {
      Backend.main(Array(Backend.TTL_KEY, "1"))
    }
    // we have to wait all service to be closed so 180 seconds is a safe limit.
    Await.result(f, 180 seconds)
  }
}
