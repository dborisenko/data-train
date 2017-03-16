package com.dbrsn.datatrain.slick.postgresql

import java.time.LocalDateTime

import com.dbrsn.datatrain.slick.ContentJdbcComponent
import com.dbrsn.datatrain.slick.ContentMetadataJdbcComponent
import com.dbrsn.datatrain.slick.ResourceJdbcComponent
import com.dbrsn.datatrain.slick.ResourceMetadataJdbcComponent

trait PostgresComponent[P <: DefaultProfile] {
  self: ResourceJdbcComponent[P]
    with ContentJdbcComponent[P]
    with ResourceMetadataJdbcComponent[P]
    with ContentMetadataJdbcComponent[P] =>
  val profile: P

  import profile.api._

  override val localDateTimeColumnType: BaseColumnType[LocalDateTime] = implicitly[BaseColumnType[LocalDateTime]]
}
