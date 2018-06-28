package com.kaizoku.doku.plugins.impl

import java.time.OffsetDateTime

import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.Future
import io.circe.{Json, JsonObject}

import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.documents._
import com.kaizoku.doku.plugins._

class HashtagPlugin extends DocumentPlugin with StrictLogging {

  val hashtag = raw"(?m)[^\n\S#]#([a-zA-Z_-]+)".r

  def uniqueName = "hashtags"

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
          .add(
            "tags",
            Json.fromValues(
              hashtag
                .findAllMatchIn(body)
                .map(m => Json.fromString(m.group(1).toLowerCase))
                .toIterable
            )
          )
      )
    )
}
