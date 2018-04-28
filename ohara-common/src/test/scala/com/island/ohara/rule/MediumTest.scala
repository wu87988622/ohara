package com.island.ohara.rule

import org.junit.Rule
import org.junit.rules.Timeout
import org.scalatest.junit.JUnitSuiteLike

/**
  * Set the timeout to 2 minutes. If your tests use the OharaTestUtil, your should consider to extend this trait.
  */
trait MediumTest extends JUnitSuiteLike {
  @Rule
  def globalTimeout: Timeout = Timeout.seconds(2 * 60)
}
