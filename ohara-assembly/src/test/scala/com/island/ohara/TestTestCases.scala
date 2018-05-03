package com.island.ohara

import java.net.URL
import java.util.regex.Pattern

import com.island.ohara.rule.{LargeTest, MediumTest, SmallTest}
import com.typesafe.scalalogging.Logger
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer

class TestTestCases extends MediumTest with Matchers {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] val validTestCatalog: Array[Class[_]] =
    Array(classOf[SmallTest], classOf[MediumTest], classOf[LargeTest])
  private[this] val validTestName: Array[String] = validTestCatalog.map(_.getName)

  /**
    * fail if any test case have not extended the test catalog.
    */
  @Test
  def testSupperClassOfTestCases(): Unit = {
    val classLoader = ClassLoader.getSystemClassLoader
    val packageName = getClass.getPackage.getName
    val path = packageName.replace('.', '/') + "/"
    val pattern = Pattern.compile("^file:(.+\\.jar)!/" + path + "$")
    val urls = classLoader.getResources(path)
    new Iterator[URL] {
      def hasNext = urls.hasMoreElements()

      def next() = urls.nextElement()
    }.map(url => pattern.matcher(url.getFile))
      .filter(_.find())
      .map(_.group(1))
      // hard code but it is ok since it is convention to name the tests jar as "tests.jar"
      .filter(_.contains("tests.jar"))
      .foreach(f => {
        import java.io.FileInputStream
        import java.util.jar.JarInputStream
        val jarInput = new JarInputStream(new FileInputStream(f))
        try {
          Iterator
            .continually(jarInput.getNextJarEntry)
            .takeWhile(_ != null)
            .map(_.getName)
            .filter(_.endsWith(".class"))
            // scala may generate some extra classes
            .filter(!_.contains('$'))
            .map(_.replace('/', '.'))
            .map(clzName => clzName.substring(0, clzName.length - ".class".length))
            .foreach(clzName => {
              def listSuperClassName = (clz: Class[_]) => {
                val buf = new ArrayBuffer[String]
                val superClz = clz.getAnnotatedSuperclass
                if (superClz != null) buf += superClz.getType.getTypeName
                clz.getAnnotatedInterfaces.foreach(interfaceClz => buf += interfaceClz.getType.getTypeName)
                logger.info(s"${clz.getName} have ${buf.mkString(", ")}")
                buf.toArray
              }
              val clz = Class.forName(clzName)
              if (clz.getSimpleName.startsWith("Test")) {
                val validClzs = listSuperClassName(clz).filter(c => validTestName.contains(c))
                withClue(s"$clzName should extend one of ${validTestName.mkString(", ")}") { validClzs.size shouldBe 1 }
                logger.info(s"$clzName matches ${validClzs.head}")
              } else logger.info(s"${clz.getName} doesn't belong to test case. Skip")
            })
        } finally if (jarInput != null) jarInput.close()
      })
  }
}
