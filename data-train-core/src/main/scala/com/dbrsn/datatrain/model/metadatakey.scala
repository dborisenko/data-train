package com.dbrsn.datatrain.model

import cats.syntax.either._
import enumeratum.Enum
import enumeratum.EnumEntry
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.parser.decode

import scala.collection.immutable.IndexedSeq
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

sealed trait ContentMetadataKey extends EnumEntry with MetadataKey

object ContentMetadataKey extends Enum[ContentMetadataKey] {
  override def values: IndexedSeq[ContentMetadataKey] = findValues

  case object ContentLengthMetadata extends LongMetadataKey with ContentMetadataKey

  case object ContentMd5Metadata extends StringMetadataKey with ContentMetadataKey

  case object ImageSizeMetadata extends GenericMetadataKey[ImageSize] with ContentMetadataKey

}

sealed trait ResourceMetadataKey extends EnumEntry with MetadataKey

object ResourceMetadataKey extends Enum[ResourceMetadataKey] {
  override def values: IndexedSeq[ResourceMetadataKey] = findValues

  case object FileNameMetadata extends StringMetadataKey with ResourceMetadataKey

}
