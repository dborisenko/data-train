package com.dbrsn.datatrain.slick

import cats.~>
import com.dbrsn.datatrain.dsl.meta.MetadataComponent
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentId
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import shapeless._
import slick.ast.TypedType
import slick.jdbc.JdbcProfile
import slick.lifted.ForeignKeyQuery
import slick.lifted.ProvenShape
import slickless._

trait ContentMetadataRelationalComponent[P <: JdbcProfile] {
  self: ContentRelationalComponent[P] with MetadataComponent[Content] =>
  val profile: P

  import MetadataDSL._
  import profile.api._

  implicit def idKey: TypedType[ContentId]
  implicit def metadataKey: TypedType[MetadataKey]

  def contentMetadataTableName: String = "dt_content_metadata"

  class ContentMetadataTable(tag: Tag) extends Table[Metadata[Content]](tag, contentMetadataTableName) {
    def id: Rep[ContentId] = column[ContentId]("id", O.PrimaryKey)
    def key: Rep[MetadataKey] = column[MetadataKey]("key")
    def value: Rep[MetadataValue] = column[MetadataValue]("value")

    override def * : ProvenShape[Metadata[Content]] = (id :: key :: value :: HNil).mappedWith(Generic[Metadata[Content]])

    def idFk: ForeignKeyQuery[ContentTable, Content] = foreignKey(s"fk_dt_${contentMetadataTableName}_id", id, contentTableQuery)(_.id)
  }

  lazy val contentMetadataTableQuery: TableQuery[ContentMetadataTable] = TableQuery[ContentMetadataTable]

  object ContentMetadataInterpreter extends (MetadataDSL ~> DBIO) {
    override def apply[A](fa: MetadataDSL[A]): DBIO[A] = fa match {
      case Create(metadata) => (contentMetadataTableQuery returning contentMetadataTableQuery.map(_.id) into ((v, _) => v)) += metadata
    }
  }

}
