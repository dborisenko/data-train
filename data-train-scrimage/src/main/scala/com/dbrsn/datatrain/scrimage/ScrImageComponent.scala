package com.dbrsn.datatrain.scrimage

import java.io.File

import cats.~>
import com.dbrsn.datatrain.dsl.ImageComponent
import com.dbrsn.datatrain.model.ContentMetadataKey.ImageSizeMetadata
import com.dbrsn.datatrain.model.ImageSize
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter

import scala.util.Try

trait ScrImageComponent
  extends ImageComponent {
  import ImageDSL._

  type ScrImageDSL[A] = ImageDSL[A]

  override type Img = Image
  override type FileExisted = File
  override type FileNotExisted = File

  val ScrImageFileMetadataInterpreter: File => PartialFunction[MetadataKey, Try[MetadataValue]] = (file: File) => PartialFunction {
    case ImageSizeMetadata =>
      Try {
        val image = Image.fromFile(file)
        ImageSizeMetadata(ImageSize(image.width, image.height))
      }
  }

  object ScrImageInterpreter extends (ScrImageDSL ~> Try) {
    override def apply[A](fa: ScrImageDSL[A]): Try[A] = fa match {
      case Cover(input, width, height)                              =>
        Try(input.cover(width, height))
      case ScaleTo(input, width, height)                            =>
        Try(input.scaleTo(width, height))
      case ScaleToWidth(input, width)                               =>
        Try(input.scaleToWidth(width))
      case ScaleToHeight(input, height)                             =>
        Try(input.scaleToHeight(height))
      case ReadFromFile(input)                                      =>
        Try(Image.fromFile(input))
      case WriteToPngFile(input, output, compressionLevel)          =>
        Try(input.output(output)(PngWriter(compressionLevel)))
      case WriteToJpegFile(input, output, compression, progressive) =>
        Try(input.output(output)(JpegWriter(compression, progressive)))
      case WriteToGifFile(input, output, progressive)               =>
        Try(input.output(output)(GifWriter(progressive)))
    }
  }

}
