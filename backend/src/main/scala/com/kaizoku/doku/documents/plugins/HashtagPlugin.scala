package com.kaizoku.doku.documents.plugins

import scala.concurrent.Future

import com.kaizoku.doku.documents._

class HashtagPlugin extends TypedDocumentPlugin[MapData] {

  def uniqueName = "hashtag"

  def process(doc: DocumentDetails, body: DocumentBody, metadata: Option[MapData]): Future[Option[MapData]] =
    Future(metadata)
}
