package com.kaizoku.doku.common

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.StatusCodes

object FutureHelpers {
  implicit class PimpedFuture[T](future: Future[T])(implicit val ec: ExecutionContext) {
    def mapToUnit = future.map(_ => ())
    def mapToOk   = future.map(_ => StatusCodes.OK)
  }
}
