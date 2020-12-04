package ru.hardmet.memologio
package logging_infrastructure

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.effect.Clock
import cats.{FlatMap, Functor}
import domain.RequestInfo
import tofu.generate.GenUUID
import tofu.syntax.monadic._
import tofu.{Context, HasContext}

trait RequestInfoConstructor[F[_]] {

  def build: F[RequestInfo]

  def initContext: HasContext[F, RequestInfo]
}

class RequestInfoBuilder[F[_]: GenUUID: FlatMap: Clock] extends RequestInfoConstructor[F] {

  override def build: F[RequestInfo] =
    for {
      traceId     <- GenUUID.random[F]
      startTimeMS <- Clock[F].realTime(TimeUnit.MILLISECONDS)
    } yield RequestInfo(
      traceId = traceId,
      startTime = Instant.ofEpochMilli(startTimeMS),
    )

  override def initContext: Context[F] { type Ctx = RequestInfo } = new Context[F] {
    override def functor: Functor[F] = Functor[F]

    override type Ctx = RequestInfo

    override def context: F[RequestInfo] = build
  }
}

object RequestInfoConstructor {
  def apply[F[_] : GenUUID : FlatMap : Clock](): RequestInfoBuilder[F] = new RequestInfoBuilder()
}
