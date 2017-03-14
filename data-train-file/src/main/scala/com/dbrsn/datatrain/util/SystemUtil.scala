package com.dbrsn.datatrain.util

import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

object SystemUtil {
  private val BUF_SIZE = 0x1000

  def md5(file: File): Array[Byte] = {
    val md = MessageDigest.getInstance("MD5")
    val is = new FileInputStream(file)
    try {
      val dis = new DigestInputStream(is, md)
      try {
        val buf: Array[Byte] = new Array[Byte](BUF_SIZE)
        while (dis.read(buf) != -1) {}
      } finally {
        dis.close()
      }
    } finally {
      is.close()
    }
    md.digest()
  }

  def base64Md5(file: File): String = DatatypeConverter.printBase64Binary(md5(file))
}
