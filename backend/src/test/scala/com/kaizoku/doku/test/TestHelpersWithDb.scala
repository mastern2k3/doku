package com.kaizoku.doku.test

import com.kaizoku.doku.common.sql.SqlDatabase
import com.kaizoku.doku.email.application.{DummyEmailService, EmailTemplatingEngine}
import com.kaizoku.doku.user.application.{UserDao, UserService}
import com.kaizoku.doku.user.domain.User
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext

trait TestHelpersWithDb extends TestHelpers with ScalaFutures {

  lazy val emailService          = new DummyEmailService()
  lazy val emailTemplatingEngine = new EmailTemplatingEngine
  lazy val userDao               = new UserDao(sqlDatabase)
  lazy val userService           = new UserService(userDao, emailService, emailTemplatingEngine, passwordHashing)

  def sqlDatabase: SqlDatabase

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def newRandomStoredUser(password: Option[String] = None): User = {
    val u = newRandomUser(password)
    userDao.add(u).futureValue
    u
  }
}
