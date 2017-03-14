package com.dbrsn.datatrain.scrimage

import java.io.File

import cats.~>
import com.dbrsn.datatrain.dsl.ImageComponent
import com.dbrsn.datatrain.interpreter.ErrorOr
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter

import scala.util.Try

trait ScrImageComponent {
  self: ImageComponent[Image, File, File] =>
  import ImageDSL._

  object ScrImageInterpreter extends (ImageDSL ~> ErrorOr) {
    override def apply[A](fa: ImageDSL[A]): ErrorOr[A] = fa match {
      case Cover(input, width, height)                              =>
        Try(input.cover(width, height)).toEither
      case ScaleTo(input, width, height)                            =>
        Try(input.scaleTo(width, height)).toEither
      case ScaleToWidth(input, width)                               =>
        Try(input.scaleToWidth(width)).toEither
      case ScaleToHeight(input, height)                             =>
        Try(input.scaleToHeight(height)).toEither
      case ReadFromFile(input)                                      =>
        Try(Image.fromFile(input)).toEither
      case WriteToPngFile(input, output, compressionLevel)          =>
        Try(input.output(output)(PngWriter(compressionLevel))).toEither
      case WriteToJpegFile(input, output, compression, progressive) =>
        Try(input.output(output)(JpegWriter(compression, progressive))).toEither
      case WriteToGifFile(input, output, progressive)               =>
        Try(input.output(output)(GifWriter(progressive))).toEither
    }
  }

}
