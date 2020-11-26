package ru.hardmet.memologio.repository

import cats.effect.{Async, Blocker, Resource}
import doobie.postgres
import doobie.util.transactor.Transactor
import javax.sql.PooledConnection
import org.postgresql.ds.{PGConnectionPoolDataSource, PGSimpleDataSource}
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
          val alloc = Async[Task].delay{
            val dataSource = new PGSimpleDataSource()
            dataSource.setServerNames(Array[String](conf.host))
            dataSource.setPortNumbers(Array[Int](conf.port))
            dataSource.setDatabaseName(conf.database)
            dataSource.setUser(conf.user)
            dataSource.setPassword(conf.password)
            dataSource
          }
          val free = (ds: PGSimpleDataSource) => Async[Task].delay{
            ds.getConnection.close()
          }
          Resource.make(alloc)(free).map(x => Transactor.fromDataSource[Task](x, liveEC, Blocker.liftExecutionContext(blockEC)))
        }.toManagedZIO
      } yield Service(trans)
    )
}
