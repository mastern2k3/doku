package com.kaizoku.doku.common.crypto

import com.kaizoku.doku.common.ConfigWithDefault
import com.typesafe.config.Config

trait CryptoConfig extends ConfigWithDefault {
  def rootConfig: Config

  lazy val iterations  = getInt("doku.crypto.argon2.iterations", 2)
  lazy val memory      = getInt("doku.crypto.argon2.memory", 16383)
  lazy val parallelism = getInt("doku.crypto.argon2.parallelism", 4)
}
