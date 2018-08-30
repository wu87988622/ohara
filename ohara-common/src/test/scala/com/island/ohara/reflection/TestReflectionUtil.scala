package com.island.ohara.reflection

import java.util.Comparator

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestReflectionUtil extends SmallTest with Matchers {

  @Test
  def testInstantiate(): Unit = {
    val cmp: Comparator[String] =
      ReflectionUtil.instantiate(classOf[ComparatorImpl].getName, classOf[Comparator[String]])
    cmp.getClass shouldBe classOf[ComparatorImpl]

    val cmp2 = ReflectionUtil.instantiate(classOf[ComparatorImpl].getName, classOf[Comparator[String]])
    cmp2.getClass shouldBe classOf[ComparatorImpl]

    val cmp3: Option[Comparator[String]] =
      ReflectionUtil.tryInstantiate("YOU CAN'T CATCH ME", classOf[Comparator[String]])
    cmp3 shouldBe None

    val cmp4: Option[Comparator[String]] =
      ReflectionUtil.tryInstantiate(classOf[ComparatorImpl].getName, classOf[Comparator[String]])
    cmp4.get.getClass shouldBe classOf[ComparatorImpl]
  }

  @Test
  def testInstantiateWithArgument(): Unit = {
    val cmp: Comparator[String] =
      ReflectionUtil.instantiate(classOf[ComparatorImplWithArgument].getName,
                                 classOf[Comparator[String]],
                                 (classOf[String], "name"))
    cmp.getClass shouldBe classOf[ComparatorImplWithArgument]

    val cmp2 = ReflectionUtil
      .instantiate(classOf[ComparatorImplWithArgument].getName, classOf[Comparator[String]], (classOf[String], "name"))
    cmp2.getClass shouldBe classOf[ComparatorImplWithArgument]

    val cmp3: Option[Comparator[String]] =
      ReflectionUtil.tryInstantiate("YOU CAN'T CATCH ME", classOf[Comparator[String]])
    cmp3 shouldBe None

    val cmp4: Option[Comparator[String]] =
      ReflectionUtil.tryInstantiate(classOf[ComparatorImplWithArgument].getName,
                                    classOf[Comparator[String]],
                                    (classOf[String], "name"))
    cmp4.get.getClass shouldBe classOf[ComparatorImplWithArgument]
  }
}
