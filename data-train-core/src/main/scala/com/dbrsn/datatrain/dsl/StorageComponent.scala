package com.dbrsn.datatrain.dsl

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject

import scala.language.higherKinds

trait StorageComponent {
  type StorageContent
  type FileExisted
  type FileNotExisted

  sealed trait StorageDSL[A]

  object StorageDSL {

    case class PutContent(content: StorageContent, input: FileExisted) extends StorageDSL[Unit]

    case class GetContent(content: StorageContent, output: FileNotExisted) extends StorageDSL[FileExisted]

  }

  class StorageInject[F[_]](implicit I: Inject[StorageDSL, F]) {
    import StorageDSL._

    final def putContent(content: StorageContent, input: FileExisted): Free[F, Unit] = inject[StorageDSL, F](PutContent(content, input))
    final def getContent(content: StorageContent, output: FileNotExisted): Free[F, FileExisted] = inject[StorageDSL, F](GetContent(content, output))
  }

  object StorageInject {
    implicit def storage[F[_]](implicit I: Inject[StorageDSL, F]): StorageInject[F] = new StorageInject[F]
  }

}
