package com.dbrsn.datatrain

import java.io.File

import cats.data.Coproduct
import cats.instances.all._
import cats.~>
import com.dbrsn.datatrain.aws.AwsStorageComponent
import com.dbrsn.datatrain.aws.AwsStorageConfig
import com.dbrsn.datatrain.file.FileComponent
import com.dbrsn.datatrain.interpreter.RetryErrors
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentMetadataKey
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentLengthMetadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ContentMd5Metadata
import com.dbrsn.datatrain.model.ContentMetadataKey.ImageSizeMetadata
import com.dbrsn.datatrain.model.ImageSize
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.dbrsn.datatrain.model.MimeType
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceMetadataKey
import com.dbrsn.datatrain.scrimage.ScrImageComponent
import com.dbrsn.datatrain.slick.ContentJdbcComponent
import com.dbrsn.datatrain.slick.ContentMetadataComponent
import com.dbrsn.datatrain.slick.ContentMetadataJdbcComponent
import com.dbrsn.datatrain.slick.LocalDateTimeColumnType
import com.dbrsn.datatrain.slick.MetadataKeyColumnType
import com.dbrsn.datatrain.slick.ResourceJdbcComponent
import com.dbrsn.datatrain.slick.ResourceMetadataComponent
import com.dbrsn.datatrain.slick.ResourceMetadataJdbcComponent
import com.dbrsn.datatrain.slick.postgresql.DefaultProfile
import com.dbrsn.datatrain.util.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Try

class DefaultConvertService[P <: DefaultProfile](
  val config: AwsStorageConfig,
  val profile: P,
  val db: P#Backend#Database,
  val clock: Clock,
  maxErrorRetries: Int = 3
) extends ConvertService
  with BatchConvertComponent
  with ImageConverterComponent
  with ScrImageComponent
  with AwsStorageComponent
  with FileComponent
  with ContentMetadataComponent
  with ResourceJdbcComponent[P]
  with ContentJdbcComponent[P]
  with ResourceMetadataJdbcComponent[P]
  with ContentMetadataJdbcComponent[P] {

  import profile.api._

  override type FileExisted = File
  override type FileNotExisted = File
  override type DirExisted = File
  override type ConvertCollection[A] = List[A]
  override type MetadataCollection[A] = List[A]

  type Cop1[A] = Coproduct[ResourceJdbcDSL, ContentJdbcDSL, A]
  type Cop2[A] = Coproduct[Cop1, ResourceMetadataJdbcDSL, A]
  type Cop3[A] = Coproduct[Cop2, ContentMetadataJdbcDSL, A]
  type Cop4[A] = Coproduct[Cop3, FileFsDSL, A]
  type Cop5[A] = Coproduct[Cop4, ScrImageDSL, A]
  type Cop[A] = Coproduct[Cop5, AwsStorageDSL, A]

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def retriesOnError(attempt: Int, e: Throwable): Boolean = {
    logger.error(s"Error (attempt $attempt out of $maxErrorRetries) ${e.getMessage}", e)
    attempt < maxErrorRetries
  }

  override val metadataKeyColumnType: MetadataKeyColumnType[P] = MetadataKeyColumnType[P]()(MappedColumnType.base(
    {
      case v: ContentMetadataKey  => v.entryName
      case v: ResourceMetadataKey => v.entryName
    },
    (name: String) => ContentMetadataKey.withNameInsensitiveOption(name).getOrElse(ResourceMetadataKey.withNameInsensitive(name))
  ))
  override val localDateTimeColumnType: LocalDateTimeColumnType[P] = LocalDateTimeColumnType[P]()

  override val resourceMetadata: ResourceMetadataComponent = new ResourceMetadataComponent {}
  override val contentMetadata: ContentMetadataComponent = this

  val metadataHandler: File => MetadataKey => Try[MetadataValue] = (file: File) => FileMetadataInterpreter(file) orElse ScrImageFileMetadataInterpreter(file)

  val fileInterpreter: FileFsDSL ~> Try = new FileInterpreter(metadataHandler)
  val fileRetryInterpreter: FileFsDSL ~> Try = new RetryErrors[FileFsDSL, Try, Throwable](fileInterpreter, retriesOnError)

  val imageInterpreter: ScrImageDSL ~> Try = ScrImageInterpreter

  val storageInterpreter: AwsStorageDSL ~> Try = new AwsStorageInterpreter(config)
  val storageRetryInterpreter: AwsStorageDSL ~> Try = new RetryErrors[AwsStorageDSL, Try, Throwable](storageInterpreter, retriesOnError)

  val FileDefaultMetadata: List[ContentMetadataKey] = List(ContentLengthMetadata, ContentMd5Metadata)
  val ImageFileDefaultMetadata: List[ContentMetadataKey] = FileDefaultMetadata :+ ImageSizeMetadata

  val tryToDbioInterpreter: Try ~> DBIO = new (Try ~> DBIO) {
    override def apply[A](fa: Try[A]): DBIO[A] = DBIO.from(Future.fromTry(fa))
  }
  val dbioTransactionalInterpreter: DBIO ~> DBIO = new (DBIO ~> DBIO) {
    override def apply[A](fa: DBIO[A]): DBIO[A] = fa.transactionally
  }
  val dbioToFutureInterpreter: DBIO ~> Future = new (DBIO ~> Future) {
    override def apply[A](fa: DBIO[A]): Future[A] = db.run(fa)
  }

  val interpreter: Cop ~> Future = (ResourceInterpreter or ContentInterpreter or ResourceMetadataInterpreter or ContentMetadataInterpreter or
    (fileRetryInterpreter andThen tryToDbioInterpreter) or (imageInterpreter andThen tryToDbioInterpreter) or
    (storageRetryInterpreter andThen tryToDbioInterpreter)) andThen dbioTransactionalInterpreter andThen dbioToFutureInterpreter

  override def postJpegCovers(file: File, fileName: String, coverSizes: Seq[ImageSize]): Future[Covers] = {
    val program = NormalizedBatchConvert[Cop](
      normalizer = Convert[Cop](
        contentName = s"origin.${MimeType.jpg.extension}",
        contentType = Some(MimeType.jpg.contentType),
        converter = JpegImageConverter[Cop](),
        metadata = ImageFileDefaultMetadata
      ).applyWithoutDeleting,
      convertFromNormalized = coverSizes.map { size =>
        Convert[Cop](
          contentName = s"${size.width}x${size.height}.${MimeType.jpg.extension}",
          contentType = Some(MimeType.jpg.contentType),
          converter = JpegImageConverter[Cop](),
          metadata = ImageFileDefaultMetadata
        )
      }.toList
    )
    program(file, fileName).map { v =>
      Covers(
        resource = v.select[Resource],
        origin = v.select[Content],
        covers = v.select[List[Content]])
    }.foldMap(interpreter)
  }
}
