package com.kaizoku.doku.documents.plugins

import java.time.OffsetDateTime

import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.scalalogging.StrictLogging
import io.circe.JsonObject

import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.documents._

case class PluginMention(name: PluginName, rest: String)

trait DocumentPlugin {
  def uniqueName: PluginName
  def dependencyPlugins: Array[PluginName] = Array()
  def process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[PluginMetadata],
      mention: Option[PluginMention]
  ): Future[Option[PluginMetadata]]
}

case class PluginMetadataEntry(id: DocumentId, metadata: String, updatedOn: OffsetDateTime)

trait SqlPluginSchema {

  protected val database: SqlDatabase

  import database._
  import database.driver.api._

  protected val pluginMetadata = TableQuery[PluginMetadata]

  protected class PluginMetadata(tag: Tag) extends Table[PluginMetadataEntry](tag, "plugin_metadata") {

    def id        = column[String]("id", O.PrimaryKey)
    def metadata  = column[String]("metadata", O.SqlType("CLOB"))
    def updatedOn = column[OffsetDateTime]("updated_on")

    def * = (id, metadata, updatedOn) <> ((PluginMetadataEntry.apply _).tupled, PluginMetadataEntry.unapply)
  }
}

class PluginMetadataDao(protected val database: SqlDatabase)(implicit val ec: ExecutionContext)
    extends SqlPluginSchema {

  import database._
  import database.driver.api._

  private def findOneWhere(condition: PluginMetadata => Rep[Boolean]) =
    db.run(pluginMetadata.filter(condition).result.headOption)

  def findById(docId: DocumentId): Future[Option[PluginMetadataEntry]] = findOneWhere(_.id === docId)
}

class PluginService(
    metadataDao: PluginMetadataDao,
    plugins: Array[DocumentPlugin]
)(implicit val ec: ExecutionContext)
    extends StrictLogging {

  /*, pluginConfig: Array[PluginConfiguration]*/
  import io.circe.syntax._

  val mention = raw"^@([a-zA-Z_]+)(.*)$$/gm".r

  case class PluginConfiguration(isImplicit: Boolean)

  val pluginMap = plugins.iterator.map(p => p.uniqueName -> (p, configOf(p.uniqueName))).toMap
  val pluginSet = plugins.map(p => (p, configOf(p.uniqueName)))

  private def configOf(name: PluginName): PluginConfiguration =
    if (name == "hashtag") PluginConfiguration(true) else PluginConfiguration(false)

  private def getMentions(body: DocumentBody): Iterator[PluginMention] =
    for (m <- mention.findAllMatchIn(body)) yield PluginMention(m.group(1), m.group(2))

  private def jsonFromMetadata(metadata: PluginMetadataEntry): JsonObject =
    metadata.metadata.asJson.asObject.getOrElse(JsonObject.empty)

  def get(docId: DocumentId): Future[Option[PluginMetadata]] =
    metadataDao.findById(docId).map(_.map(jsonFromMetadata))

  def process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[PluginMetadata]
  ): Future[Option[PluginMetadata]] = {
    val mentions = getMentions(body).toArray

    pluginSet.iterator
      .map(p => (p._1, p._2, mentions.find(_.name == p._1.uniqueName)))
      .filter(p => p._2.isImplicit || !p._3.isEmpty)
      .foreach(plugin => {
        plugin._1.process(doc, body, metadata.map(_.apply(plugin._1.uniqueName)), plugin._3)
      })
    //  foreach plugin => {
    //   metadata = plugin._1.process()
    // }
    /*
doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[Json],
      mention: Option[PluginMention]
     */
    //Future.successful(metadata)
    // .filter(p => p._2.isImplicit || mentions.exists(_.name == p._1)) foreach plugin => {
    //   plugin
    // }

    // metadata
    // getMentions(body).map(_.name).map(pluginMap.get).reduce(_ compose _)
    // val newMetadata = metadataDao.findById(doc.id).map(_.map(jsonFromMetadata))
  }
}
