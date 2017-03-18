package com.dbrsn.datatrain.dsl.meta

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.Resource

import scala.language.higherKinds

sealed trait ContentDSL[A]

object ContentDSL {

  final case class Create(content: Content) extends ContentDSL[Content]

}

class ContentInject[F[_]](implicit I: Inject[ContentDSL, F]) {
  import ContentDSL._

  final def createContent(resource: Content): Free[F, Content] = inject[ContentDSL, F](Create(resource))
}

object ContentInject {
  implicit def content[F[_]](implicit I: Inject[ContentDSL, F]): ContentInject[F] = new ContentInject[F]
}

sealed trait ResourceDSL[A]

object ResourceDSL {

  final case class Create(resource: Resource) extends ResourceDSL[Resource]

}

class ResourceInject[F[_]](implicit I: Inject[ResourceDSL, F]) {
  import ResourceDSL._

  final def createResource(resource: Resource): Free[F, Resource] = inject[ResourceDSL, F](Create(resource))
}

object ResourceInject {
  implicit def resource[F[_]](implicit I: Inject[ResourceDSL, F]): ResourceInject[F] = new ResourceInject[F]
}

sealed trait ContentMetadataDSL[A]

object ContentMetadataDSL {

  case class Create(metadata: Metadata[Content]) extends ContentMetadataDSL[Metadata[Content]]

}

class ContentMetadataInject[F[_]](implicit I: Inject[ContentMetadataDSL, F]) {
  import ContentMetadataDSL._

  final def createContentMetadata(metadata: Metadata[Content]): Free[F, Metadata[Content]] = inject[ContentMetadataDSL, F](Create(metadata))
}

object ContentMetadataInject {
  implicit def contentMetadata[F[_]](implicit I: Inject[ContentMetadataDSL, F]): ContentMetadataInject[F] = new ContentMetadataInject[F]
}

sealed trait ResourceMetadataDSL[A]

object ResourceMetadataDSL {

  case class Create(metadata: Metadata[Resource]) extends ResourceMetadataDSL[Metadata[Resource]]

}

class ResourceMetadataInject[F[_]](implicit I: Inject[ResourceMetadataDSL, F]) {
  import ResourceMetadataDSL._

  final def createResourceMetadata(metadata: Metadata[Resource]): Free[F, Metadata[Resource]] = inject[ResourceMetadataDSL, F](Create(metadata))
}

object ResourceMetadataInject {
  implicit def resourceMetadata[F[_]](implicit I: Inject[ResourceMetadataDSL, F]): ResourceMetadataInject[F] = new ResourceMetadataInject[F]
}
