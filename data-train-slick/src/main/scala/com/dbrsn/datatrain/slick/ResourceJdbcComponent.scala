package com.dbrsn.datatrain.slick

import java.time.LocalDateTime

import cats.~>
import com.dbrsn.datatrain.dsl.meta.ResourceDSL
import com.dbrsn.datatrain.dsl.meta.ResourceDSL.Create
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceId
import shapeless.Generic
import shapeless.HNil
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slickless._

trait ResourceJdbcComponent[P <: JdbcProfile] {
  val profile: P

  import profile.api._

  implicit def localDateTimeColumnType: BaseColumnType[LocalDateTime]

  def resourceTableName: String = "dt_resource"

  class ResourceTable(tag: Tag) extends Table[Resource](tag, resourceTableName) {
    def id: Rep[ResourceId] = column[ResourceId]("id", O.PrimaryKey)
    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

    override def * : ProvenShape[Resource] = (id :: createdAt :: HNil).mappedWith(Generic[Resource])
  }

  lazy val resourceTableQuery: TableQuery[ResourceTable] = TableQuery[ResourceTable]

  object ResourceInterpreter extends (ResourceDSL ~> DBIO) {
    override def apply[A](fa: ResourceDSL[A]): DBIO[A] = fa match {
      case Create(resource) => (resourceTableQuery returning resourceTableQuery.map(_.id) into ((v, _) => v)) += resource
    }
  }

}
