package com.dbrsn.datatrain.scrimage

import java.io.File

import cats.~>
import com.dbrsn.datatrain.dsl.FsComponent
import com.dbrsn.datatrain.interpreter.ErrorOr
import com.dbrsn.datatrain.model.ImageSize
import com.dbrsn.datatrain.model.ImageSizeMetadata
import com.sksamuel.scrimage.Image

import scala.util.Try

trait ScrImageFsComponent {
  self: FsComponent[File, File] =>
  import FsDSL._

  object ScrImageFsInterpreter extends (FsDSL ~> ErrorOr) {
    override def apply[A](fa: FsDSL[A]): ErrorOr[A] = fa match {
      case ReadMetadata(file, ImageSizeMetadata) =>
        Try {
          val image = Image.fromFile(file)
          ImageSize(image.width, image.height)
        }.toEither
    }
  }

}
