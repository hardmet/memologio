package ru.hardmet.memologio.scrapper

import cats.effect.{Async, Blocker, Resource}
import doobie.util.transactor.Transactor
import org.postgresql.ds.PGSimpleDataSource
import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.db.DB
import ru.hardmet.memologio.http.HttpClient
import ru.hardmet.memologio.{Logger, logs}
import ru.hardmet.memologio.posts.Posts
import ru.hardmet.memologio.posts.services.{DBService, LoggedService}
import ru.hardmet.memologio.posts.services.PostService.Service
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.blocking.{Blocking, blocking}
import zio.{RIO, RLayer, Task, TaskLayer, URLayer, ZIO, ZLayer}


class Scrapper(svc: Service) {

}

object Scrapper {

  val live: URLayer[Posts, Posts] =
    ZLayer.fromEffect {
      for {
        underlying <- ZIO.access[Posts](_.get)
        scrapper = new Scrapper(underlying)
      } yield underlying
    }


}
