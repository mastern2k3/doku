package com.kaizoku.doku.documents.providers

import java.io.{File, FileOutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute._
import java.security.MessageDigest
import java.util.regex.Pattern
import java.util.UUID
import java.time.OffsetDateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.kaizoku.doku.common.sql.SqlDatabase
import com.typesafe.scalalogging.StrictLogging

import com.kaizoku.doku.documents._
import com.kaizoku.doku.common.FutureHelpers._

case class InternalDocument(
    override val id: DocumentId,
    body: String,
    updatedOn: OffsetDateTime
) extends DocumentDetails {
  override val name: String = "Internal document " + id
}

trait SqlInternalDocumentSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val internalDocuments = TableQuery[InternalDocuments]

  protected class InternalDocuments(tag: Tag) extends Table[InternalDocument](tag, "internal_docs") {

    def id        = column[String]("id", O.PrimaryKey)
    def body      = column[String]("body")
    def updatedOn = column[OffsetDateTime]("updated_on")

    def * = (id, body, updatedOn) <> ((InternalDocument.apply _).tupled, InternalDocument.unapply)
  }
}

class InternalDocumentDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
    extends SqlInternalDocumentSchema
    with StrictLogging {
  import database._
  import database.driver.api._

  private def findOneWhere(condition: InternalDocuments => Rep[Boolean]) =
    db.run(internalDocuments.filter(condition).result.headOption)

  def findById(docId: DocumentId): Future[Option[InternalDocument]] = findOneWhere(_.id === docId)

  def all = db.run(internalDocuments.result)

  def save(docId: DocumentId, body: DocumentBody): Future[Unit] = {
    val newData = InternalDocument(docId, body, OffsetDateTime.now)

    db.run(internalDocuments.insertOrUpdate(newData)).mapToUnit
  }
}

class InternalDocumentProvider(dao: InternalDocumentDao)(implicit ec: ExecutionContext) extends DocumentProvider {

  private def readyDao =
    dao.all
      .map(_.isEmpty)
      .flatMap(_ match {
        case true =>
          dao.save(
            Base64Url.encode(UUID.randomUUID()),
            ". #system_config #pin\n\n# Welcome to Doku\n\n* fuck off please\n"
          )
        case false => Future.unit
      })
      .map(u => dao)

  def uniqueName = "internal"

  def status =
    readyDao.value match {
      case Some(Success(_)) => OkStatus()
      case Some(Failure(e)) => ErrorStatus(e)
      case None             => InitStatus()
    }

  def get(docId: DocumentId) = readyDao.flatMap(_.findById(docId))

  def getAll: Future[List[DocumentDetails]] = readyDao.flatMap(_.all.map(_.toList))

  def getBody(docId: DocumentId): Future[Option[DocumentBody]] = readyDao.flatMap(_.findById(docId).map(_.map(_.body)))

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit] =
    Future.failed[Unit](new Exception("not implemented"))

  def createNew(name: String): Future[DocumentDetails] =
    Future.failed[DocumentDetails](new Exception("not implemented"))
}
