package ru.hardmet.memologio
package repository

import cats.effect.Resource
import cats.implicits.catsSyntaxOptionId
import ru.hardmet.memologio.config.DBConfig

trait DBConnector[F[_], PostId] {
  def connectToRepository(config: DBConfig): Resource[F, PostRepository[F, PostId]]
}
