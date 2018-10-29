package com.island.ohara.io
import java.net.InetAddress
import java.util.Calendar

object IoUtil {
  def hostname: String = InetAddress.getLocalHost.getHostName

  def anyLocalAddress: String = "0.0.0.0"

  def timezone: String = Calendar.getInstance.getTimeZone.getID

  def path(parent: String, name: String): String = if (parent.endsWith("/")) parent + name else s"$parent/$name"

  def name(path: String): String = {
    if (path == "/") throw new IllegalArgumentException(s"no file name for path:$path")
    val last = path.lastIndexOf("/")
    if (last == -1) path
    else path.substring(last + 1)
  }
}
