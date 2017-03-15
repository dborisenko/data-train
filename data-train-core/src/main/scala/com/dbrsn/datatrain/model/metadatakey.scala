package com.dbrsn.datatrain.model

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.parser.decode

import scala.util.Try

trait MetadataKey {
  type Value

  def encodeValue(value: Value): MetadataValue
  def decodeValue(value: MetadataValue): Option[Value]

  def apply(value: Value): MetadataValue = encodeValue(value)
}

class GenericMetadataKey[T: Decoder : Encoder] extends MetadataKey {
  override type Value = T
  private val encoder: Encoder[T] = implicitly[Encoder[Value]]
  override def encodeValue(value: Value): MetadataValue = encoder.apply(value).noSpaces
  override def decodeValue(value: MetadataValue): Option[Value] = decode[Value](value).toOption
}

trait LongMetadataKey extends MetadataKey {
  override type Value = Long
  override def encodeValue(value: Value): MetadataValue = value.toString
  override def decodeValue(str: MetadataValue): Option[Value] = Try(str.toLong).toOption
}

trait StringMetadataKey extends MetadataKey {
  override type Value = String
  override def encodeValue(value: Value): MetadataValue = value
  override def decodeValue(str: MetadataValue): Option[Value] = Option(str)
}

trait SpecificMetadataKey[T] extends MetadataKey

case object ContentLengthMetadata extends LongMetadataKey with SpecificMetadataKey[Content]

case object ContentMd5Metadata extends StringMetadataKey with SpecificMetadataKey[Content]

case object ImageSizeMetadata extends GenericMetadataKey[ImageSize] with SpecificMetadataKey[Content]

case object UserFileNameMetadata extends StringMetadataKey with SpecificMetadataKey[Resource]
