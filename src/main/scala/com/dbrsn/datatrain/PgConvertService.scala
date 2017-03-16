package com.dbrsn.datatrain

import cats.data.Coproduct
import cats.~>
import com.dbrsn.datatrain.dsl.meta.ContentDSL
import com.dbrsn.datatrain.dsl.meta.MetadataComponent
import com.dbrsn.datatrain.dsl.meta.ResourceDSL
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.slick.ContentJdbcComponent
import com.dbrsn.datatrain.slick.ContentMetadataJdbcComponent
import com.dbrsn.datatrain.slick.HasMetadataKeyColumnType
import com.dbrsn.datatrain.slick.ResourceJdbcComponent
import com.dbrsn.datatrain.slick.ResourceMetadataJdbcComponent
import com.dbrsn.datatrain.slick.postgresql.DefaultProfile
import com.dbrsn.datatrain.slick.postgresql.PostgresComponent

import scala.concurrent.Future
import scala.util.Try

trait PgConvertService[P <: DefaultProfile]
  extends PostgresComponent[P]
    with ResourceJdbcComponent[P]
    with ContentJdbcComponent[P]
    with ResourceMetadataJdbcComponent[P]
    with ContentMetadataJdbcComponent[P]
    with HasMetadataKeyColumnType[P] {
  self: MetadataComponent[Content] =>

  import profile.api._

  val db: P#Backend#Database

  override val resourceMetadata: MetadataComponent[Resource] = new MetadataComponent[Resource] {}
  override val contentMetadata: MetadataComponent[Content] = self

  type MetaCop[A] = Coproduct[Coproduct[Coproduct[ResourceDSL, ContentDSL, ?], resourceMetadata.MetadataDSL, ?], contentMetadata.MetadataDSL, A]

  val metaDbioInterpreter: MetaCop ~> DBIO = ResourceInterpreter or ContentInterpreter or ResourceMetadataInterpreter or ContentMetadataInterpreter
  val tryToDbioInterpreter: Try ~> DBIO = new (Try ~> DBIO) {
    override def apply[A](fa: Try[A]): DBIO[A] = DBIO.from(Future.fromTry(fa))
  }
  val dbioToFutureTransactionalInterpreter: DBIO ~> Future = new (DBIO ~> Future) {
    override def apply[A](fa: DBIO[A]): Future[A] = db.run(fa.transactionally)
  }

  val metaInterpreter: MetaCop ~> Future = metaDbioInterpreter andThen dbioToFutureTransactionalInterpreter
}
