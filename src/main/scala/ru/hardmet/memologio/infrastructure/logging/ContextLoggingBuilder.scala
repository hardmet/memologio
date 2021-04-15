package ru.hardmet.memologio
package infrastructure
package logging

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.effect.Clock
import cats.{FlatMap, Functor}
import tofu.generate.GenUUID
import tofu.syntax.monadic._
import tofu.{Context, HasContext}

trait ContextInfoBuilder[F[_]] {

  def buildInfo: F[ContextInfo]

  def buildContext: HasContext[F, ContextInfo]
}

class ContextInfoBuilderInterpreter[F[_] : GenUUID : FlatMap : Clock] extends ContextInfoBuilder[F] {

  override def buildInfo: F[ContextInfo] =
    for {
      traceId <- GenUUID.random[F]
      startTimeMS <- Clock[F].realTime(TimeUnit.MILLISECONDS)
    } yield ContextInfo(
      traceId = traceId,
      startTime = Instant.ofEpochMilli(startTimeMS),
    )

  override def buildContext: Context[F] {type Ctx = ContextInfo} = new Context[F] {
    override def functor: Functor[F] = Functor[F]

    override type Ctx = ContextInfo

    override def context: F[ContextInfo] = buildInfo
  }
}

object ContextInfoBuilder {
  def apply[F[_] : GenUUID : FlatMap : Clock](): ContextInfoBuilder[F] = new ContextInfoBuilderInterpreter()
}
