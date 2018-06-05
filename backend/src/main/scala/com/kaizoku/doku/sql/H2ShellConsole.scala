package com.kaizoku.doku.sql

import com.kaizoku.doku.common.sql.{DatabaseConfig, SqlDatabase}
import com.typesafe.config.ConfigFactory

object H2ShellConsole extends App {
  val config = new DatabaseConfig {
    def rootConfig = ConfigFactory.load()
  }

  println("Note: when selecting from tables, enclose the table name in \" \".")
  new org.h2.tools.Shell().runTool("-url", SqlDatabase.embeddedConnectionStringFromConfig(config))
}