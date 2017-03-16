package com.dbrsn.datatrain.dsl

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.MetadataValue

import scala.language.higherKinds

trait FsComponent[FileExisted, FileNotExisted, DirExisted] {

  sealed trait FsDSL[A]

  object FsDSL {

    case class ReadMetadata[M <: ContentMetadataKey](file: FileExisted, key: M) extends FsDSL[MetadataValue]

    case object CreateTempDir extends FsDSL[DirExisted]

    case class Describe(dir: DirExisted, contentName: String) extends FsDSL[FileNotExisted]

    case class DeleteFile(file: FileExisted) extends FsDSL[Unit]

    case class DeleteDir(dir: DirExisted) extends FsDSL[Unit]

  }

  class FsInject[F[_]](implicit I: Inject[FsDSL, F]) {
    import FsDSL._

    final def readMetadata[M <: ContentMetadataKey](file: FileExisted, key: M): Free[F, MetadataValue] = inject[FsDSL, F](ReadMetadata[M](file, key))
    final def createTempDir: Free[F, DirExisted] = inject[FsDSL, F](CreateTempDir)
    final def describe(dir: DirExisted, contentName: String): Free[F, FileNotExisted] = inject[FsDSL, F](Describe(dir, contentName))
    final def deleteFile(file: FileExisted): Free[F, Unit] = inject[FsDSL, F](DeleteFile(file))
    final def deleteDir(dir: DirExisted): Free[F, Unit] = inject[FsDSL, F](DeleteDir(dir))
  }

  object FsInject {
    implicit def fs[F[_]](implicit I: Inject[FsDSL, F]): FsInject[F] = new FsInject[F]
  }

}
