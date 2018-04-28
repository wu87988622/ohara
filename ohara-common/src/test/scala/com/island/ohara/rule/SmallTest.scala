package com.island.ohara.rule

import org.junit.Rule
import org.junit.rules.Timeout
import org.scalatest.junit.JUnitSuiteLike

/**
  * Set the timeout to 30 seconds. Having you tests extend SmallTest is reasonable if your tests are about functional evaluation.
  */
trait SmallTest extends JUnitSuiteLike {
  @Rule
  def globalTimeout: Timeout = Timeout.seconds(30)
}
