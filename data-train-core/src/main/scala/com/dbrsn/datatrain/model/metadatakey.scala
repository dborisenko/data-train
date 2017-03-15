package com.dbrsn.datatrain.model

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.parser.decode

trait MetadataKey {
  type Value

  def encodeValue(value: Value): MetadataValue
  def decodeValue(value: MetadataValue): Option[Value]
}

class GenericMetadataKey[T: Decoder : Encoder] extends MetadataKey {
  override type Value = T
  private val encoder: Encoder[T] = implicitly[Encoder[Value]]
  override def encodeValue(value: Value): MetadataValue = encoder.apply(value).noSpaces
  override def decodeValue(value: MetadataValue): Option[Value] = decode[Value](value).toOption

  def apply(value: Value): MetadataValue = encodeValue(value)
}

trait SpecificMetadataKey[T] extends MetadataKey

case object ContentLengthMetadata extends GenericMetadataKey[Long] with SpecificMetadataKey[Content]

case object ContentMd5Metadata extends GenericMetadataKey[Md5] with SpecificMetadataKey[Content]

case object ImageSizeMetadata extends GenericMetadataKey[ImageSize] with SpecificMetadataKey[Content]

case object UserFileNameMetadata extends GenericMetadataKey[String] with SpecificMetadataKey[Resource]
