package ru.hardmet.memologio
package infrastructure
package repository.skunk

import java.util.UUID

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits.catsSyntaxOptionId
import config.DBConfig
import repository.DBConnector
import repository.PostRepository
import skunk.Session

class SkunkConnector[F[_]: Concurrent: ContextShift: natchez.Trace] extends DBConnector[F,  UUID] {

  override def connectToRepository(config: DBConfig): Resource[F, PostRepository[F, UUID]] = {
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
