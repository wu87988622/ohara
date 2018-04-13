package com.island.ohara

import com.typesafe.scalalogging.Logger
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * TODO: remove this sample code after we start to write something formally.
  */
@RunWith(classOf[JUnitRunner])
class TestHelloOharaV2 extends FlatSpec with Matchers {

  private[this] val logger = Logger(getClass.getName)

  "The \"hello ohara v2\"" should "be printed in console" in {
    logger.debug("Hello ohara v2")
  }
}
