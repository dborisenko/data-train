package com.dbrsn.datatrain.model

import org.joda.time.DateTime

sealed trait Identified {
  type Id
}

final case class Content(
  id: ContentId,
  createdAt: DateTime,
  resourceId: ResourceId,
  contentType: ContentType,
  contentName: String
) extends Identified {
  override type Id = ContentId
}

final case class Resource(
  id: ResourceId,
  createdAt: DateTime
) extends Identified {
  override type Id = ResourceId
}

trait MetadataKey {
  type Value
}

trait SpecificMetadataKey[T] extends MetadataKey

final case class Metadata[T <: Identified, M <: MetadataKey](
  id: T#Id,
  key: M,
  value: M#Value
)