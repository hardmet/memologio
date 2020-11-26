package ru.hardmet.memologio.posts
package services

import java.util.UUID

import ru.hardmet.memologio.Memologio
import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.repository.DB
import zio.{UIO, URLayer}

object PostService {

  trait Service {

    def getData: Memologio[Seq[PostData]]

    def putOne(data: PostData): Memologio[PostId]

    def remove(name: String): Memologio[Unit]
  }

  def newId[T](app: UUID => T): UIO[T] = UIO.effectTotal(app(UUID.randomUUID()))
}
