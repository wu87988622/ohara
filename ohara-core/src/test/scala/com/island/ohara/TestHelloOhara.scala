package com.island.ohara

import com.typesafe.scalalogging.Logger
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

/**
  * TODO: remove this sample code after we start to write something formally.
  */
class TestHelloOhara extends AssertionsForJUnit {

  private[this] val logger = Logger(getClass.getName)

  @Test
  def testHelloOhara() = {
    logger.debug("Hello ohara")
  }
}
