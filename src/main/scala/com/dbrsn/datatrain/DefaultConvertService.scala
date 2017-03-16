package com.dbrsn.datatrain

import java.io.File

import cats.data.Coproduct
import cats.implicits._
import cats.~>
import com.dbrsn.datatrain.aws.AmazonS3StorageConfig
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentLengthMetadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentMd5Metadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ImageSizeMetadata
import com.dbrsn.datatrain.model.ImageSize
import com.dbrsn.datatrain.model.MimeType
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.slick.postgresql.DefaultProfile

import scala.concurrent.Future

class DefaultConvertService(
  val config: AmazonS3StorageConfig,
  val profile: DefaultProfile,
  val db: DefaultProfile#Backend#Database
) extends ConvertService
  with OpsConvertService
  with AwsConvertService
  with PgConvertService[DefaultProfile] {

  type Cop[A] = Coproduct[OpsCop, MetaCop, A]

  val FileDefaultMetadata: List[ContentMetadataKey] = List(ContentLengthMetadata, ContentMd5Metadata)
  val ImageFileDefaultMetadata: List[ContentMetadataKey] = FileDefaultMetadata :+ ImageSizeMetadata

  val interpreter: Cop ~> Future = ((opsInterpreter andThen tryToDbioInterpreter) or metaDbioInterpreter) andThen dbioToFutureTransactionalInterpreter

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
