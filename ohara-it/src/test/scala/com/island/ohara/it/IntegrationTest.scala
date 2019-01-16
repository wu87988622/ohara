package com.island.ohara.it

import com.island.ohara.common.rule.OharaTest
import org.junit.Rule
import org.junit.rules.Timeout

class IntegrationTest extends OharaTest {
  @Rule def globalTimeout: Timeout = Timeout.seconds(60 * 15)
}
