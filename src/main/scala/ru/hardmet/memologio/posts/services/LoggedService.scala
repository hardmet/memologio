package ru.hardmet.memologio.posts
package services

import ru.hardmet.memologio
import ru.hardmet.memologio._
import ru.hardmet.memologio.logs
import ru.hardmet.memologio.posts.services.PostService.Service
import ru.hardmet.memologio.{Logger, Memologio}
import tofu.syntax.logging._
import zio.{URLayer, ZIO, ZLayer}

class LoggedService(svc: Service)(implicit l: Logger) extends Service {
  def getData: Memologio[Seq[PostData]] = for {
    _ <- info"started reading Post data"
    res <- svc.getData
    _ <- info"finished reading Post data, ${res.size} items read"
  } yield res

  def getOne(name: String): memologio.Memologio[Post] = svc.getOne(name)

  def putOne(data: PostData): memologio.Memologio[PostId] = for {
    _ <- info"started creating new Post $data"
    id <- svc.putOne(data)
    _ <- info"finished creating new Post $data with id $id"
  } yield id

  def remove(name: String): memologio.Memologio[Unit] = svc.remove(name)
}

object LoggedService {
  val live: URLayer[Posts, Posts] =
    ZLayer.fromEffect {
      for {
        logging <- logs.forService[Service]
        underlying <- ZIO.access[Posts](_.get)
      } yield new LoggedService(underlying)(logging)
    }
}