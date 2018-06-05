package com.kaizoku.doku.email.application

import com.kaizoku.doku.email.domain.EmailContentWithSubject
import org.scalatest.{FlatSpec, Matchers}

class DummyEmailSendingServiceSpec extends FlatSpec with Matchers {
  it should "send scheduled email" in {
    val service = new DummyEmailService
    service.scheduleEmail("test@sml.com", new EmailContentWithSubject("content", "subject"))
    service.wasEmailSent("test@sml.com", "subject") should be(true)
  }
}
