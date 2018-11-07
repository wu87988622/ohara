package com.island.ohara.io
import java.net.InetAddress
import java.util.Calendar
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._

object IoUtil {
  private[this] val LOG = Logger(IoUtil.getClass)

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

  def retry[T](function: () => T,
               cleanup: () => Unit = () => {},
               retryCount: Int = 1,
               retryInterval: Duration = 1 second): T = {
    var lastException: Throwable = null
    var remainingRetry = retryCount
    do {
      try return function()
      catch {
        case e: Throwable =>
          LOG.error(s"[FBP] Something fails! remaining retryCount:$retryCount", e)
          lastException = e
          TimeUnit.MILLISECONDS.sleep(retryInterval.toMillis)
          try cleanup()
          catch {
            case e: Throwable => LOG.error("Failed to run the cleanup", e)
          }
      } finally remainingRetry -= 1
    } while (remainingRetry >= 0)
    throw new IllegalArgumentException("still fail...", lastException)
  }
}
