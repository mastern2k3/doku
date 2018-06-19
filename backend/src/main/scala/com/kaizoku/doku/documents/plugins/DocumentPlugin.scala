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

case class PluginMetadataRecord(id: DocumentId, metadata: String, updatedOn: OffsetDateTime)

trait SqlPluginSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val pluginMetadata = TableQuery[PluginMetadataRecords]

  protected class PluginMetadataRecords(tag: Tag) extends Table[PluginMetadataRecord](tag, "plugin_metadata") {

    def id        = column[String]("id", O.PrimaryKey)
    def metadata  = column[String]("metadata")
    def updatedOn = column[OffsetDateTime]("updated_on")

    def * = (id, metadata, updatedOn) <> ((PluginMetadataRecord.apply _).tupled, PluginMetadataRecord.unapply)
  }
}

class PluginMetadataDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
    extends SqlPluginSchema
    with StrictLogging {

  import database._
  import database.driver.api._

  private def findOneWhere(condition: PluginMetadataRecords => Rep[Boolean]) =
    db.run(pluginMetadata.filter(condition).result.headOption)

  def findById(docId: DocumentId): Future[Option[PluginMetadataRecord]] = findOneWhere(_.id === docId)

  def save(docId: DocumentId, data: PluginMetadata): Future[Unit] = {
    val newData = PluginMetadataRecord(docId, Json.fromJsonObject(data).noSpaces, OffsetDateTime.now)

    db.run(pluginMetadata.insertOrUpdate(newData)).mapToUnit
  }
}

class PluginService(
    metadataDao: PluginMetadataDao,
    plugins: List[DocumentPlugin]
)(implicit val ec: ExecutionContext)
    extends StrictLogging {

  /*, pluginConfig: Array[PluginConfiguration]*/
  import io.circe.syntax._
  import io.circe.ObjectEncoder.objectEncoderContravariant

  val mention = raw"^@([a-zA-Z_]+)(.*)$$/gm".r

  case class PluginConfiguration(isImplicit: Boolean)

  // val pluginMap = plugins.iterator.map(p => p.uniqueName -> (p, configOf(p.uniqueName))).toMap
  val pluginSet = plugins.map(p => (p, configOf(p.uniqueName)))

  private def configOf(name: PluginName): PluginConfiguration =
    if (name.equals("hashtag")) PluginConfiguration(true) else PluginConfiguration(false)

  private def getMentions(body: DocumentBody): Iterator[PluginMention] =
    for (m <- mention.findAllMatchIn(body)) yield PluginMention(m.group(1), m.group(2))

  private def jsonFromMetadata(metadata: PluginMetadataRecord): JsonObject = {
    implicit val
    val d = metadata.metadata.asJsonObject.getOrElse(JsonObject.empty)

    logger.warn(metadata.metadata.asJson.toString)

    d
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
      .andThen({ case f => logger.info(f.toString) })
      .andThen(_ match {
        case Success(v) => metadataDao.save(doc.id, v)
        case Failure(e) => logger.error(e.toString)
      })
  }

  def get(docId: DocumentId): Future[Option[PluginMetadata]] =
    metadataDao.findById(docId).map(_.map(jsonFromMetadata))

  def process(doc: DocumentDetails, body: DocumentBody): Future[PluginMetadata] =
    get(doc.id).flatMap(_process(doc, body, _))
}
