package ru.hardmet.memologio

import scala.concurrent.ExecutionContext

object MemologioApp extends App {

    import cats.effect._
    val executionContext: ExecutionContext = ExecutionContext.global
    implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)
    implicit val timer: Timer[IO] = IO.timer(executionContext)
    new ApplicationBuilderBase[IO](executionContext).create.unsafeRunSync()
}
