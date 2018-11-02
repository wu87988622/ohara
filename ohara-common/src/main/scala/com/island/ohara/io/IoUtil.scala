package com.island.ohara.io
import java.net.InetAddress
import java.util.Calendar

object IoUtil {
  def hostname: String = InetAddress.getLocalHost.getHostName

  def anyLocalAddress: String = "0.0.0.0"

  def timezone: String = Calendar.getInstance.getTimeZone.getID

  def path(parent: String, name: String): String = if (parent.endsWith("/")) parent + name else s"$parent/$name"

  /**
    * replace the path's parent path by new parent
    * @param parent new parent
    * @param path original path
    * @return new path
    */
  def replaceParent(parent: String, path: String): String = IoUtil.path(parent, IoUtil.name(path))

  def name(path: String): String = {
    if (path == "/") throw new IllegalArgumentException(s"no file name for path:$path")
    val last = path.lastIndexOf("/")
    if (last == -1) path
    else path.substring(last + 1)
  }
}
