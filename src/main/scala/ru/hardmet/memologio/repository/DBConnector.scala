package ru.hardmet.memologio
package repository

import java.util.UUID

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits.catsSyntaxOptionId
import ru.hardmet.memologio.config.DBConfig
import skunk.Session
import skunk_interpreter.SkunkPostRepositoryInterpreter

trait DBConnector[F[_], PostId] {

  def connectToRepository(config: DBConfig): Resource[F, PostRepository[F, PostId]]
}

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
        max = 10,
        debug = false
      )
    } yield new SkunkPostRepositoryInterpreter[F](sessionPool)
  }
}
