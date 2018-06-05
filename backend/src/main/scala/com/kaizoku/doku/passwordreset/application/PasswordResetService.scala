package com.kaizoku.doku.passwordreset.application

import java.time.Instant

import com.kaizoku.doku.common.crypto.{PasswordHashing, Salt}
import com.kaizoku.doku.common.Utils
import com.kaizoku.doku.email.application.{EmailService, EmailTemplatingEngine}
import com.kaizoku.doku.email.domain.EmailContentWithSubject
import com.kaizoku.doku.passwordreset.domain.PasswordResetCode
import com.kaizoku.doku.user.application.UserDao
import com.kaizoku.doku.user.domain.User
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

class PasswordResetService(
    userDao: UserDao,
    codeDao: PasswordResetCodeDao,
    emailService: EmailService,
    emailTemplatingEngine: EmailTemplatingEngine,
    config: PasswordResetConfig,
    passwordHashing: PasswordHashing
)(implicit ec: ExecutionContext)
    extends StrictLogging {

  def sendResetCodeToUser(login: String): Future[Unit] = {
    logger.debug(s"Preparing to generate and send reset code to user $login")
    val userFut = userDao.findByLoginOrEmail(login)
    userFut.flatMap {
      case Some(user) =>
        logger.debug("User found")
        val code = randomPass(user)
        storeCode(code).flatMap(_ => sendCode(code))
      case None =>
        logger.debug(s"User not found: $login")
        Future.successful((): Unit)
    }
  }

  private def randomPass(user: User): PasswordResetCode = PasswordResetCode(Utils.randomString(32), user)

  private def storeCode(code: PasswordResetCode): Future[Unit] = {
    logger.debug(s"Storing reset code for user ${code.user.login}")
    codeDao.add(code)
  }

  private def sendCode(code: PasswordResetCode): Future[Unit] = {
    logger.debug(s"Scheduling e-mail with reset code for user ${code.user.login}")
    emailService.scheduleEmail(code.user.email, prepareResetEmail(code.user, code))
  }

  private def prepareResetEmail(user: User, code: PasswordResetCode): EmailContentWithSubject = {
    val resetLink = String.format(config.resetLinkPattern, code.code)
    emailTemplatingEngine.passwordReset(user.login, resetLink)
  }

  def performPasswordReset(code: String, newPassword: String): Future[Either[String, Boolean]] = {
    logger.debug("Performing password reset")
    codeDao.findByCode(code).flatMap {
      case Some(c) =>
        if (c.validTo.toInstant.isAfter(Instant.now())) {
          for {
            _ <- changePassword(c, newPassword)
            _ <- invalidateResetCode(c)
          } yield Right(true)
        } else {
          invalidateResetCode(c).map(_ => Left("Your reset code is invalid. Please try again."))
        }
      case None =>
        logger.debug("Reset code not found")
        Future.successful(Left("Your reset code is invalid. Please try again."))
    }
  }

  private def changePassword(code: PasswordResetCode, newPassword: String): Future[Unit] = {
    val salt = Salt.newSalt()
    userDao.changePassword(code.user.id, passwordHashing.hashPassword(newPassword, salt), salt)
  }

  private def invalidateResetCode(code: PasswordResetCode): Future[Unit] =
    codeDao.remove(code)
}
