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
import com.kaizoku.doku.common.FutureHelpers._
import com.kaizoku.doku.version.BuildInfo._
import com.kaizoku.doku.documents.plugins.{PluginMetadata, PluginService}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait DocumentsRoutes extends RoutesSupport with DocumentsRoutesAnnotations {

  implicit val documentInfoJsonCbs   = CanBeSerialized[DocumentInfoJson]
  implicit val documentCreateJsonCbs = CanBeSerialized[DocumentCreateJson]

  def documentService: DocumentService
  def pluginService: PluginService

  def detailsMetadataToJson(details: DocumentDetails, metadata: Option[PluginMetadata]): DocumentInfoJson =
    DocumentInfoJson(details.id, details.name, metadata)

  val documentsRoutes = pathPrefix("docs") {
    pathEndOrSingleSlash {
      get {
        allDocuments
      } ~
        put {
          entity(as[DocumentCreateJson]) { req =>
            complete(documentService.createNew(req.name).map(detailsMetadataToJson(_, None)))
          }
        }
    } ~
      path("new") {
        get {
          parameters("hintName") { hintName =>
            onSuccess(documentService.createNew(hintName)) { newDetails =>
              redirect("/doc/" + newDetails.id, StatusCodes.Found)
            }
          }
        }
      } ~
      pathPrefix(Segment.repeat(1, separator = Slash)) { str =>
        pathEndOrSingleSlash {
          complete(for {
            det      <- documentService.get(str.head)
            metadata <- pluginService.get(det.id)
          } yield detailsMetadataToJson(det, metadata))
        } ~
          path("raw") {
            complete(documentService.getBody(str.head))
          } ~
          path("save") {
            post {
              entity(as[String]) { newBody =>
                complete(documentService.saveBody(str.head, newBody).mapToOk)
              }
            }
          }
      }
  }

  def allDocuments: Route =
    complete(
      documentService.getAll
        .map(all => Future.sequence(all.map(d => pluginService.get(d.id).map((d, _)))))
        .flatten
        .map(_.map(t => detailsMetadataToJson(t._1, t._2)))
    )
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
    @(ApiModelProperty @field)(value = "Plugin metadata") metadata: Option[PluginMetadata]
)

@ApiModel(description = "New document creation details")
case class DocumentCreateJson(
    @(ApiModelProperty @field)(value = "Desired name") name: String
)
