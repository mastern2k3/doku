package com.kaizoku.doku.email.application

import com.kaizoku.doku.email.domain.EmailContentWithSubject

import scala.concurrent.Future

trait EmailService {

  def scheduleEmail(address: String, emailData: EmailContentWithSubject): Future[Unit]

}
