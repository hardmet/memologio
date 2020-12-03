package ru.hardmet.memologio
package domain

import java.time.Instant
import java.util.UUID

import derevo.derive
import tofu.logging.derivation.loggable


@derive(loggable)
case class RequestInfo(traceId: UUID, startTime: Instant)
