package com.kaizoku.doku.common.sql

import com.kaizoku.doku.common.ConfigWithDefault
import com.kaizoku.doku.common.sql.DatabaseConfig._
import com.typesafe.config.Config

trait DatabaseConfig extends ConfigWithDefault {
  def rootConfig: Config

  // format: OFF
  lazy val dbH2Url              = getString(s"doku.db.h2.properties.url", "jdbc:h2:file:./data/doku")
  lazy val dbPostgresServerName = getString(PostgresServerNameKey, "")
  lazy val dbPostgresPort       = getString(PostgresPortKey, "5432")
  lazy val dbPostgresDbName     = getString(PostgresDbNameKey, "")
  lazy val dbPostgresUsername   = getString(PostgresUsernameKey, "")
  lazy val dbPostgresPassword   = getString(PostgresPasswordKey, "")
}

object DatabaseConfig {
  val PostgresDSClass       = "doku.db.postgres.dataSourceClass"
  val PostgresServerNameKey = "doku.db.postgres.properties.serverName"
  val PostgresPortKey       = "doku.db.postgres.properties.portNumber"
  val PostgresDbNameKey     = "doku.db.postgres.properties.databaseName"
  val PostgresUsernameKey   = "doku.db.postgres.properties.user"
  val PostgresPasswordKey   = "doku.db.postgres.properties.password"
  // format: ON
}
