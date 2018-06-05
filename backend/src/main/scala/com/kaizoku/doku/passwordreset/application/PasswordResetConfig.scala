package com.kaizoku.doku.passwordreset.application

import com.kaizoku.doku.common.ConfigWithDefault
import com.typesafe.config.Config

trait PasswordResetConfig extends ConfigWithDefault {
  def rootConfig: Config

  lazy val resetLinkPattern =
    getString("doku.reset-link-pattern", "http://localhost:8080/#/password-reset?code=%s")
}
