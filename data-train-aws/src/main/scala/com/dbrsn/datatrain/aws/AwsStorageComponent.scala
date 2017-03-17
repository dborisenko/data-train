package com.dbrsn.datatrain.aws

import java.io.File
import java.io.FileOutputStream

import cats.~>
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.dbrsn.datatrain.dsl.StorageComponent
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.util.UuidUtil
import com.google.common.io.ByteStreams

import scala.util.Try

final case class AwsStorageConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  bucketName: String,
  endpoint: Option[String],
  accessControlList: Option[String]
) {
  val cannedAccessControlList: Option[CannedAccessControlList] = accessControlList.map(CannedAccessControlList.valueOf)
}

trait AwsStorageComponent
  extends StorageComponent {
  import StorageDSL._

  type AwsStorageDSL[A] = StorageDSL[A]

  override type StorageContent = Content
  override type FileExisted = File
  override type FileNotExisted = File

  class AwsStorageInterpreter(config: AwsStorageConfig) extends (AwsStorageDSL ~> Try) {

    private val amazonS3Client: AmazonS3 = (config.accessKey, config.secretKey) match {
      case (Some(credentialsAccessKey), Some(credentialsSecretKey)) =>
        AmazonS3Client
          .builder()
          .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(credentialsAccessKey, credentialsSecretKey)))
          .build()

      case _ =>
        AmazonS3Client
          .builder()
          .withCredentials(InstanceProfileCredentialsProvider.getInstance())
          .build()
    }

    config.endpoint.foreach(amazonS3Client.setEndpoint)

    def contentPath(content: Content): String = {
      UuidUtil.toBase64(content.resourceId) + "/" + content.contentName
    }

    override def apply[A](fa: AwsStorageDSL[A]): Try[A] = fa match {
      case PutContent(content, inputFile) =>
        Try {
          val src = contentPath(content)
          val s3Object = amazonS3Client.putObject(config.bucketName, src, inputFile)
          config.cannedAccessControlList.foreach(amazonS3Client.setObjectAcl(config.bucketName, src, _))
          ()
        }

      case GetContent(content, outputFile) =>
        Try {
          val src = contentPath(content)
          val s3Object = amazonS3Client.getObject(config.bucketName, src)
          try {
            val inputStream = s3Object.getObjectContent
            try {
              val outputStream = new FileOutputStream(outputFile)
              try {
                ByteStreams.copy(inputStream, outputStream)
              } finally {
                outputStream.close()
              }
            } finally {
              inputStream.close()
            }
          } finally {
            s3Object.close()
          }
          outputFile
        }
    }
  }

}
