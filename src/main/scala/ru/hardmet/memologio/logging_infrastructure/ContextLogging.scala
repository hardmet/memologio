package ru.hardmet.memologio
package logging_infrastructure

import cats.effect.{Clock, Sync}
import domain.RequestInfo
import tofu.HasContext
import tofu.generate.GenUUID
import tofu.logging.derivation.loggable.generate
import tofu.logging.{Loggable, LoggableContext, Logging, Logs}

import scala.reflect.ClassTag

class ContextLogging[F[_]: GenUUID: Sync: Clock] {
  implicit val context: LoggableContext[F] { type Ctx = RequestInfo } = new LoggableContext[F] {
    override type Ctx = RequestInfo

    override implicit def loggable: Loggable[RequestInfo] = generate[RequestInfo]

    override implicit def context: HasContext[F, RequestInfo] = RequestInfoBuilder[F]().initContext
  }

  def loggerForService[A: ClassTag]: F[Logging[F]] = Logs.withContext[F, F].forService[A]
}
