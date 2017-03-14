package com.dbrsn.datatrain.dsl

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.MetadataKey

import scala.language.higherKinds

trait FsComponent[FileExisted, FileNotExisted] {

  sealed trait FsDSL[A]

  object FsDSL {

    case class ReadMetadata[M <: MetadataKey](file: FileExisted, key: M) extends FsDSL[M#Value]

    case object CreateTempDir extends FsDSL[FileExisted]

    case class Describe(dir: FileExisted, contentName: String) extends FsDSL[FileNotExisted]

    case class Delete(file: FileExisted) extends FsDSL[Unit]

  }

  class FsInject[F[_]](implicit I: Inject[FsDSL, F]) {
    import FsDSL._

    final def readMetadata[M <: MetadataKey](file: FileExisted, key: M): Free[F, M#Value] = inject[FsDSL, F](ReadMetadata[M](file, key))
    final def createTempDir: Free[F, FileExisted] = inject[FsDSL, F](CreateTempDir)
    final def describe(dir: FileExisted, contentName: String): Free[F, FileNotExisted] = inject[FsDSL, F](Describe(dir, contentName))
    final def delete(file: FileExisted): Free[F, Unit] = inject[FsDSL, F](Delete(file))
  }

  object FsInject {
    implicit def fs[F[_]](implicit I: Inject[FsDSL, F]): FsInject[F] = new FsInject[F]
  }

}
