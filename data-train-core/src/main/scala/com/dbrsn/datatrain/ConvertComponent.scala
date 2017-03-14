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
import com.dbrsn.datatrain.model.ContentType
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.ResourceId
import com.dbrsn.datatrain.util.Clock

import scala.language.higherKinds

trait ConvertComponent[Img, FileExisted, FileNotExisted] {
  self: FsComponent[FileExisted, FileNotExisted]
    with StorageComponent[Content, FileExisted, FileNotExisted]
    with ImageConverterComponent[Img, FileExisted, FileNotExisted]
    with ImageComponent[Img, FileExisted, FileNotExisted]
    with MetadataComponent[Content] =>

  def clock: Clock

  case class Convert[F[_], C[_]](
    contentName: String,
    contentType: ContentType,
    converter: (FileExisted, FileNotExisted) => Free[F, FileExisted],
    metadata: C[MetadataKey]
  )(implicit F: FsInject[F], M: MetadataInject[F], C: ContentInject[F], CT: Traverse[C])
    extends ((FileExisted, ResourceId) => Free[F, Unit]) {

    type FreeF[A] = Free[F, A]

    def traverseMetadata(content: Content, file: FileExisted): FreeF[C[Metadata[Content, MetadataKey]]] = CT.traverse(metadata) { key =>
      val g: FreeF[Metadata[Content, MetadataKey]] = for {
        value <- F.readMetadata(file, key)
        m <- M.createMetadata(content, key, value)
      } yield m
      g
    }

    def apply(input: FileExisted, resourceId: ResourceId): Free[F, Unit] = for {
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
      _ <- F.delete(convertedFile)
      _ <- F.delete(tmpDir)
    } yield ()
  }

}
