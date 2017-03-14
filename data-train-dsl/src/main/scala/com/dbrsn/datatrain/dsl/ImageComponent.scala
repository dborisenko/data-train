package com.dbrsn.datatrain.dsl

import cats.free.Free
import cats.free.Free.inject
import cats.free.Inject

import scala.language.higherKinds

trait ImageComponent[Img, FileExisted, FileNotExisted] {

  sealed trait ImageDSL[A]

  object ImageDSL {

    case class Cover(input: Img, width: Int, height: Int) extends ImageDSL[Img]

    case class ScaleTo(input: Img, width: Int, height: Int) extends ImageDSL[Img]

    case class ScaleToWidth(input: Img, width: Int) extends ImageDSL[Img]

    case class ScaleToHeight(input: Img, height: Int) extends ImageDSL[Img]

    case class ReadFromFile(input: FileExisted) extends ImageDSL[Img]

    case class WriteToPngFile(input: Img, output: FileNotExisted, compressionLevel: Int = 9) extends ImageDSL[FileExisted]

    case class WriteToJpegFile(input: Img, output: FileNotExisted, compression: Int = 80, progressive: Boolean = true) extends ImageDSL[FileExisted]

    case class WriteToGifFile(input: Img, output: FileNotExisted, progressive: Boolean = true) extends ImageDSL[FileExisted]

  }

  class ImageInject[F[_]](implicit I: Inject[ImageDSL, F]) {
    import ImageDSL._

    final def cover(input: Img, width: Int, height: Int): Free[F, Img] = inject[ImageDSL, F](Cover(input, width, height))
    final def scaleTo(input: Img, width: Int, height: Int): Free[F, Img] = inject[ImageDSL, F](ScaleTo(input, width, height))
    final def scaleToWidth(input: Img, width: Int): Free[F, Img] = inject[ImageDSL, F](ScaleToWidth(input, width))
    final def scaleToHeight(input: Img, height: Int): Free[F, Img] = inject[ImageDSL, F](ScaleToHeight(input, height))
    final def readFromFile(input: FileExisted): Free[F, Img] = inject[ImageDSL, F](ReadFromFile(input))
    final def writeToPngFile(input: Img, output: FileNotExisted, compressionLevel: Int = 9): Free[F, FileExisted] =
      inject[ImageDSL, F](WriteToPngFile(input, output, compressionLevel))
    final def writeToJpegFile(input: Img, output: FileNotExisted, compression: Int = 80, progressive: Boolean = true): Free[F, FileExisted] =
      inject[ImageDSL, F](WriteToJpegFile(input, output, compression, progressive))
    final def writeToGifFile(input: Img, output: FileNotExisted, progressive: Boolean = true): Free[F, FileExisted] =
      inject[ImageDSL, F](WriteToGifFile(input, output, progressive))
  }

  object ImageInject {
    implicit def image[F[_]](implicit I: Inject[ImageDSL, F]): ImageInject[F] = new ImageInject[F]
  }

}
