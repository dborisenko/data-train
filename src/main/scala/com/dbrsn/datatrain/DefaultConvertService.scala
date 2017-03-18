package com.dbrsn.datatrain

import java.io.File

import cats.data.Coproduct
import cats.implicits._
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
import com.dbrsn.datatrain.slick.ContentMetadataJdbcComponent
import com.dbrsn.datatrain.slick.LocalDateTimeColumnType
import com.dbrsn.datatrain.slick.MetadataKeyColumnType
import com.dbrsn.datatrain.slick.ResourceJdbcComponent
import com.dbrsn.datatrain.slick.ResourceMetadataJdbcComponent
import com.dbrsn.datatrain.slick.postgresql.DefaultProfile
import com.dbrsn.datatrain.util.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

class DefaultConvertService[P <: DefaultProfile](
  val config: AwsStorageConfig,
  val profile: P,
  val db: P#Backend#Database,
  val clock: Clock = Clock(),
  maxErrorRetries: Int = 3
)(implicit ec: ExecutionContext) extends ConvertService
  with BatchConvertComponent
  with ImageConverterComponent
  with ScrImageComponent
  with AwsStorageComponent
  with FileComponent
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

  type Cop1[A] = Coproduct[ScrImageDSL, AwsStorageDSL, A]
  type Cop2[A] = Coproduct[FileFsDSL, Cop1, A]
  type Cop3[A] = Coproduct[ContentMetadataJdbcDSL, Cop2, A]
  type Cop4[A] = Coproduct[ResourceMetadataJdbcDSL, Cop3, A]
  type Cop5[A] = Coproduct[ContentJdbcDSL, Cop4, A]
  type Cop[A] = Coproduct[ResourceJdbcDSL, Cop5, A]

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

  val metadataHandler: File => MetadataKey => Try[MetadataValue] = (file: File) => FileMetadataInterpreter(file) orElse ScrImageFileMetadataInterpreter(file)

  val fileInterpreter: FileFsDSL ~> Try = new FileInterpreter(metadataHandler)
  val fileRetryInterpreter: FileFsDSL ~> Try = new RetryErrors[FileFsDSL, Try, Throwable](fileInterpreter, retriesOnError)

  val imageInterpreter: ScrImageDSL ~> Try = ScrImageInterpreter

  val storageInterpreter: AwsStorageDSL ~> Try = new AwsStorageInterpreter(config)
  val storageRetryInterpreter: AwsStorageDSL ~> Try = new RetryErrors[AwsStorageDSL, Try, Throwable](storageInterpreter, retriesOnError)

  val tryToDbioInterpreter: Try ~> DBIO = new (Try ~> DBIO) {
    override def apply[A](fa: Try[A]): DBIO[A] = DBIO.from(Future.fromTry(fa))
  }
  val dbioTransactionalInterpreter: DBIO ~> DBIO = new (DBIO ~> DBIO) {
    override def apply[A](fa: DBIO[A]): DBIO[A] = fa.transactionally
  }
  val dbioToFutureInterpreter: DBIO ~> Future = new (DBIO ~> Future) {
    override def apply[A](fa: DBIO[A]): Future[A] = db.run(fa)
  }

  val dbioInterpreter1: Cop1 ~> DBIO = (imageInterpreter andThen tryToDbioInterpreter) or (storageRetryInterpreter andThen tryToDbioInterpreter)
  val dbioInterpreter2: Cop2 ~> DBIO = (fileRetryInterpreter andThen tryToDbioInterpreter) or dbioInterpreter1
  val dbioInterpreter3: Cop3 ~> DBIO = ContentMetadataInterpreter or dbioInterpreter2
  val dbioInterpreter4: Cop4 ~> DBIO = ResourceMetadataInterpreter or dbioInterpreter3
  val dbioInterpreter5: Cop5 ~> DBIO = ContentInterpreter or dbioInterpreter4
  val dbioInterpreter: Cop ~> DBIO = ResourceInterpreter or dbioInterpreter5

  val dbioInterpreterToTransactional: Cop ~> DBIO = dbioInterpreter andThen dbioTransactionalInterpreter
  val futureInterpreter: Cop ~> Future = dbioInterpreterToTransactional andThen dbioToFutureInterpreter

  val FileDefaultMetadata: MetadataCollection[ContentMetadataKey] = List(ContentLengthMetadata, ContentMd5Metadata)
  val ImageFileDefaultMetadata: MetadataCollection[ContentMetadataKey] = FileDefaultMetadata :+ ImageSizeMetadata

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
        covers = v.select[ConvertCollection[Content]])
    }.foldMap(futureInterpreter)
  }
}
