package com.kaizoku.doku.documents.plugins

import java.time.OffsetDateTime

import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Json, JsonObject}
import scala.util.{Failure, Success, Try}

import com.kaizoku.doku.common.FutureHelpers._
import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.documents._

case class PluginMention(name: PluginName, rest: String)

trait DocumentPlugin {
  def uniqueName: PluginName
  def dependencyPlugins: List[PluginName] = List()
  def process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[PluginMetadata],
      mention: Option[PluginMention]
  ): Future[Option[PluginMetadata]]
}

class PluginService(metadataDao: PluginMetadataDao, plugins: List[DocumentPlugin])(implicit val ec: ExecutionContext)
    extends StrictLogging {

  /*, pluginConfig: Array[PluginConfiguration]*/
  import io.circe.syntax._
  import io.circe.jawn.decode

  val mention = raw"(?m)^@([a-zA-Z_]+)(.*)$$".r

  case class PluginConfiguration(isImplicit: Boolean)

  val pluginSet = plugins.map(p => (p, configOf(p.uniqueName)))

  private def configOf(name: PluginName): PluginConfiguration =
    if (name.equals("hashtags")) PluginConfiguration(true) else PluginConfiguration(false)

  private def getMentions(body: DocumentBody): Iterator[PluginMention] =
    for (m <- mention.findAllMatchIn(body)) yield PluginMention(m.group(1), m.group(2))

  private def jsonFromMetadata(metadata: PluginMetadataRecord): JsonObject =
    decode[JsonObject](metadata.metadata) match {
      case Right(jObj) => jObj
      case Left(err) => {
        logger.error(err.getMessage)
        err.printStackTrace()
        JsonObject.empty
      }
    }

  private def _process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[PluginMetadata]
  ): Future[PluginMetadata] = {
    val mentions = getMentions(body).toArray

    Future
      .sequence(
        pluginSet
          .map(p => (p._1, p._2, mentions.find(_.name == p._1.uniqueName)))
          .filter(p => p._2.isImplicit || !p._3.isEmpty)
          .map(
            p =>
              p._1
                .process(
                  doc,
                  body,
                  metadata.flatMap(_.apply(p._1.uniqueName)).map(_.asObject.getOrElse(JsonObject.empty)),
                  p._3
                )
                .map(_.map((p._1.uniqueName, _)))
          )
      )
      .map(_.flatten.map(p => (p._1, Json.fromJsonObject(p._2))))
      .map(JsonObject.fromIterable)
      .andThen({
        case Success(v) => metadataDao.save(doc.id, v)
        case Failure(e) => logger.error(e.toString)
      })
  }

  def get(docId: DocumentId): Future[Option[PluginMetadata]] =
    metadataDao.findById(docId).map(_.map(jsonFromMetadata))

  def process(doc: DocumentDetails, body: DocumentBody): Future[PluginMetadata] =
    get(doc.id).flatMap(_process(doc, body, _))
}
