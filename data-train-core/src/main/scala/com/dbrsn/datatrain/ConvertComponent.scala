package com.dbrsn.datatrain

import cats.Traverse
import cats.free.Free
import com.dbrsn.datatrain.dsl.FsComponent
import com.dbrsn.datatrain.dsl.ImageComponent
import com.dbrsn.datatrain.dsl.StorageComponent
import com.dbrsn.datatrain.dsl.meta.ContentInject
import com.dbrsn.datatrain.dsl.meta.MetadataComponent
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentId
import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.ContentType
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.ResourceId
import com.dbrsn.datatrain.util.Clock
import shapeless._

import scala.language.higherKinds

trait ConvertComponent[Img, FileExisted, FileNotExisted, DirExisted, MetadataCollection[_]] {
  self: FsComponent[FileExisted, FileNotExisted, DirExisted]
    with StorageComponent[Content, FileExisted, FileNotExisted]
    with ImageConverterComponent[Img, FileExisted, FileNotExisted]
    with ImageComponent[Img, FileExisted, FileNotExisted]
    with MetadataComponent[Content] =>

  def clock: Clock

  case class Convert[F[_]](
    contentName: String,
    contentType: Option[ContentType],
    converter: (FileExisted, FileNotExisted) => Free[F, FileExisted],
    metadata: MetadataCollection[ContentMetadataKey]
  )(implicit F: FsInject[F], M: MetadataInject[F], C: ContentInject[F], CT: Traverse[MetadataCollection])
    extends ((FileExisted, ResourceId) => Free[F, Content]) {

    type FreeF[A] = Free[F, A]

    def traverseMetadata(content: Content, file: FileExisted): FreeF[MetadataCollection[Metadata[Content]]] = CT.traverse(metadata) { key =>
      val g: FreeF[Metadata[Content]] = for {
        value <- F.readMetadata(file, key)
        m <- M.createMetadata(Metadata[Content](
          id = content.id,
          key = key,
          value = value
        ))
      } yield m
      g
    }

    def applyWithoutDeleting(input: FileExisted, resourceId: ResourceId): Free[F, Content :: DirExisted :: FileExisted :: HNil] = for {
      tmpDir <- F.createTempDir
      output <- F.describe(tmpDir, contentName)
      convertedFile <- converter(input, output)
      content <- C.createContent(Content(
        id = ContentId.newContentId,
        createdAt = clock.now,
        resourceId = resourceId,
        contentType = contentType,
        contentName = contentName
      ))
      _ <- traverseMetadata(content, convertedFile)
    } yield content :: tmpDir :: convertedFile :: HNil

    def apply(input: FileExisted, resourceId: ResourceId): Free[F, Content] = for {
      out <- applyWithoutDeleting(input, resourceId)
      _ <- delete(out.select[FileExisted], out.select[DirExisted])
    } yield out.select[Content]

    def delete(file: FileExisted, dir: DirExisted): Free[F, Unit] = for {
      _ <- F.deleteFile(file)
      _ <- F.deleteDir(dir)
    } yield ()
  }

}
