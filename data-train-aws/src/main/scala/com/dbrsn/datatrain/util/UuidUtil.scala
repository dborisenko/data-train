package com.dbrsn.datatrain.util

import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

object UuidUtil {
  private val encoder = Base64.getUrlEncoder
  private val decoder = Base64.getUrlDecoder

  private val lastRedundantCharacters = "=="

  def toBase64(uuid: UUID): String = {
    val uuidBytes = ByteBuffer.wrap(new Array[Byte](16))
    uuidBytes.putLong(uuid.getMostSignificantBits)
    uuidBytes.putLong(uuid.getLeastSignificantBits)
    val result = encoder.encodeToString(uuidBytes.array())
    if (result.endsWith(lastRedundantCharacters)) {
      result.substring(0, result.length - lastRedundantCharacters.length)
    } else {
      result
    }
  }

  def toUuid(str: String): UUID = try {
    UUID.fromString(str)
  } catch {
    case e: IllegalArgumentException =>
      val uuidBytes = ByteBuffer.wrap(decoder.decode(str))
      val result = new UUID(uuidBytes.getLong, uuidBytes.getLong)
      if (uuidBytes.hasRemaining) {
        throw new IllegalArgumentException("Invalid UUID string: " + str)
      }
      result
  }
}
