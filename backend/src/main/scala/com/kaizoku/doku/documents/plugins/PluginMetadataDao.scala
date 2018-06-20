package com.kaizoku.doku.documents.plugins

import java.time.OffsetDateTime

import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Json, JsonObject}

import com.kaizoku.doku.common.FutureHelpers._
import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.documents._

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
