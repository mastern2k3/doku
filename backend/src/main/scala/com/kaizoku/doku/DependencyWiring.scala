package com.kaizoku.doku

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import com.kaizoku.doku.common.crypto.{Argon2dPasswordHashing, CryptoConfig, PasswordHashing}
import com.kaizoku.doku.common.sql.{DatabaseConfig, SqlDatabase}
import com.kaizoku.doku.email.application.{DummyEmailService, EmailConfig, EmailTemplatingEngine, SmtpEmailService}
import com.kaizoku.doku.passwordreset.application.{PasswordResetCodeDao, PasswordResetConfig, PasswordResetService}
import com.kaizoku.doku.user.application.{RefreshTokenStorageImpl, RememberMeTokenDao, UserDao, UserService}
import com.kaizoku.doku.documents.plugins.{PluginMetadataDao, PluginService}
import com.kaizoku.doku.documents.plugins.impl.{HashtagPlugin}
import com.kaizoku.doku.documents.DocumentService
import com.kaizoku.doku.documents.providers.{LocalDocumentProvider}

trait DependencyWiring extends StrictLogging {

  def system: ActorSystem

  lazy val config = new PasswordResetConfig with EmailConfig with DatabaseConfig with ServerConfig with CryptoConfig {
    override def rootConfig = ConfigFactory.load()
  }

  lazy val passwordHashing: PasswordHashing = new Argon2dPasswordHashing(config)

  lazy val daoExecutionContext = system.dispatchers.lookup("dao-dispatcher")

  lazy val userDao = new UserDao(sqlDatabase)(daoExecutionContext)

  lazy val codeDao = new PasswordResetCodeDao(sqlDatabase)(daoExecutionContext)

  lazy val rememberMeTokenDao = new RememberMeTokenDao(sqlDatabase)(daoExecutionContext)

  lazy val pluginDao = new PluginMetadataDao(sqlDatabase)(daoExecutionContext)

  lazy val sqlDatabase = SqlDatabase.create(config)

  lazy val serviceExecutionContext = system.dispatchers.lookup("service-dispatcher")

  lazy val emailService = if (config.emailEnabled) {
    new SmtpEmailService(config)(serviceExecutionContext)
  } else {
    logger.info("Starting with fake email sending service. No emails will be sent.")
    new DummyEmailService
  }

  lazy val emailTemplatingEngine = new EmailTemplatingEngine

  lazy val userService = new UserService(
    userDao,
    emailService,
    emailTemplatingEngine,
    passwordHashing
  )(serviceExecutionContext)

  lazy val passwordResetService = new PasswordResetService(
    userDao,
    codeDao,
    emailService,
    emailTemplatingEngine,
    config,
    passwordHashing
  )(serviceExecutionContext)

  lazy val plugins = List(
    new HashtagPlugin
  )

  lazy val pluginService = new PluginService(pluginDao, plugins)(serviceExecutionContext)

  lazy val documentProviders =
    List(
      new LocalDocumentProvider(config.localProviderFolder)(
        serviceExecutionContext
      )
    )

  lazy val documentService = new DocumentService(documentProviders, pluginService)(serviceExecutionContext)

  lazy val refreshTokenStorage = new RefreshTokenStorageImpl(rememberMeTokenDao, system)(serviceExecutionContext)
}
