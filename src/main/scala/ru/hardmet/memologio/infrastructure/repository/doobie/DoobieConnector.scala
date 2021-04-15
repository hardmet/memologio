package ru.hardmet.memologio
package infrastructure
package repository.doobie

import cats.effect.{Blocker, Concurrent, ContextShift, Resource}
import cats.syntax.functor._
import config.DBConfig
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.postgresql.ds.PGSimpleDataSource
import repository.{DBConnector, PostRepository}

class DoobieConnector[F[_] : Concurrent : ContextShift : natchez.Trace] extends DBConnector[F] {
  override def connectToRepository(config: DBConfig): Resource[F, PostRepository[F]] = {
    import config._
    for {
      ds <- Resource.make {
        F.delay {
          val dataSource = new PGSimpleDataSource()
          dataSource.setServerNames(Array[String](host))
          dataSource.setPortNumbers(Array[Int](port))
          dataSource.setDatabaseName(database)
          dataSource.setUser(user)
          dataSource.setPassword(password)
          dataSource
        }
      }((ds: PGSimpleDataSource) => F.delay(ds.getConnection.close()).void)
      ec <- ExecutionContexts.fixedThreadPool[F](poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
    } yield new DoobiePostRepositoryInterpreter[F](
      Transactor.fromDataSource[F](ds, ec, Blocker.liftExecutionContext(txnEc))
    )
  }
}
