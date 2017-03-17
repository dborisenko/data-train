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

case class LocalDateTimeColumnType[P <: JdbcProfile](implicit columnType: P#BaseColumnType[LocalDateTime])

trait ResourceJdbcComponent[P <: JdbcProfile] {

  val localDateTimeColumnType: LocalDateTimeColumnType[P]
  val profile: P

  import profile.api._
  import localDateTimeColumnType._

  type ResourceJdbcDSL[A] = ResourceDSL[A]

  def resourceTableName: String = "dt_resource"

  class ResourceTable(tag: Tag) extends Table[Resource](tag, resourceTableName) {
    def id: Rep[ResourceId] = column[ResourceId]("id", O.PrimaryKey)
    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

    override def * : ProvenShape[Resource] = (id :: createdAt :: HNil).mappedWith(Generic[Resource])
  }

  lazy val resourceTableQuery: TableQuery[ResourceTable] = TableQuery[ResourceTable]

  object ResourceInterpreter extends (ResourceJdbcDSL ~> DBIO) {
    override def apply[A](fa: ResourceJdbcDSL[A]): DBIO[A] = fa match {
      case Create(resource) => (resourceTableQuery returning resourceTableQuery.map(_.id) into ((v, _) => v)) += resource
    }
  }

}
