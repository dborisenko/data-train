package com.dbrsn.datatrain.dsl

import java.io.File

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject
import com.dbrsn.datatrain.model.Content

import scala.language.higherKinds

trait StorageComponent[Content, FileExisted, FileNotExisted] {

  sealed trait StorageDSL[A]

  object StorageDSL {

    case class PutContent(content: Content, input: FileExisted) extends StorageDSL[Unit]

    case class GetContent(content: Content, output: FileNotExisted) extends StorageDSL[FileExisted]

  }

  class StorageInject[F[_]](implicit I: Inject[StorageDSL, F]) {
    import StorageDSL._

    final def putContent(content: Content, input: FileExisted): Free[F, Unit] = inject[StorageDSL, F](PutContent(content, input))
    final def getContent(content: Content, output: FileNotExisted): Free[F, FileExisted] = inject[StorageDSL, F](GetContent(content, output))
  }

  object StorageInject {
    implicit def storage[F[_]](implicit I: Inject[StorageDSL, F]): StorageInject[F] = new StorageInject[F]
  }

}

object contentStorage extends StorageComponent[Content, File, File]
