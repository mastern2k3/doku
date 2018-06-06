package com.kaizoku.doku.documents

import javax.ws.rs.{Path => JPath}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.kaizoku.doku.common.api.RoutesSupport
import com.kaizoku.doku.version.BuildInfo._
import io.circe.generic.auto._
import io.swagger.annotations.{ApiResponse, _}
import java.nio.file._
import java.nio.file.attribute._
import scala.io.Source._

import scala.annotation.meta.field

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

  implicit val documentJsonCbs = CanBeSerialized[Array[DocumentInfoJson]]

  val allFiles = getAllFiles("C:\\Users\\Nitzanz\\Dropbox\\Projects")

  val documentsRoutes = pathPrefix("docs") {
    pathEndOrSingleSlash {
      getAllDocs
    }
  }

  def getAllDocs: Route = allFiles match {
    case Left(msg) => complete(StatusCodes.Forbidden, msg.getMessage)
    case Right(docs) =>
      complete(
        docs
          .filter(isMdFile)
          .map(p => DocumentInfoJson(p.toString(), p.getFileName().toString()))
          .toArray[DocumentInfoJson]
      )
  }

  def isMdFile(file: Path): Boolean = file.getFileName.toString.toLowerCase.endsWith(".md")

  def getAllFiles(rootFolder: String): Either[Throwable, Seq[Path]] =
    try {
      return Right(new TraversePath(Paths.get(rootFolder)).map(_._1).toSeq)
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
    response = classOf[Array[DocumentInfoJson]],
    value = "Returns an object which describes running version"
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 500, message = "Internal Server Error"),
      new ApiResponse(code = 200, message = "OK", response = classOf[Array[DocumentInfoJson]])
    )
  )
  @JPath("/")
  def getVersion: Route
}

@ApiModel(description = "Metadata about a document")
case class DocumentInfoJson(
    @(ApiModelProperty @field)(value = "Document name") name: String,
    @(ApiModelProperty @field)(value = "Document path") path: String
)
