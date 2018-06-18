package com.kaizoku.doku.documents.plugins

import scala.concurrent.Future
import org.h2.mvstore._

import com.kaizoku.doku.documents._

class HashtagPlugin extends TypedDocumentPlugin[MapData] {

  def uniqueName = "hashtag"

  val jew = MVStore.open("koklol")

  def process(doc: DocumentDetails, body: DocumentBody, metadata: Option[MapData]): Future[Option[MapData]] =
    Future.successful(metadata)
}
