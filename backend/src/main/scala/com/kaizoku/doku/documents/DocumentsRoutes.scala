package com.kaizoku.doku.documents

import java.util.UUID
import java.util.regex.Pattern
import java.nio.file._
import java.nio.file.attribute._
import java.nio.charset.StandardCharsets
import javax.ws.rs.{Path => JPath}

import scala.io.Source._
import scala.annotation.meta.field
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import io.circe.generic.auto._
import io.swagger.annotations.{ApiResponse, _}

import com.kaizoku.doku.common.api.RoutesSupport
import com.kaizoku.doku.version.BuildInfo._

import scala.concurrent.ExecutionContext.Implicits.global

trait DocumentsRoutes extends RoutesSupport with DocumentsRoutesAnnotations {

  implicit val documentJsonCbs = CanBeSerialized[DocumentInfoJson]

  def documentService: DocumentService

  def detailsToDocumentJson(details: DocumentDetails): DocumentInfoJson =
    DocumentInfoJson(details.id, details.name)

  val documentsRoutes = pathPrefix("docs") {
    pathEndOrSingleSlash {
      allDocuments
    } ~
      pathPrefix(Segment.repeat(1, separator = Slash)) { str =>
        pathEndOrSingleSlash {
          complete(documentService.get(str.head).map(detailsToDocumentJson))
        } ~
          path("raw") {
            complete(documentService.getBody(str.head))
          } ~
          path("save") {
            post {
              entity(as[String]) { newBody =>
                complete(documentService.saveBody(str.head, newBody).map(_ => StatusCodes.OK))
              }
            }
          }
      }
  }

  def allDocuments: Route = complete(documentService.getAll.map(_.map(detailsToDocumentJson)))
}

@Api(
  value = "Documents",
  produces = "application/json",
  consumes = "application/json"
)
@JPath("api/docs")
trait DocumentsRoutesAnnotations {

  @ApiOperation(
    httpMethod = "GET",
    response = classOf[List[DocumentInfoJson]],
    value = "Returns an object which describes running version"
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 500, message = "Internal Server Error"),
      new ApiResponse(code = 200, message = "OK", response = classOf[List[DocumentInfoJson]])
    )
  )
  @JPath("/")
  def allDocuments: Route
}

@ApiModel(description = "Metadata about a document")
case class DocumentInfoJson(
    @(ApiModelProperty @field)(value = "Document id") id: String,
    @(ApiModelProperty @field)(value = "Document name") name: String
)
