package ru.hardmet.memologio
package infrastructure
package repository.skunk

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.syntax.option._
import config.DBConfig
import repository.{DBConnector, PostRepository}
import skunk.Session

class SkunkConnector[F[_] : Concurrent : ContextShift : natchez.Trace] extends DBConnector[F] {

  override def connectToRepository(config: DBConfig): Resource[F, PostRepository[F]] = {
    import config._
    for {
      sessionPool <- Session.pooled[F](
        host = host,
        port = port,
        user = user,
        password = password.some,
        database = database,
        max = poolSize,
        debug = false
      )
    } yield new SkunkPostRepositoryInterpreter[F](sessionPool)
  }
}
