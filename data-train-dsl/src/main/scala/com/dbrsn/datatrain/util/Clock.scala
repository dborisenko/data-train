package com.dbrsn.datatrain.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate

trait Clock {
  def timezone: DateTimeZone
  def now: DateTime
  def today: LocalDate
}

object Clock {
  def apply() = new Clock {
    val timezone = DateTimeZone.UTC
    def now: DateTime = DateTime.now(timezone)
    def today: LocalDate = LocalDate.now(timezone)
  }
}
