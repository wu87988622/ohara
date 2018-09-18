package com.island.ohara.serialization

import java.io._

import com.island.ohara.io.CloseOnce._

/**
  * If the data doesn't have "binary" compatibility, using this serializer is fine.
  * However, it would be better to control the compatibility by other more stable (and efficient) library
  */
object ObjectSerializer extends Serializer[Any] {

  override def to(obj: Any): Array[Byte] = doClose(new ByteArrayOutputStream()) { bo =>
    doClose(new ObjectOutputStream(bo)) { output =>
      output.writeObject(obj)
      bo.toByteArray
    }
  }

  override def from(serial: Array[Byte]): Any = doClose(new ByteArrayInputStream(serial)) { bi =>
    doClose(new ObjectInputStream(bi))(_.readObject())
  }
}
