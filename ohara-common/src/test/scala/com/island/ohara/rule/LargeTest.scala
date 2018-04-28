package com.island.ohara.rule

import org.junit.Rule
import org.junit.rules.Timeout
import org.scalatest.junit.JUnitSuiteLike

/**
  * Set the timeout to 5 minutes. Please don't use this LargeTest unless the tests you created is really a big stuff.
  */
trait LargeTest extends JUnitSuiteLike {
  @Rule
  def globalTimeout: Timeout = Timeout.seconds(5 * 60)
}

