package com.dbrsn.datatrain.model

import enumeratum.Enum
import enumeratum.EnumEntry

import scala.collection.immutable.IndexedSeq

sealed abstract class MimeType(
  val contentType: ContentType,
  val extension: String
) extends EnumEntry

object MimeType extends Enum[MimeType] {
  override lazy val values: IndexedSeq[MimeType] = findValues

  case object jpg extends MimeType("image/jpeg", "jpg")

  case object png extends MimeType("image/png", "png")

  case object gif extends MimeType("image/gif", "gif")

  case object bin extends MimeType("application/octet-stream", "bin")

}
