package com.dbrsn.datatrain.file

import java.io.File

import cats.~>
import com.dbrsn.datatrain.dsl.FsComponent
import com.dbrsn.datatrain.interpreter.ErrorOr
import com.dbrsn.datatrain.model.ContentLengthMetadata
import com.dbrsn.datatrain.model.ContentMd5Metadata
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.dbrsn.datatrain.util.SystemUtil
import com.google.common.io.Files

import scala.util.Try

trait FileComponent {
  self: FsComponent[File, File] =>
  import FsDSL._

  val FileMetadataInterpreter: (File) => (MetadataKey) => Either[Throwable, MetadataValue] = (file: File) => {
    case ContentLengthMetadata =>
      Try(ContentLengthMetadata(file.length())).toEither
    case ContentMd5Metadata    =>
      Try(ContentMd5Metadata(SystemUtil.base64Md5(file))).toEither
  }

  class FileInterpreter(metadataInterpreter: (File, MetadataKey) => ErrorOr[MetadataValue]) extends (FsDSL ~> ErrorOr) {
    override def apply[A](fa: FsDSL[A]): ErrorOr[A] = fa match {
      case CreateTempDir                =>
        Try(Files.createTempDir()).toEither
      case Describe(dir, contentName)   =>
        Try(new File(dir, contentName)).toEither
      case Delete(file)                 =>
        Try {
          file.delete()
          ()
        }.toEither
      case ReadMetadata(file, metadata) =>
        metadataInterpreter(file, metadata)
    }
  }

}
