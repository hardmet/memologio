package ru.hardmet.memologio

import ru.hardmet.memologio.config.Config
import ru.hardmet.memologio.db.DB
import ru.hardmet.memologio.http.{HttpClient, RequestInfo, ResourceCache, Server}
import ru.hardmet.memologio.posts.services.PostService
import zio._

object Memologio extends App {
  val live: RLayer[SystemEnv, MemologioEnv] =
    (ZLayer.identity[SystemEnv] ++ Config.live) >>>
      ((DB.live >>> PostService.db >>> PostService.logging) ++ HttpClient.live ++ ResourceCache.live ++
        ZLayer.identity[SystemEnv with Config]) ++ RequestInfo.build

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    Server.run as 1
}

class MemologioError(message: String) extends RuntimeException(message, null, false, false)
