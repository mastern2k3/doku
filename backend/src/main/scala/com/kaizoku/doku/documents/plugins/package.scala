package com.kaizoku.doku.documents

import io.circe.JsonObject

package object plugins {
  type PluginName     = String
  type PluginMetadata = JsonObject
}
