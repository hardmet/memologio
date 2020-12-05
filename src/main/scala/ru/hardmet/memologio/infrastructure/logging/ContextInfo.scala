package ru.hardmet.memologio
package infrastructure
package logging

import java.time.Instant
import java.util.UUID

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
case class ContextInfo(traceId: UUID, startTime: Instant)
