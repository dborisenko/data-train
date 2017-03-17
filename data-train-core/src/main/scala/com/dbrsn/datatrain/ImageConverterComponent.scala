package com.dbrsn.datatrain

import cats.free.Free
import com.dbrsn.datatrain.dsl.ImageComponent

import scala.language.higherKinds

trait ImageConverterComponent
  extends ImageComponent {

  case class JpegImageConverter[F[_]](
    compression: Int = 80,
    progressive: Boolean = true,
    transformer: Img => Free[F, Img] = (img: Img) => Free.pure[F, Img](img)
  )(implicit I: ImageInject[F])
    extends ((FileExisted, FileNotExisted) => Free[F, FileExisted]) {
    def apply(input: FileExisted, output: FileNotExisted): Free[F, FileExisted] = {
      for {
        originImage <- I.readFromFile(input)
        transformedImage <- transformer(originImage)
        out <- I.writeToJpegFile(transformedImage, output, compression, progressive)
      } yield out
    }
  }

  case class PngImageConverter[F[_]](
    compressionLevel: Int = 9,
    transformer: Img => Free[F, Img] = (img: Img) => Free.pure[F, Img](img)
  )(implicit I: ImageInject[F])
    extends ((FileExisted, FileNotExisted) => Free[F, FileExisted]) {
    def apply(input: FileExisted, output: FileNotExisted): Free[F, FileExisted] = {
      for {
        originImage <- I.readFromFile(input)
        transformedImage <- transformer(originImage)
        out <- I.writeToPngFile(transformedImage, output, compressionLevel)
      } yield out
    }
  }

  case class GifImageConverter[F[_]](
    progressive: Boolean = true,
    transformer: Img => Free[F, Img] = (img: Img) => Free.pure[F, Img](img)
  )(implicit I: ImageInject[F])
    extends ((FileExisted, FileNotExisted) => Free[F, FileExisted]) {
    def apply(input: FileExisted, output: FileNotExisted): Free[F, FileExisted] = {
      for {
        originImage <- I.readFromFile(input)
        transformedImage <- transformer(originImage)
        out <- I.writeToGifFile(transformedImage, output, progressive)
      } yield out
    }
  }

  case class CoverTransformer[F[_]](
    width: Int,
    height: Int
  )(implicit I: ImageInject[F])
    extends (Img => Free[F, Img]) {
    def apply(image: Img): Free[F, Img] = {
      I.cover(image, width, height)
    }
  }

  case class ScaleToTransformer[F[_]](
    width: Int,
    height: Int
  )(implicit I: ImageInject[F])
    extends (Img => Free[F, Img]) {
    def apply(image: Img): Free[F, Img] = {
      I.scaleTo(image, width, height)
    }
  }

  case class ScaleToWidthTransformer[F[_]](
    width: Int
  )(implicit I: ImageInject[F])
    extends (Img => Free[F, Img]) {
    def apply(image: Img): Free[F, Img] = {
      I.scaleToWidth(image, width)
    }
  }

  case class ScaleToHeightTransformer[F[_]](
    height: Int
  )(implicit I: ImageInject[F])
    extends (Img => Free[F, Img]) {
    def apply(image: Img): Free[F, Img] = {
      I.scaleToHeight(image, height)
    }
  }

}
