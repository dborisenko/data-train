package com.dbrsn.datatrain.util

import java.time.LocalDateTime

trait Clock {
  def now: LocalDateTime
}

object Clock {
  def apply() = new Clock {
    def now: LocalDateTime = LocalDateTime.now
  }
}
