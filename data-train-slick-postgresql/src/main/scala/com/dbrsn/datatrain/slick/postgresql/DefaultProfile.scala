package com.dbrsn.datatrain.slick.postgresql

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait DefaultProfile extends ExPostgresProfile
  with PgArraySupport
  with PgNetSupport
  with PgLTreeSupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgDate2Support {

  class DefaultAPI extends API
    with ArrayImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants
    with DateTimeImplicits {
  }

  override val api = new DefaultAPI

  protected override def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate
}

object DefaultProfile extends DefaultProfile
