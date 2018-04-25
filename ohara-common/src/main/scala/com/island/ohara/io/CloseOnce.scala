package com.island.ohara.io

import scala.util.{Failure, Success, Try}

/**
  * A framework to implement the auto-close.
  * see TestCloseOnce for examples.
  */
trait CloseOnce extends AutoCloseable {
  private[this] var closed = false

  protected def doClose(): Unit

  def checkClose(): Unit = if (closed) throw new IllegalStateException("have closed")

  def isClosed: Boolean = closed

  final override def close(): Unit = {
    checkClose
    try doClose() finally closed = true
  }
}

object CloseOnce {
  def doFinally[A, B](generator: => A)(worker: A => B)(closer: A => Unit): B = {
    Try(generator) match {
      case Success(obj) => {
        try worker(obj)
        finally closer(obj)
      }
      case Failure(e) => throw e
    }
  }

  def doClose[A <: java.lang.AutoCloseable, B](generator: => A)(worker: A => B): B = {
    Try(generator) match {
      case Success(obj) => {
        try worker(obj)
        finally obj.close()
      }
      case Failure(e) => throw e
    }
  }
}
