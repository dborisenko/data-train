package com.dbrsn.datatrain.dsl.meta

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.Identified
import com.dbrsn.datatrain.model.Metadata

import scala.language.higherKinds

trait MetadataComponent[T <: Identified] {

  sealed trait MetadataDSL[A]

  object MetadataDSL {

    case class Create(metadata: Metadata[T]) extends MetadataDSL[Metadata[T]]

  }

  class MetadataInject[F[_]](implicit I: Inject[MetadataDSL, F]) {
    import MetadataDSL._

    final def createMetadata(metadata: Metadata[T]): Free[F, Metadata[T]] = inject[MetadataDSL, F](Create(metadata))
  }

  object MetadataInject {
    implicit def metadata[F[_]](implicit I: Inject[MetadataDSL, F]): MetadataInject[F] = new MetadataInject[F]
  }

}
