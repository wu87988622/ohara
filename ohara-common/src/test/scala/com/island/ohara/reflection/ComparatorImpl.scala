package com.island.ohara.reflection

import java.util.Comparator

class ComparatorImpl extends Comparator[String] {
  override def compare(o1: String, o2: String): Int = o1.compareTo(o2)
}
