package com.island.ohara.util

import java.io.{File, FileInputStream}
import java.util.Properties

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestVersionUtil extends SmallTest with Matchers {
  @Test
  def testVersion(): Unit = {
    val modulePath = new File(".").getAbsolutePath
    val filePath = modulePath.substring(0, modulePath.indexOf("/ohara-common")) + "/gradle.properties"
    val props = new Properties
    val input = new FileInputStream(new File(filePath))
    try props.load(input)
    finally input.close()
    props.getProperty("version") shouldBe VersionUtil.VERSION
  }
}
