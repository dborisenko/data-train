package com.dbrsn.datatrain.slick

import cats.~>
import com.dbrsn.datatrain.dsl.meta.ResourceDSL
import com.dbrsn.datatrain.dsl.meta.ResourceDSL.Create
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceId
import org.joda.time.DateTime
import slick.ast.TypedType
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape


trait ResourceRelationalComponent[P <: JdbcProfile] {
  val profile: P

  import profile.api._

  implicit def uuidTypedType: TypedType[ResourceId]
  implicit def dateTimeTypedType: TypedType[DateTime]

  def resourceTableName: String = "dt_resource"

  class ResourceTable(tag: Tag) extends Table[Resource](tag, resourceTableName) {

    def id: Rep[ResourceId] = column[ResourceId]("id", O.PrimaryKey)
    def createdAt: Rep[DateTime] = column[DateTime]("created_at")

    override def * : ProvenShape[Resource] = (id :: createdAt :: HNil).mappedWith(Generic[Resource])
  }

  lazy val resourceTableQuery: TableQuery[ResourceTable] = TableQuery[ResourceTable]

  object ResourceInterpreter extends (ResourceDSL ~> DBIO) {
    override def apply[A](fa: ResourceDSL[A]): DBIO[A] = fa match {
      case Create(resource) => (resourceTableQuery returning resourceTableQuery.map(_.id) into ((v, _) => v)) += resource
    }
  }

}
