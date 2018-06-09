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

  val provider: DocumentProvider = new LocalDirectoryDocumentProvider("C:\\Users\\Nitzan\\Dropbox\\Projects")

  def detailsToDocumentJson(details: DocumentDetails): DocumentInfoJson =
    DocumentInfoJson(details.id, details.name, details.path)

  val documentsRoutes = pathPrefix("docs") {
    pathEndOrSingleSlash {
      allDocuments
    } ~
      pathPrefix(Segment.repeat(1, separator = Slash)) { str =>
        pathEndOrSingleSlash {
          complete(provider.get(str.head).map(detailsToDocumentJson))
        } ~
          path("raw") {
            complete(provider.getBody(str.head))
          } ~
          path("save") {
            post {
              entity(as[String]) { newBody =>
                complete(provider.saveBody(str.head, newBody).map(f => "ok"))
              }
            }
          }
      }
  }

  def allDocuments: Route = complete(provider.getAll.map(_.map(detailsToDocumentJson)))
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
    @(ApiModelProperty @field)(value = "Document name") name: String,
    @(ApiModelProperty @field)(value = "Document path") path: List[String]
)
