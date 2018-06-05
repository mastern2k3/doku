package com.kaizoku.doku.passwordreset.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.kaizoku.doku.passwordreset.application.{
  PasswordResetCodeDao,
  PasswordResetConfig,
  PasswordResetService
}
import com.kaizoku.doku.passwordreset.domain.PasswordResetCode
import com.kaizoku.doku.test.{BaseRoutesSpec, TestHelpersWithDb}
import com.kaizoku.doku.user.domain.User
import com.typesafe.config.ConfigFactory

class PasswordResetRoutesSpec extends BaseRoutesSpec with TestHelpersWithDb { spec =>

  lazy val config = new PasswordResetConfig {
    override def rootConfig = ConfigFactory.load()
  }
  val passwordResetCodeDao = new PasswordResetCodeDao(sqlDatabase)
  val passwordResetService =
    new PasswordResetService(
      userDao,
      passwordResetCodeDao,
      emailService,
      emailTemplatingEngine,
      config,
      passwordHashing
    )

  val routes = Route.seal(new PasswordResetRoutes with TestRoutesSupport {
    override val userService          = spec.userService
    override val passwordResetService = spec.passwordResetService
  }.passwordResetRoutes)

  "POST /" should "send e-mail to user" in {
    // given
    val user = newRandomStoredUser()

    // when
    Post("/passwordreset", Map("login" -> user.login)) ~> routes ~> check {
      emailService.wasEmailSentTo(user.email) should be(true)
    }
  }

  "POST /[code] with password" should "change the password" in {
    // given
    val user = newRandomStoredUser()
    val code = PasswordResetCode(randomString(), user)
    passwordResetCodeDao.add(code).futureValue

    val newPassword = randomString()

    // when
    Post(s"/passwordreset/${code.code}", Map("password" -> newPassword)) ~> routes ~> check {
      responseAs[String] should be("ok")
      val updatedUser = userDao.findById(user.id).futureValue.get
      passwordHashing.verifyPassword(updatedUser.password, newPassword, updatedUser.salt) should be(true)
    }
  }

  "POST /[code] without password" should "result in an error" in {
    // given
    val user = newRandomStoredUser()
    val code = PasswordResetCode(randomString(), user)
    passwordResetCodeDao.add(code).futureValue

    // when
    Post("/passwordreset/123") ~> routes ~> check {
      status should be(StatusCodes.BadRequest)
    }
  }

  "POST /[code] with password but with invalid code" should "result in an error" in {
    // given
    val user = newRandomStoredUser()
    val code = PasswordResetCode(randomString(), user)
    passwordResetCodeDao.add(code).futureValue

    val newPassword = randomString()

    // when
    Post("/passwordreset/123", Map("password" -> newPassword)) ~> routes ~> check {
      status should be(StatusCodes.Forbidden)
      val updatedUser = userDao.findById(user.id).futureValue.get
      passwordHashing.verifyPassword(updatedUser.password, newPassword, updatedUser.salt) should be(false)
    }
  }
}
