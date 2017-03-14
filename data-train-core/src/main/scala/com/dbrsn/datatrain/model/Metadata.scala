package com.dbrsn.datatrain.model

final case class Metadata[T <: Identified, M <: MetadataKey](
  id: T#Id,
  key: M,
  value: MetadataValue
) {
  lazy val typedValue: Option[M#Value] = key.decodeValue(value)
}
