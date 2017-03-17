package com.dbrsn.datatrain.file

import java.io.File

import cats.~>
import com.dbrsn.datatrain.dsl.FsComponent
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentLengthMetadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentMd5Metadata
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.dbrsn.datatrain.util.SystemUtil
import com.google.common.io.Files

import scala.util.Try

trait FileComponent
  extends FsComponent {
  import FsDSL._

  type FileFsDSL[A] = FsDSL[A]

  override type FileExisted = File
  override type FileNotExisted = File
  override type DirExisted = File

  val FileMetadataInterpreter: File => PartialFunction[MetadataKey, Try[MetadataValue]] = (file: File) => PartialFunction {
    case ContentLengthMetadata =>
      Try(ContentLengthMetadata(file.length()))
    case ContentMd5Metadata    =>
      Try(ContentMd5Metadata(SystemUtil.base64Md5(file)))
  }

  class FileInterpreter(metadataInterpreter: File => MetadataKey => Try[MetadataValue]) extends (FileFsDSL ~> Try) {
    override def apply[A](fa: FileFsDSL[A]): Try[A] = fa match {
      case CreateTempDir                =>
        Try(Files.createTempDir())
      case Describe(dir, contentName)   =>
        Try(new File(dir, contentName))
      case DeleteFile(file)             =>
        Try {
          file.delete()
          ()
        }
      case DeleteDir(dir)               =>
        Try {
          dir.delete()
          ()
        }
      case ReadMetadata(file, metadata) =>
        metadataInterpreter(file)(metadata)
    }
  }

}
