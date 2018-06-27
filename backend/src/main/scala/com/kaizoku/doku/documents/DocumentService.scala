package com.kaizoku.doku.documents

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.kaizoku.doku.common.FutureHelpers._
import com.kaizoku.doku.documents.plugins.PluginService

trait DocumentDetails {
  def id: DocumentId
  // Hint name
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

  def createNew(name: String): Future[DocumentDetails]
}

class DocumentService(
    docProviders: List[DocumentProvider],
    pluginService: PluginService
)(implicit ec: ExecutionContext) {

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

  def saveBody(docId: DocumentId, newBody: DocumentBody): Future[Unit] = {
    get(docId).flatMap(pluginService.process(_, newBody))
    docProviders.head.saveBody(docId, newBody)
  }

  def createNew(name: String): Future[DocumentDetails] =
    docProviders.head.createNew(name)
}
