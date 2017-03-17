package com.dbrsn.datatrain

import cats.data.Coproduct
import cats.~>
import com.dbrsn.datatrain.slick.ContentJdbcComponent
import com.dbrsn.datatrain.slick.ContentMetadataJdbcComponent
import com.dbrsn.datatrain.slick.LocalDateTimeColumnType
import com.dbrsn.datatrain.slick.ResourceJdbcComponent
import com.dbrsn.datatrain.slick.ResourceMetadataJdbcComponent
import com.dbrsn.datatrain.slick.postgresql.DefaultProfile

import scala.concurrent.Future
import scala.util.Try

trait PostgresComponent[P <: DefaultProfile]
  extends ResourceJdbcComponent[P]
    with ContentJdbcComponent[P]
    with ResourceMetadataJdbcComponent[P]
    with ContentMetadataJdbcComponent[P] {
  import profile.api._

  override val localDateTimeColumnType: LocalDateTimeColumnType[P] = LocalDateTimeColumnType[P]()
  def db: P#Backend#Database

  type MetaCop[A] = Coproduct[Coproduct[Coproduct[ResourceJdbcDSL, ContentJdbcDSL, ?], ResourceMetadataJdbcDSL, ?], ContentMetadataJdbcDSL, A]

  val metaDbioInterpreter: MetaCop ~> DBIO = ResourceInterpreter or ContentInterpreter or ResourceMetadataInterpreter or ContentMetadataInterpreter
  val tryToDbioInterpreter: Try ~> DBIO = new (Try ~> DBIO) {
    override def apply[A](fa: Try[A]): DBIO[A] = DBIO.from(Future.fromTry(fa))
  }
  val dbioTransactionalInterpreter: DBIO ~> DBIO = new (DBIO ~> DBIO) {
    override def apply[A](fa: DBIO[A]): DBIO[A] = fa.transactionally
  }
  val dbioToFutureInterpreter: DBIO ~> Future = new (DBIO ~> Future) {
    override def apply[A](fa: DBIO[A]): Future[A] = db.run(fa)
  }
}
