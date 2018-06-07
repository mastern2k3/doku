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
import io.circe.generic.auto._
import io.swagger.annotations.{ApiResponse, _}

import com.kaizoku.doku.common.api.RoutesSupport
import com.kaizoku.doku.version.BuildInfo._

class TraversePath(path: Path) extends Traversable[(Path, BasicFileAttributes)] {

  def foreach[U](f: ((Path, BasicFileAttributes)) => U): Unit = {

    class Visitor extends SimpleFileVisitor[Path] {

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
        try {
          f(file, attrs)
          FileVisitResult.CONTINUE
        } catch {
          case _: Throwable => FileVisitResult.TERMINATE
        }
    }

    Files.walkFileTree(path, new Visitor)
  }
}

trait DocumentsRoutes extends RoutesSupport with DocumentsRoutesAnnotations {

  implicit val documentJsonCbs = CanBeSerialized[DocumentInfoJson]

  val docsRootFolder = "C:\\Users\\Nitzanz\\Dropbox\\Projects"

  val allFiles = getAllFiles(docsRootFolder)

  val documentsRoutes = pathPrefix("docs") {
    pathEndOrSingleSlash {
      allDocuments
    } ~
      pathPrefix(Segment.repeat(1, separator = Slash)) { str =>
        pathEndOrSingleSlash {
          complete(
            allFiles.map(_.filter(isMdFile).map(pathToDocumentJson).head)
          )
        } ~
          path("raw") {
            complete(wholeFile("C:\\Users\\Nitzanz\\Dropbox\\Projects\\036_doku\\tasks.md"))
          }
      }
  }

  def allDocuments = complete(allFiles.map(_.filter(isMdFile).map(pathToDocumentJson).toList))

  def wholeFile(path: String): Either[Throwable, String] =
    try {
      Right(new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8));
    } catch {
      case t: Throwable => Left(t)
    }

  def pathToDocumentJson(path: Path): DocumentInfoJson = {

    val relPath = path.toString.replaceFirst("^" + Pattern.quote(docsRootFolder), "").replace("\\", "/");

    DocumentInfoJson(UUID.randomUUID.toString, path.getFileName.toString, relPath)
  }

  def isMdFile(file: Path): Boolean = file.getFileName.toString.toLowerCase.endsWith(".md")

  def getAllFiles(rootFolder: String): Either[Throwable, Seq[Path]] =
    try {
      Right(new TraversePath(Paths.get(rootFolder)).map(_._1).toSeq)
    } catch {
      case t: Throwable => Left(t)
    }
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
    @(ApiModelProperty @field)(value = "Document path") path: String
)
