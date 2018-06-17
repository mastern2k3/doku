package com.kaizoku.doku.documents.plugins

import scala.concurrent.Future

import com.kaizoku.doku.documents._

abstract class DocumentMetadataObject

case class StringData(string: String)                         extends DocumentMetadataObject
case class ArrayData(array: Array[DocumentMetadataObject])    extends DocumentMetadataObject
case class MapData(data: Map[String, DocumentMetadataObject]) extends DocumentMetadataObject

trait DocumentMetadata {
  def metadata: Map[PluginName, DocumentMetadataObject]
}

trait DocumentPlugin {
  def uniqueName: PluginName
  def dependencyPlugins: Array[PluginName] = Array()
  def process(
      doc: DocumentDetails,
      body: DocumentBody,
      metadata: Option[DocumentMetadataObject]
  ): DocumentMetadataObject
}

trait TypedDocumentPlugin[D <: DocumentMetadataObject] {
  def uniqueName: PluginName
  def dependencyPlugins: Array[PluginName] = Array()
  def process(doc: DocumentDetails, body: DocumentBody, metadata: Option[D]): Future[Option[D]]
}
