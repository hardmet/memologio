package ru.hardmet.memologio
package infrastructure
package logging

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant
import java.util.UUID

object domain {

  @derive(loggable)
  case class ContextInfo(traceId: UUID, startTime: Instant)

}
