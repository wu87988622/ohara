package com.island.ohara.connector.jdbc.util

import java.util.{Calendar, TimeZone}

object DateTimeUtils {
  private val timeZoneID: String = System.getProperty("user.timezone")
  private val UTC: TimeZone = TimeZone.getTimeZone(timeZoneID)

  val CALENDAR: Calendar = Calendar.getInstance(UTC)
}
