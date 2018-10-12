package com.island.ohara.io

object VersionUtil {

  /**
    * there are many magic tricks which can make "dynamical" version. However, it doesn't make sense to us currently.
    * We have put a test case (see TestVersionUtil) to ensure both value in gradle.build and VersionUtil are same.
    */
  val VERSION: String = "0.1-SNAPSHOT"

  def main(args: Array[String]): Unit = {
    println("ohara version \"" + VERSION + "\"")
  }
}
