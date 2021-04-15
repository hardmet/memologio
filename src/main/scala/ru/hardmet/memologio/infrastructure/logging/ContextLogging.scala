package ru.hardmet.memologio
package infrastructure
package logging

import cats.effect.{Clock, Sync}
import ru.hardmet.memologio.infrastructure.logging.domain.ContextInfo
import tofu.HasContext
import tofu.generate.GenUUID
import tofu.logging.derivation.loggable.generate
import tofu.logging.{Loggable, LoggableContext, Logging, Logs}

import scala.reflect.ClassTag

class ContextLogging[F[_] : GenUUID : Sync : Clock] {
  implicit val context: LoggableContext[F] {type Ctx = ContextInfo} = new LoggableContext[F] {
    override type Ctx = ContextInfo

    override implicit def loggable: Loggable[ContextInfo] = generate[ContextInfo]

    override implicit def context: HasContext[F, ContextInfo] = ContextInfoBuilder[F]().buildContext
  }

  def loggerForService[A: ClassTag]: F[Logging[F]] = Logs.withContext[F, F].forService[A]
}
