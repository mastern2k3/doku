package com.kaizoku.doku.documents.plugins

import java.time.OffsetDateTime

import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.Future
import io.circe.{Json, JsonObject}

import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.documents._

class HashtagPlugin extends DocumentPlugin with StrictLogging {

  def uniqueName = "hashtag"

  def process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[PluginMetadata],
      mention: Option[PluginMention]
  ): Future[Option[PluginMetadata]] =
    Future.successful(
      Some(
        metadata
          .getOrElse(JsonObject.empty)
          .add("tags", Json.arr(Json.fromString("lol"), Json.fromString("top"), Json.fromString("pin")))
      )
    )
}
