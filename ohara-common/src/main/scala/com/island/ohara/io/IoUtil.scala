package com.island.ohara.io
import java.net.InetAddress

object IoUtil {
  def hostname: String = InetAddress.getLocalHost.getHostName
}
