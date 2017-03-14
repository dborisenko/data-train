package com.dbrsn.datatrain.model

case object ContentLengthMetadata extends SpecificMetadataKey[Content] {
  override type Value = Long
}

case object ContentMd5Metadata extends SpecificMetadataKey[Content] {
  override type Value = Md5
}

case object ImageSizeMetadata extends SpecificMetadataKey[Content] {
  override type Value = ImageSize
}

case object UserFileNameMetadata extends SpecificMetadataKey[Resource] {
  override type Value = String
}
