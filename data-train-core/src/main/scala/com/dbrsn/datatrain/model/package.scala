package com.dbrsn.datatrain

import java.util.UUID

package object model {
  type ContentId = UUID

  object ContentId {
    def newContentId: ContentId = UUID.randomUUID()
  }

  type ResourceId = UUID

  object ResourceId {
    def newResourceId: ResourceId = UUID.randomUUID()
  }

  type ContentType = String
  type Md5 = String
  type MetadataValue = String
}
