package com.island.ohara.integration

import java.io.IOException

import com.island.ohara.rule.MediumTest
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.junit.Test
import org.scalatest.Matchers

class TestHDFS extends MediumTest with Matchers {

  //fake url
  val hdfsURI: String = "hdfs://10.1.0.1"

  val envMap = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
  }

  /**
    * remote url is fake , cuz minicluster can't use in OHARA porject
    * All remote method will throw exception
    */
  @Test
  def testHDFSRemote(): Unit = {
    envMap.put("ohara.it.hdfs", hdfsURI)

    val hdfs = OharaTestUtil.localHDFS().hdfs

    hdfs.isLocal shouldBe false
    hdfs.tmpDirectory.startsWith("/it") shouldBe true

    val config = new Configuration()
    config.set("fs.defaultFS", hdfs.hdfsURL)
    an[IOException] should be thrownBy FileSystem.get(config).listFiles(new Path("/"), false)

    an[IOException] should be thrownBy hdfs.fileSystem.listFiles(new Path("/"), false)
  }

  @Test
  def testHDFSLocal(): Unit = {
    envMap.remove("ohara.it.hdfs", hdfsURI)

    val hdfs = OharaTestUtil.localHDFS().hdfs

    hdfs.isLocal shouldBe true
    hdfs.tmpDirectory.startsWith("/it") shouldBe false

    val config = new Configuration()
    config.set("fs.defaultFS", hdfs.hdfsURL)
    noException should be thrownBy FileSystem.get(config).listFiles(new Path("/"), false)

    noException should be thrownBy hdfs.fileSystem.listFiles(new Path("/"), false)
    hdfs.fileSystem.getHomeDirectory.toString().startsWith("file:") shouldBe true

  }
}
