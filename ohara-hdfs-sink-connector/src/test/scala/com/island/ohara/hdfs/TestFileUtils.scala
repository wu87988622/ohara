package com.island.ohara.hdfs

import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestFileUtils extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testGetFileName(): Unit = {
    val result1 = FileUtils.offsetFileName("prefix", 1, 1000)
    result1 shouldBe "prefix-000000001-000001000" + FileUtils.FILENAME_ENDSWITH

    val result2 = FileUtils.offsetFileName("prefix", 222222, 999999999)
    result2 shouldBe "prefix-000222222-999999999" + FileUtils.FILENAME_ENDSWITH
  }

  @Test
  def testGetStopOffset(): Unit = {
    val fileNameList = List(
      "prefix-000000001-000000011" + FileUtils.FILENAME_ENDSWITH,
      "prefix-000001000-000001001" + FileUtils.FILENAME_ENDSWITH,
      "prefix-000000009-000000999" + FileUtils.FILENAME_ENDSWITH
    )

    FileUtils.getStopOffset(fileNameList) shouldBe 1001
  }

  @Test
  def testGetStopZero(): Unit = {
    val fileNameList = List()
    FileUtils.getStopOffset(fileNameList) shouldBe 0
  }

  @Test
  def testCheckFileNameFormat(): Unit = {
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111.csv") shouldBe true
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111") shouldBe false
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111csv") shouldBe false
    FileUtils.checkFileNameFormat("preA-fi123x-000000000-111111111csv") shouldBe false
    FileUtils.checkFileNameFormat("preA_fi123x-000000000-111111111.csv") shouldBe false
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111.txt") shouldBe true
  }
}
