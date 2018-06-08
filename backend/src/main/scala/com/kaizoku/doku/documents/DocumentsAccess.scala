package com.kaizoku.doku.documents

import java.util.UUID
import java.util.regex.Pattern
import java.nio.file._
import java.nio.file.attribute._
import java.nio.charset.StandardCharsets
import javax.ws.rs.{Path => JPath}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
  def path: String
}

trait DocumentProvider {

  def get(docId: DocumentId): Future[DocumentDetails]
  def getAll: Future[List[DocumentDetails]]
  def getBody(docId: DocumentId): Future[DocumentBody]
}

case class LocalFileDocumentDetails(
    override val id: DocumentId,
    override val name: String,
    override val path: String,
    val localPath: Path
) extends DocumentDetails

class LocalDirectoryDocumentProvider(rootPath: String) extends DocumentProvider {

  val allFiles = getAllFiles(rootPath)

  val allDocuments = allFiles.map(_.filter(isMdFile).map(pathToDocumentDetails).toList)

  def wholeFile(path: Path): Either[Throwable, String] =
    try {
      Right(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    } catch {
      case t: Throwable => Left(t)
    }

  def glue[A](e: Either[Throwable, A]): Future[A] =
    e match {
      case Right(item) => Future { item }
      case Left(th)    => Future.failed(th)
    }

  def glue2[A](e: Option[A]): Future[A] =
    e match {
      case Some(item) => Future { item }
      case None       => Future.failed(new Exception("Entity not found"))
    }

  def op2ei[A](e: Option[A]): Either[Throwable, A] =
    e match {
      case Some(item) => Right(item)
      case None       => Left(new Exception("Entity not found"))
    }

  def pathToDocumentDetails(path: Path) = {

    val relPath = path.toString.replaceFirst("^" + Pattern.quote(rootPath), "").replace("\\", "/");

    LocalFileDocumentDetails(UUID.randomUUID.toString, path.getFileName.toString, relPath, path)
  }

  def isMdFile(file: Path): Boolean = file.getFileName.toString.toLowerCase.endsWith(".md")

  def getAllFiles(rootFolder: String): Either[Throwable, List[Path]] =
    try {
      Right(new TraversePath(Paths.get(rootFolder)).map(_._1).toList)
    } catch {
      case t: Throwable => Left(t)
    }

  def get(docId: DocumentId): Future[DocumentDetails] =
    glue(allDocuments.map(_.find(_.id == docId))).flatMap(glue2)

  def getAll: Future[List[DocumentDetails]] =
    glue(allDocuments)

  def getBody(docId: DocumentId): Future[DocumentBody] =
    glue(allDocuments.map(_.find(_.id == docId).map(lf => wholeFile(lf.localPath))).flatMap(op2ei).joinRight)
}
