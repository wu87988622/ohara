package com.island.ohara.reflection

trait ClassName {
  // `final` prevents subclasses to change method behavior.
  final def name: String = this.getClass.getSimpleName
}
