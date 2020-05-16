package ru.hardmet.memologio.db

import cats.effect.Blocker
import doobie.h2.H2Transactor
import doobie.util.transactor.Transactor
import ru.hardmet.memologio.config.Config
import zio._
import zio.blocking.{Blocking, blocking}
import zio.interop.catz._

object DB {
  case class Service(transactor: Transactor[Task])

  val live: RLayer[Config with Blocking, DB] =
    ZLayer.fromManaged(
      for {
        liveEC  <- ZIO.descriptor.map(_.executor.asEC).toManaged_
        blockEC <- blocking(ZIO.descriptor.map(_.executor.asEC)).toManaged_
        conf    <- RIO.access[Config](_.get.db).toManaged_
        trans <- H2Transactor
                  .newH2Transactor[Task](
                    conf.url,
                    conf.user,
                    conf.password,
                    liveEC,
                    Blocker.liftExecutionContext(blockEC)
                  )
                  .toManagedZIO
      } yield Service(trans)
    )
}
