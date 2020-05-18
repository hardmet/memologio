package ru.hardmet.memologio.db

import cats.effect.{Async, Blocker, Resource}
import doobie.postgres
import doobie.util.transactor.Transactor
import org.h2.jdbcx.JdbcConnectionPool
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
        trans <- {
          val alloc = Async[Task].delay(JdbcConnectionPool.create(conf.url, conf.user, conf.password))
          val free = (ds: JdbcConnectionPool) => Async[Task].delay(ds.dispose())
          Resource.make(alloc)(free).map(Transactor.fromDataSource[Task](_, liveEC, Blocker.liftExecutionContext(blockEC)))
        }.toManagedZIO
      } yield Service(trans)
    )
//  trans <- Transactor.fromDriverManager[Task](
//    "org.postgresql.Driver", conf.url, conf.user, conf.password
//  )
}
