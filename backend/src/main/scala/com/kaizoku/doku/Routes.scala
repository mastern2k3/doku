package com.kaizoku.doku

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import com.kaizoku.doku.common.api.RoutesRequestWrapper
import com.kaizoku.doku.passwordreset.api.PasswordResetRoutes
import com.kaizoku.doku.swagger.SwaggerDocService
import com.kaizoku.doku.user.api.UsersRoutes
import com.kaizoku.doku.version.VersionRoutes

trait Routes extends RoutesRequestWrapper with UsersRoutes with PasswordResetRoutes with VersionRoutes {

  def system: ActorSystem
  def config: ServerConfig

  lazy val routes = requestWrapper {
    pathPrefix("api") {
      passwordResetRoutes ~
        usersRoutes ~
        versionRoutes
    } ~
      getFromResourceDirectory("webapp") ~
      new SwaggerDocService(config.serverHost, config.serverPort, system).routes ~
      path("") {
        getFromResource("webapp/index.htm")
      }
  }
}
