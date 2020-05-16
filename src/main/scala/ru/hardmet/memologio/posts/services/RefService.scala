package ru.hardmet.memologio.posts
package services

import java.util.UUID

import cats.instances.list._
import cats.syntax.traverse._
import ru.hardmet.memologio.Memologio
import ru.hardmet.memologio.posts.{Post, Posts}
import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.entities.Entities
import ru.hardmet.memologio.posts.services.RefService.State
import ru.hardmet.memologio._
import zio._

import scala.collection.immutable.ArraySeq

class RefService(ref: Ref[State]) extends PostService.Service {

  def getData: UIO[Seq[PostData]] =
    ref.get.map(_.valuesIterator.map(_.data).to(ArraySeq))

  def getOne(name: String): Task[Post] =
    ref.get.map(_.get(name)).someOrFail(NotFound(name))

  def putOne(data: PostData): Memologio[PostId] =
    for {
      uuid   <- UIO.effectTotal(UUID.randomUUID())
      id     = PostId(uuid)
      entity <- Entities.makeEntity(id, data)
      added <- ref.modify(m =>
        if (m.contains(data.name)) (false, m)
        else (true, m + (data.name -> entity))
      )
      _ <- ZIO.fail(AlreadyExists(data.name)).when(!added)
    } yield id

  def remove(name: String): Task[Unit] =
    ref.modify { m =>
      if (m.contains(name)) (true, m - name)
      else (false, m)
    }.reject { case false => NotFound(name) }.unit
}

object RefService {
  private def initialState: URIO[Config, State] =
    ZIO.accessM[Config](_.get.posts.initial.traverse { data =>
      for {
        id     <- PostService.newId
        entity = Entities.initEntity(id, data)
      } yield data.name -> entity
    }.map(_.toMap))

  type State = Map[String, Post]

  val live: URLayer[Config, Posts] = ZLayer.fromEffect(
    for (state <- initialState; ref <- Ref.make(state)) yield new RefService(ref)
  )
}
