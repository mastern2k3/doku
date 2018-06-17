package com.kaizoku.doku.documents

import java.util.UUID
import java.util.regex.Pattern
import java.nio.file._
import java.nio.file.attribute._
import java.nio.charset.StandardCharsets
import javax.ws.rs.{Path => JPath}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.kaizoku.doku.documents.sources.Base64Url

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

object DocumentDetails {
  type DocumentId   = String
  type DocumentBody = String
}

import DocumentDetails.DocumentId
import DocumentDetails.DocumentBody

trait DocumentDetails {
  def id: DocumentId
  def name: String
}

abstract class DocumentProviderStatus

case class OkStatus()                    extends DocumentProviderStatus
case class ErrorStatus(error: Throwable) extends DocumentProviderStatus
case class InitStatus()                  extends DocumentProviderStatus

trait DocumentProvider {

  def uniqueName: String

  def status: DocumentProviderStatus

  def get(docId: DocumentId): Future[DocumentDetails]

  def getAll: Future[List[DocumentDetails]]

  def getBody(docId: DocumentId): Future[DocumentBody]

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit]
}

trait DocumentService {

  def get(docId: DocumentId): Future[DocumentDetails]

  def getAll: Future[List[DocumentDetails]]

  def getBody(docId: DocumentId): Future[DocumentBody]

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit]
}

case class LocalFileDocumentDetails(
    override val id: DocumentId,
    override val name: String,
    val localPath: Path
) extends DocumentDetails

class LocalDirectoryDocumentProvider(rootPath: String)(implicit ec: ExecutionContext)
    extends DocumentService
    with DocumentProvider {

  def uniqueName = "local"

  def status =
    allFiles.value match {
      case Some(Success(_)) => OkStatus()
      case Some(Failure(e)) => ErrorStatus(e)
      case None             => InitStatus()
    }

  def getAllFiles(rootFolder: String) = Future(new TraversePath(Paths.get(rootFolder)).map(_._1).toList)

  val allFiles = getAllFiles(rootPath)

  val allDocuments = allFiles.map(_.filter(isMdFile).map(pathToDocumentDetails).toList)

  def wholeFile(path: Path): Future[String] =
    Future(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))

  def glue2[A](e: Option[A]): Future[A] =
    e match {
      case Some(item) => Future { item }
      case None       => Future.failed(new Exception("Entity not found"))
    }

  def op2ei[A](e: Option[A]): Try[A] =
    e match {
      case Some(item) => Success(item)
      case None       => Failure(new Exception("Entity not found"))
    }

  def pathToDocumentDetails(path: Path) = {

    val relPath = path.getParent.toString.replaceFirst("^" + Pattern.quote(rootPath) + "\\\\", "").replace("\\", "/");

    LocalFileDocumentDetails(
      Base64Url.encode(UUID.randomUUID),
      (relPath.split("/") :+ (path.getFileName.toString)).mkString("."),
      path
    )
  }

  def isMdFile(file: Path): Boolean = file.getFileName.toString.toLowerCase.endsWith(".md")

  def internalGet(docId: DocumentId) = allDocuments.map(_.find(_.id == docId)).flatMap(glue2)

  def get(docId: DocumentId) = internalGet(docId)

  def getAll: Future[List[DocumentDetails]] = allDocuments

  def getBody(docId: DocumentId): Future[DocumentBody] = internalGet(docId).flatMap(lf => wholeFile(lf.localPath))

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit] =
    internalGet(docId).flatMap(
      details => Future { Files.write(details.localPath, newBody.getBytes(StandardCharsets.UTF_8)) }
    )
}
