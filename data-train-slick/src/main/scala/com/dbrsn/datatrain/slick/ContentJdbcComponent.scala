package com.dbrsn.datatrain.slick

import java.time.LocalDateTime

import cats.~>
import com.dbrsn.datatrain.dsl.meta.ContentDSL
import com.dbrsn.datatrain.dsl.meta.ContentDSL.Create
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.ContentId
import com.dbrsn.datatrain.model.ContentType
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceId
import shapeless.Generic
import shapeless.HNil
import slick.jdbc.JdbcProfile
import slick.lifted.ForeignKeyQuery
import slick.lifted.ProvenShape
import slickless._

class ContentJdbcComponent[P <: JdbcProfile](
  val profile: P,
  val resourceJdbcC: ResourceJdbcComponent[P]
)(implicit ct: P#BaseColumnType[LocalDateTime]) {
  import resourceJdbcC._
  import profile.api._

  type ContentJdbcDSL[A] = ContentDSL[A]

  def contentTableName: String = "dt_content"

  class ContentTable(tag: Tag) extends Table[Content](tag, contentTableName) {
    def id: Rep[ContentId] = column[ContentId]("id", O.PrimaryKey)
    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")
    def resourceId: Rep[ResourceId] = column[ResourceId]("resource_id")
    def contentType: Rep[Option[ContentType]] = column[Option[ContentType]]("content_type")
    def contentName: Rep[String] = column[String]("content_name")

    override def * : ProvenShape[Content] = (id :: createdAt :: resourceId :: contentType :: contentName :: HNil).mappedWith(Generic[Content])

    def resourceIdFk: ForeignKeyQuery[ResourceTable, Resource] = foreignKey(s"fk_${contentTableName}_resource_id", resourceId, resourceTableQuery)(_.id)
  }

  lazy val contentTableQuery: TableQuery[ContentTable] = TableQuery[ContentTable]

  object ContentInterpreter extends (ContentJdbcDSL ~> DBIO) {
    override def apply[A](fa: ContentJdbcDSL[A]): DBIO[A] = fa match {
      case Create(content) => (contentTableQuery returning contentTableQuery.map(_.id) into ((v, _) => v)) += content
    }
  }

}
