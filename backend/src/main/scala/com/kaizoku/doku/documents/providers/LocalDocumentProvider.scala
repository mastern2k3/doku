package com.kaizoku.doku.documents.providers

import java.io.{File, FileOutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute._
import java.security.MessageDigest
import java.util.regex.Pattern
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.kaizoku.doku.documents._

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

case class LocalFileDocumentDetails(
    override val id: DocumentId,
    override val name: String,
    val localPath: Path
) extends DocumentDetails

class LocalDocumentProvider(rootPath: String)(implicit ec: ExecutionContext) extends DocumentProvider {

  private def getAllFiles(rootFolder: String) = Future(new TraversePath(Paths.get(rootFolder)).map(_._1).toList)

  private val allFiles = getAllFiles(rootPath)

  private var allDocuments = allFiles.map(_.filter(isMdFile).map(pathToDocumentDetails).toList)

  private def wholeFile(path: Path): Future[String] =
    Future(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))

  private def of2fo[T](o: Option[Future[T]]): Future[Option[T]] =
    o.map(_.map(Some(_))).getOrElse(Future.successful(None))

  private def md5(s: String) = MessageDigest.getInstance("MD5").digest(s.getBytes)

  private def pathToDocumentDetails(path: Path) = {

    val relPath = path.getParent.toString.replaceFirst("^" + Pattern.quote(rootPath), "").replace("\\", "/");

    val hintName = (relPath.split("/").filter(!_.isEmpty) :+ (path.getFileName.toString)).mkString(".")

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
              _ <- Future(Files.write(details.localPath, newBody.getBytes(StandardCharsets.UTF_8)))
            } yield Unit
        )
      )
      .map(t => Unit)

  def createNew(name: String): Future[DocumentDetails] = {

    val f =
      new File(Paths.get(rootPath).toFile, name + "." + Base64Url.encode(UUID.randomUUID()).substring(0, 5) + ".md")

    new FileOutputStream(f).close()

    val n = pathToDocumentDetails(f.toPath)

    allDocuments = allDocuments.map(f => n :: f)

    Future(n)
  }
}
