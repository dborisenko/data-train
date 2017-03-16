package com.dbrsn.datatrain.slick

import cats.~>
import com.dbrsn.datatrain.dsl.meta.MetadataComponent
import com.dbrsn.datatrain.model.Metadata
import com.dbrsn.datatrain.model.MetadataKey
import com.dbrsn.datatrain.model.MetadataValue
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceId
import shapeless.Generic
import shapeless.HNil
import slick.jdbc.JdbcProfile
import slick.lifted.ForeignKeyQuery
import slick.lifted.ProvenShape
import slickless._

trait ResourceMetadataJdbcComponent[P <: JdbcProfile] {
  self: ResourceJdbcComponent[P] with MetadataComponent[Resource] =>
  val profile: P

  import MetadataDSL._
  import profile.api._

  implicit def metadataKeyColumnType: BaseColumnType[MetadataKey]

  def resourceMetadataTableName: String = "dt_resource_metadata"

  class ResourceMetadataTable(tag: Tag) extends Table[Metadata[Resource]](tag, resourceMetadataTableName) {
    def id: Rep[ResourceId] = column[ResourceId]("id", O.PrimaryKey)
    def key: Rep[MetadataKey] = column[MetadataKey]("key")
    def value: Rep[MetadataValue] = column[MetadataValue]("value")

    override def * : ProvenShape[Metadata[Resource]] = (id :: key :: value :: HNil).mappedWith(Generic[Metadata[Resource]])

    def idFk: ForeignKeyQuery[ResourceTable, Resource] = foreignKey(s"fk_dt_${resourceMetadataTableName}_id", id, resourceTableQuery)(_.id)
  }

  lazy val resourceMetadataTableQuery: TableQuery[ResourceMetadataTable] = TableQuery[ResourceMetadataTable]

  object ResourceMetadataInterpreter extends (MetadataDSL ~> DBIO) {
    override def apply[A](fa: MetadataDSL[A]): DBIO[A] = fa match {
      case Create(metadata) => (resourceMetadataTableQuery returning resourceMetadataTableQuery.map(_.id) into ((v, _) => v)) += metadata
    }
  }

}
