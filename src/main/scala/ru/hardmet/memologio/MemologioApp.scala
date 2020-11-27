package ru.hardmet.memologio

import scala.concurrent.ExecutionContext

object Memologio extends App {

    import cats.effect._
    val executionContext: ExecutionContext = ExecutionContext.global
    implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)
    implicit val timer: Timer[IO] = IO.timer(executionContext)

    new ApplicationBuilderBase[cats.effect.IO](executionContext).create.unsafeRunSync()
}

class MemologioError(message: String) extends RuntimeException(message, null, false, false) // TODO use or delete
