package ru.hardmet.memologio
package http

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import derevo.derive
import tofu.logging.Loggable._
import tofu.logging.derivation.loggable
import zio.{UIO, URLayer, ZLayer}
import zio.clock.{Clock, currentTime}

@derive(loggable)
case class RequestInfo(
                        traceId: UUID,
                        startTime: Instant
                      )

object RequestInfo {
  val build: URLayer[Clock, ReqInfo] = ZLayer.fromEffect(
    for {
      traceId     <- UIO.effectTotal(UUID.randomUUID())
      startTimeMS <- currentTime(TimeUnit.MILLISECONDS)
    } yield RequestInfo(
      traceId = traceId,
      startTime = Instant.ofEpochMilli(startTimeMS),
    )
  )
}