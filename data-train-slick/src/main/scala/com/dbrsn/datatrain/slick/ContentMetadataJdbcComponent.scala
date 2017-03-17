package com.dbrsn.datatrain.slick

import cats.~>
import com.dbrsn.datatrain.dsl.meta.MetadataComponent
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentId
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import shapeless.Generic
import shapeless.HNil
import slick.jdbc.JdbcProfile
import slick.lifted.ForeignKeyQuery
import slick.lifted.ProvenShape
import slickless._

trait ContentMetadataComponent extends MetadataComponent {
  override type Target = Content
}

trait ContentMetadataJdbcComponent[P <: JdbcProfile] {
  self: ContentJdbcComponent[P] =>

  val profile: P
  val metadataKeyColumnType: MetadataKeyColumnType[P]
  val contentMetadata: ContentMetadataComponent

  import contentMetadata._
  import MetadataDSL._
  import profile.api._
  import metadataKeyColumnType._

  type ContentMetadataJdbcDSL[A] = MetadataDSL[A]

  def contentMetadataTableName: String = "dt_content_metadata"

  class ContentMetadataTable(tag: Tag) extends Table[Metadata[Content]](tag, contentMetadataTableName) {
    def id: Rep[ContentId] = column[ContentId]("id", O.PrimaryKey)
    def key: Rep[MetadataKey] = column[MetadataKey]("key")
    def value: Rep[MetadataValue] = column[MetadataValue]("value")

    override def * : ProvenShape[Metadata[Content]] = (id :: key :: value :: HNil).mappedWith(Generic[Metadata[Content]])

    def idFk: ForeignKeyQuery[ContentTable, Content] = foreignKey(s"fk_dt_${contentMetadataTableName}_id", id, contentTableQuery)(_.id)
  }

  lazy val contentMetadataTableQuery: TableQuery[ContentMetadataTable] = TableQuery[ContentMetadataTable]

  object ContentMetadataInterpreter extends (ContentMetadataJdbcDSL ~> DBIO) {
    override def apply[A](fa: ContentMetadataJdbcDSL[A]): DBIO[A] = fa match {
      case Create(metadata) => (contentMetadataTableQuery returning contentMetadataTableQuery.map(_.id) into ((v, _) => v)) += metadata
    }
  }

}
