package com.kaizoku.doku.documents

import java.util.UUID
import java.util.regex.Pattern
import java.nio.file._
import java.nio.file.attribute._
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.kaizoku.doku.documents.sources.Base64Url
import com.kaizoku.doku.documents.plugins.PluginService

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

  def get(docId: DocumentId): Future[Option[DocumentDetails]]

  def getAll: Future[List[DocumentDetails]]

  def getBody(docId: DocumentId): Future[Option[DocumentBody]]

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit]
}

class DocumentService(docProviders: List[DocumentProvider])(implicit ec: ExecutionContext) {

  private def failNotFound[A](e: Option[A]): Future[A] =
    e match {
      case Some(item) => Future.successful(item)
      case None       => Future.failed(new Exception("Entity not found"))
    }

  def get(docId: DocumentId): Future[DocumentDetails] =
    Future.find(docProviders.map(_.get(docId)))(d => !d.isEmpty).map(_.flatten).flatMap(failNotFound)

  def getAll: Future[List[DocumentDetails]] =
    Future.sequence(docProviders.map(_.getAll)).map(_.flatten)

  def getBody(docId: DocumentId): Future[DocumentBody] =
    Future.find(docProviders.map(_.getBody(docId)))(d => !d.isEmpty).map(_.flatten).flatMap(failNotFound)

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit] =
    Future.failed(new Exception("Fuck you"))
}

case class LocalFileDocumentDetails(
    override val id: DocumentId,
    override val name: String,
    val localPath: Path
) extends DocumentDetails

class LocalDirectoryDocumentProvider(
    rootPath: String,
    pluginService: PluginService
)(implicit ec: ExecutionContext)
    extends DocumentProvider {

  private def getAllFiles(rootFolder: String) = Future(new TraversePath(Paths.get(rootFolder)).map(_._1).toList)

  private val allFiles = getAllFiles(rootPath)

  private val allDocuments = allFiles.map(_.filter(isMdFile).map(pathToDocumentDetails).toList)

  private def wholeFile(path: Path): Future[String] =
    Future(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))

  private def op2ei[A](e: Option[A]): Try[A] =
    e match {
      case Some(item) => Success(item)
      case None       => Failure(new Exception("Entity not found"))
    }

  private def of2fo[T](o: Option[Future[T]]): Future[Option[T]] =
    o.map(_.map(Some(_))).getOrElse(Future.successful(None))

  private def md5(s: String) = MessageDigest.getInstance("MD5").digest(s.getBytes)

  private def pathToDocumentDetails(path: Path) = {

    val relPath = path.getParent.toString.replaceFirst("^" + Pattern.quote(rootPath) + "\\\\", "").replace("\\", "/");

    val hintName = (relPath.split("/") :+ (path.getFileName.toString)).mkString(".")

    LocalFileDocumentDetails(
      Base64Url.encode(md5(hintName)),
      hintName,
      path
    )
  }

  private def isMdFile(file: Path): Boolean = file.getFileName.toString.toLowerCase.endsWith(".md")

  private def internalGet(docId: DocumentId) = allDocuments.map(_.find(_.id == docId))

  def uniqueName = "local"

  def status =
    allFiles.value match {
      case Some(Success(_)) => OkStatus()
      case Some(Failure(e)) => ErrorStatus(e)
      case None             => InitStatus()
    }

  def get(docId: DocumentId) = internalGet(docId)

  def getAll: Future[List[DocumentDetails]] = allDocuments

  def getBody(docId: DocumentId): Future[Option[DocumentBody]] =
    internalGet(docId).map(_.map(lf => wholeFile(lf.localPath))).map(of2fo).flatten

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit] =
    internalGet(docId)
      .map(
        _.map(
          details =>
            for {
              _ <- pluginService.process(details, newBody)
              _ <- Future(Files.write(details.localPath, newBody.getBytes(StandardCharsets.UTF_8)))
            } yield Unit
        )
      )
      .map(t => Unit)
}
