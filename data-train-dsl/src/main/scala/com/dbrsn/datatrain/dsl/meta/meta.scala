package com.dbrsn.datatrain.dsl.meta

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.Content
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
