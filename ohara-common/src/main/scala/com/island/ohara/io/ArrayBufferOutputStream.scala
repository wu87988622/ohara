package com.island.ohara.io

import java.io.ByteArrayOutputStream

class ArrayBufferOutputStream(capacity: Int = 32) extends ByteArrayOutputStream(capacity) {
  def buffer(): Array[Byte] = buf
}
