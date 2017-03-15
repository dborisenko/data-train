package com.dbrsn.datatrain.model

final case class Metadata[T <: Identified](
  id: T#Id,
  key: MetadataKey,
  value: MetadataValue
)
