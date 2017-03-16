package com.dbrsn.datatrain

import java.io.File

import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ImageSize

import scala.concurrent.Future

trait ConvertService {
  def postJpegCovers(file: File, fileName: String, coverSizes: Seq[ImageSize]): Future[Seq[Content]]
}
