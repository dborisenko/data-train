package com.dbrsn.datatrain

import java.io.File

import cats.data.Coproduct
import cats.instances.all._
import cats.~>
import com.dbrsn.datatrain.aws.AmazonS3StorageConfig
import com.dbrsn.datatrain.aws.AwsComponent
import com.dbrsn.datatrain.dsl.FsComponent
import com.dbrsn.datatrain.dsl.ImageComponent
import com.dbrsn.datatrain.dsl.StorageComponent
import com.dbrsn.datatrain.dsl.meta.MetadataComponent
import com.dbrsn.datatrain.file.FileComponent
import com.dbrsn.datatrain.interpreter.RetryErrors
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.dbrsn.datatrain.scrimage.ScrImageComponent
import com.dbrsn.datatrain.util.Clock
import com.sksamuel.scrimage.Image
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.Try

trait OpsConvertService extends ConvertComponent[Image, File, File, File, Set]
  with FsComponent[File, File, File]
  with StorageComponent[Content, File, File]
  with ImageConverterComponent[Image, File, File]
  with ImageComponent[Image, File, File]
  with FileComponent
  with ScrImageComponent
  with MetadataComponent[Content] {

  override val clock: Clock = Clock()
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def retriesOnError(attempt: Int, e: Throwable): Boolean = {
    logger.error(s"Error (attempt $attempt out of 3) ${e.getMessage}", e)
    attempt < 3
  }

  val metadataHandler: File => MetadataKey => Try[MetadataValue] = (file: File) => FileMetadataInterpreter(file) orElse ScrImageFileMetadataInterpreter(file)

  val fileInterpreter: FsDSL ~> Try = new FileInterpreter(metadataHandler)
  val fileRetryInterpreter: FsDSL ~> Try = new RetryErrors[FsDSL, Try, Throwable](fileInterpreter, retriesOnError)

  val imageInterpreter: ImageDSL ~> Try = ScrImageInterpreter

  val storageInterpreter: StorageDSL ~> Try
  val storageRetryInterpreter: StorageDSL ~> Try = new RetryErrors[StorageDSL, Try, Throwable](storageInterpreter, retriesOnError)

  type OpsCop[A] = Coproduct[Coproduct[FsDSL, ImageDSL, ?], StorageDSL, A]

  val opsInterpreter: OpsCop ~> Try = fileRetryInterpreter or imageInterpreter or storageRetryInterpreter
}

trait AwsConvertService extends OpsConvertService with AwsComponent {
  val config: AmazonS3StorageConfig

  override val storageInterpreter: StorageDSL ~> Try = new AwsInterpreter(config)
}
