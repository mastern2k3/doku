package com.kaizoku.doku.documents

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.kaizoku.doku.common.api.RoutesSupport
import com.kaizoku.doku.version.BuildInfo._
import io.circe.generic.auto._
import io.swagger.annotations.{ApiResponse, _}
import java.nio.file.{Path => _}
import scala.io.Source._

import scala.annotation.meta.field

trait DocumentsRoutes extends RoutesSupport with DocumentsRoutesAnnotations {

  implicit val versionJsonCbs = CanBeSerialized[DocumentInfoJson]

  val versionRoutes = pathPrefix("docs") {
    pathEndOrSingleSlash {
      getVersion
    }
  }

  def getVersion: Route =
    complete {
      DocumentInfoJson(buildSha.substring(0, 6), buildDate)
    }

  def isMdFile(file: Path): Boolean = file.getFileName.toString.toLowerCase.endsWith(".md")

  def allMdFiles(rootFolder: String): Either[Throwable, Seq[Path]] = {

    var files = Seq[Path]()

    try {
      Files.walkFileTree(Paths.get(rootFolder), (f: Path) => {

        FileVisitResult.CONTINUE;
      })
    } catch {
      case t: Throwable => return Left(t)
    }

    Right(files)
  }
}

@Api(
  value = "Documents",
  produces = "application/json",
  consumes = "application/json"
)
@Path("api/docs")
trait DocumentsRoutesAnnotations {

  @ApiOperation(
    httpMethod = "GET",
    response = classOf[DocumentInfoJson],
    value = "Returns an object which describes running version"
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 500, message = "Internal Server Error"),
      new ApiResponse(code = 200, message = "OK", response = classOf[DocumentInfoJson])
    )
  )
  @Path("/")
  def getVersion: Route
}

@ApiModel(description = "Metadata about a document")
case class DocumentInfoJson(
    @(ApiModelProperty @field)(value = "Document name") name: String,
    @(ApiModelProperty @field)(value = "Document path") path: String
)
