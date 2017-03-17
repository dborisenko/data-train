package com.dbrsn.datatrain

import java.io.File

import cats.data.Coproduct
import cats.instances.all._
import cats.~>
import com.dbrsn.datatrain.aws.AwsStorageComponent
import com.dbrsn.datatrain.aws.AwsStorageConfig
import com.dbrsn.datatrain.file.FileComponent
import com.dbrsn.datatrain.interpreter.RetryErrors
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.dbrsn.datatrain.scrimage.ScrImageComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.Try

trait ScrImageAwsStorageComponent
  extends ScrImageComponent
    with AwsStorageComponent
    with FileComponent {

  def config: AwsStorageConfig
  def maxErrorRetries: Int = 3

  type OpsCop[A] = Coproduct[Coproduct[FileFsDSL, ScrImageDSL, ?], AwsStorageDSL, A]

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def retriesOnError(attempt: Int, e: Throwable): Boolean = {
    logger.error(s"Error (attempt $attempt out of $maxErrorRetries) ${e.getMessage}", e)
    attempt < maxErrorRetries
  }

  val metadataHandler: File => MetadataKey => Try[MetadataValue] = (file: File) => FileMetadataInterpreter(file) orElse ScrImageFileMetadataInterpreter(file)

  val fileInterpreter: FileFsDSL ~> Try = new FileInterpreter(metadataHandler)
  val fileRetryInterpreter: FileFsDSL ~> Try = new RetryErrors[FileFsDSL, Try, Throwable](fileInterpreter, retriesOnError)

  val imageInterpreter: ScrImageDSL ~> Try = ScrImageInterpreter

  val storageInterpreter: AwsStorageDSL ~> Try = new AwsStorageInterpreter(config)
  val storageRetryInterpreter: AwsStorageDSL ~> Try = new RetryErrors[AwsStorageDSL, Try, Throwable](storageInterpreter, retriesOnError)

  val opsInterpreter: OpsCop ~> Try = fileRetryInterpreter or imageInterpreter or storageRetryInterpreter
}
