package com.island.ohara.configurator.store

import java.util.Comparator

class StringComparator extends Comparator[String] {
  override def compare(o1: String, o2: String): Int = o1.compareTo(o2)
}
