package com.island.ohara.rule

import com.typesafe.scalalogging.Logger
import org.junit.Rule
import org.junit.rules.{TestName, Timeout}
import org.scalatest.junit.JUnitSuiteLike

/**
  * Set the timeout to 2 minutes. If your tests use the OharaTestUtil, your should consider to extend this trait.
  */
trait MediumTest extends JUnitSuiteLike {
  protected lazy val logger = Logger(getClass.getName)
  @Rule
  def globalTimeout: Timeout = Timeout.seconds(2 * 60)

  /**
    * We have to make @rule methods be Public to be accessed by java code but the def will new a object for each call.
    * Hence, pre-creating the object and return the object by def method in order to make sure junit will update the same object
    * passed to later tests.
    */
  val _testName = new TestName

  @Rule
  def testName: TestName = _testName
}
