package com.dbrsn.datatrain.slick

import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.ResourceMetadataKey
import slick.jdbc.JdbcProfile

trait HasMetadataKeyColumnType[P <: JdbcProfile] {
  val profile: P

  import profile.api._

  def metadataKeyColumnType: BaseColumnType[MetadataKey] = MappedColumnType.base(
    {
      case v: ContentMetadataKey  => v.entryName
      case v: ResourceMetadataKey => v.entryName
    },
    (name: String) => ContentMetadataKey.withNameInsensitiveOption(name).getOrElse(ResourceMetadataKey.withNameInsensitive(name))
  )

}
