package com.dbrsn.datatrain

import cats.Traverse
import cats.free.Free
import com.dbrsn.datatrain.dsl.FsComponent
import com.dbrsn.datatrain.dsl.meta.ContentInject
import com.dbrsn.datatrain.dsl.meta.ContentMetadataInject
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentId
import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.ContentType
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.ResourceId
import com.dbrsn.datatrain.util.Clock
import shapeless._

import scala.language.higherKinds

trait ConvertComponent
  extends FsComponent {

  def clock: Clock

  type MetadataCollection[A]

  case class Convert[F[_]](
    contentName: String,
    contentType: Option[ContentType],
    converter: (FileExisted, FileNotExisted) => Free[F, FileExisted],
    metadata: MetadataCollection[ContentMetadataKey]
  )(implicit F: FsInject[F], M: ContentMetadataInject[F], C: ContentInject[F], CT: Traverse[MetadataCollection])
    extends ((FileExisted, ResourceId) => Free[F, Content]) {

    type FreeF[A] = Free[F, A]

    def traverseMetadata(content: Content, file: FileExisted): FreeF[MetadataCollection[Metadata[Content]]] = CT.traverse(metadata) { key =>
      val g: FreeF[Metadata[Content]] = for {
        value <- F.readMetadata(file, key)
        m <- M.createContentMetadata(Metadata[Content](
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
      _ <- F.deleteFile(out.select[FileExisted])
      _ <- F.deleteDir(out.select[DirExisted])
    } yield out.select[Content]

  }

}
