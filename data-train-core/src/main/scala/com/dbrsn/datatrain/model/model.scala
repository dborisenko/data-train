package com.dbrsn.datatrain.model

import java.time.LocalDateTime

sealed trait Identified {
  type Id
}

final case class Content(
  id: ContentId,
  createdAt: LocalDateTime,
  resourceId: ResourceId,
  contentType: Option[ContentType],
  contentName: String
) extends Identified {
  override type Id = ContentId
}

final case class Resource(
  id: ResourceId,
  createdAt: LocalDateTime
) extends Identified {
  override type Id = ResourceId
}
