package com.kaizoku.doku.swagger

import com.github.swagger.akka.model.Info

import akka.actor.ActorSystem
import com.github.swagger.akka._
import com.kaizoku.doku.version.VersionRoutes
import com.kaizoku.doku.documents.DocumentsRoutes
import com.kaizoku.doku.version.BuildInfo._

class SwaggerDocService(address: String, port: Int, system: ActorSystem) extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set( // add here routes in order to add to swagger
    classOf[VersionRoutes],
    classOf[DocumentsRoutes],
  )
  override val host        = address + ":" + port
  override val info        = Info(version = buildDate, title = "Doku")
  override val apiDocsPath = "api-docs"
}
