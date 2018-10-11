package com.island.ohara.util

trait Timer {
  def current(): Long
}

private[this] class DefaultSystemTime extends Timer {
  def current(): Long = {
    System.currentTimeMillis()
  }
}

object SystemUtil {
  @volatile private[this] var timer: Timer = new DefaultSystemTime()

  def current(): Long = {
    this.timer.current()
  }

  def inject(timer: Timer): Unit = {
    this.timer = timer
  }
}
