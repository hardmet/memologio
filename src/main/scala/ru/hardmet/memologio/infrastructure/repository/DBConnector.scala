package ru.hardmet.memologio
package infrastructure
package repository

import cats.effect.Resource
import ru.hardmet.memologio.config.DBConfig

trait DBConnector[F[_]] {
  def connectToRepository(config: DBConfig): Resource[F, PostRepository[F]]
}
