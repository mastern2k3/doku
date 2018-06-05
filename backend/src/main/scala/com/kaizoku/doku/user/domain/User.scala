package com.kaizoku.doku.user.domain

import java.time.OffsetDateTime
import java.util.UUID

import com.kaizoku.doku.common.crypto.PasswordHashing
import com.kaizoku.doku.user._

case class User(
    id: UserId,
    login: String,
    loginLowerCased: String,
    email: String,
    password: String,
    salt: String,
    createdOn: OffsetDateTime
)

object User {
  def withRandomUUID(
      login: String,
      email: String,
      plainPassword: String,
      salt: String,
      createdOn: OffsetDateTime,
      passwordHashing: PasswordHashing
  ) =
    User(
      UUID.randomUUID(),
      login,
      login.toLowerCase,
      email,
      passwordHashing.hashPassword(plainPassword, salt),
      salt,
      createdOn
    )
}

case class BasicUserData(id: UserId, login: String, email: String, createdOn: OffsetDateTime)

object BasicUserData {
  def fromUser(user: User) = new BasicUserData(user.id, user.login, user.email, user.createdOn)
}
