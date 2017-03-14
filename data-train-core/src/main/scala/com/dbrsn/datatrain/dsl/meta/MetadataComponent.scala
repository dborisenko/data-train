package com.dbrsn.datatrain.dsl.meta

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.Identified
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.MetadataKey

import scala.language.higherKinds

trait MetadataComponent[T <: Identified] {

  sealed trait MetadataDSL[A]

  object MetadataDSL {

    case class Create[M <: MetadataKey](content: T, key: M, value: M#Value) extends MetadataDSL[Metadata[T, M]]

  }

  class MetadataInject[F[_]](implicit I: Inject[MetadataDSL, F]) {
    import MetadataDSL._

    final def createMetadata[M <: MetadataKey](content: T, key: M, value: M#Value): Free[F, Metadata[T, M]] = inject[MetadataDSL, F](Create[M](
      content = content,
      key = key,
      value = value
    ))
  }

  object MetadataInject {
    implicit def metadata[F[_]](implicit I: Inject[MetadataDSL, F]): MetadataInject[F] = new MetadataInject[F]
  }

}
