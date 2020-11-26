package ru.hardmet

import java.util.UUID

import cats.effect.{Concurrent, ContextShift}
import cats.{ApplicativeError, Monad}
import ru.hardmet.memologio.MemologioEnv
import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.http.{HttpClient, ReqInfo, ResourceCache}
import ru.tinkoff.tschema.finagle.zioRouting.RIOH
import tofu.logging.zlogs.ZLogs
import tofu.logging.{Loggable, Logging}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz

package object memologio extends LowPriorInstances {
  type SystemEnv  = Blocking with Clock with Console
  type LoggingEnv = ReqInfo
  type MemologioEnv =
    ResourceCache with Config with SystemEnv with LoggingEnv with HttpClient

  type Memologio[+A]     = RIO[MemologioEnv, A]
  type MemologioHttp[+A] = RIOH[MemologioEnv, A]
  type Init[+A]       = RManaged[SystemEnv, A]

  final implicit val MemologioMonad: Concurrent[Memologio] = catz.taskConcurrentInstance
  final implicit val initMonad: Monad[Init]          = catz.monadErrorZManagedInstances

  final implicit val httpMonad: Monad[MemologioHttp]         = catz.monadErrorInstance
  final implicit val uioMonad: Monad[UIO]                 = catz.monadErrorInstance
  final implicit val taskMonad: Concurrent[Task]          = catz.taskConcurrentInstance
  final implicit val taskContextShift: ContextShift[Task] = catz.zioContextShift

  implicit val uuidLoggable: Loggable[UUID] = Loggable[String].contramap(_.toString)

  type Logger = Logging[URIO[LoggingEnv, *]]
  val logs: ZLogs[LoggingEnv] = ZLogs.build.of[ReqInfo].make[LoggingEnv]
}

trait LowPriorInstances {
  final implicit def MemologioErrMonad[E]: ApplicativeError[ZIO[MemologioEnv, E, *], E] = catz.monadErrorInstance
}
