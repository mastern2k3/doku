package com.kaizoku.doku.documents.plugins

import java.time.OffsetDateTime

import scala.concurrent.Future
import io.circe.Json

import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.documents._

class HashtagPlugin extends DocumentPlugin {

  def uniqueName = "hashtag"

  def process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[PluginMetadata],
      mention: Option[PluginMention]
  ): Future[Option[PluginMetadata]] =
    Future.successful(metadata)
}
