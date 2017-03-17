package com.dbrsn.datatrain

import java.io.File

import cats.data.Coproduct
import cats.~>
import com.dbrsn.datatrain.aws.AwsStorageConfig
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentLengthMetadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentMd5Metadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ImageSizeMetadata
import com.dbrsn.datatrain.model.ImageSize
import com.dbrsn.datatrain.model.MimeType
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceMetadataKey
import com.dbrsn.datatrain.slick.ContentMetadataComponent
import com.dbrsn.datatrain.slick.MetadataKeyColumnType
import com.dbrsn.datatrain.slick.ResourceMetadataComponent
import com.dbrsn.datatrain.slick.postgresql.DefaultProfile
import com.dbrsn.datatrain.util.Clock

import scala.concurrent.Future

class DefaultConvertService[P <: DefaultProfile](
  val config: AwsStorageConfig,
  val profile: DefaultProfile,
  val db: DefaultProfile#Backend#Database,
  val clock: Clock
) extends ConvertService
  with BatchConvertComponent
  with ImageConverterComponent
  with ScrImageAwsStorageComponent
  with PostgresComponent[P] {

  import profile.api._

  override type ConvertCollection[A] = List[A]
  override type MetadataCollection[A] = List[A]

  type Cop[A] = Coproduct[OpsCop, MetaCop, A]

  override val metadataKeyColumnType: MetadataKeyColumnType[P] = MetadataKeyColumnType[P]()(MappedColumnType.base(
    {
      case v: ContentMetadataKey  => v.entryName
      case v: ResourceMetadataKey => v.entryName
    },
    (name: String) => ContentMetadataKey.withNameInsensitiveOption(name).getOrElse(ResourceMetadataKey.withNameInsensitive(name))
  ))

  override val resourceMetadata: ResourceMetadataComponent = new ResourceMetadataComponent {}
  override val contentMetadata: ContentMetadataComponent = new ContentMetadataComponent {}

  val FileDefaultMetadata: List[ContentMetadataKey] = List(ContentLengthMetadata, ContentMd5Metadata)
  val ImageFileDefaultMetadata: List[ContentMetadataKey] = FileDefaultMetadata :+ ImageSizeMetadata

  val interpreter: Cop ~> Future = ((opsInterpreter andThen tryToDbioInterpreter) or metaDbioInterpreter) andThen dbioTransactionalInterpreter andThen dbioToFutureInterpreter

  override def postJpegCovers(file: File, fileName: String, coverSizes: Seq[ImageSize]): Future[Covers] = {
    val program = NormalizedBatchConvert[Cop](
      normalizer = Convert[Cop](
        contentName = s"origin.${MimeType.jpg.extension}",
        contentType = Some(MimeType.jpg.contentType),
        converter = JpegImageConverter[Cop](),
        metadata = ImageFileDefaultMetadata
      ),
      convertFromNormalized = coverSizes.toList.map { size =>
        Convert[Cop](
          contentName = s"${size.width}x${size.height}.${MimeType.jpg.extension}",
          contentType = Some(MimeType.jpg.contentType),
          converter = JpegImageConverter[Cop](),
          metadata = ImageFileDefaultMetadata
        )
      }
    )
    program(file, fileName).map { v =>
      Covers(
        resource = v.select[Resource],
        origin = v.select[Content],
        covers = v.select[List[Content]])
    }.foldMap(interpreter)
  }
}
