package com.dbrsn.datatrain

import java.io.File

import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ImageSize
import com.dbrsn.datatrain.model.Resource

import scala.concurrent.Future

final case class Covers(resource: Resource, origin: Content, covers: Seq[Content])

trait ConvertService {
  def postJpegCovers(file: File, fileName: String, coverSizes: Seq[ImageSize]): Future[Covers]
}
