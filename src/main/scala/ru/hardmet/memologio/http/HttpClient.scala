package ru.hardmet.memologio.http

import ru.hardmet.memologio.Memologio
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._

object HttpClient {
  class Service(sttpBackend: SttpBackend[Task, Nothing, WebSocketHandler]) {
    def httpReq[T](request: Request[T, Nothing]): Task[Response[T]] =
      sttpBackend.send(request)
  }

  val live: TaskLayer[HttpClient] =
    ZLayer.fromManaged(AsyncHttpClientZioBackend.managed().map(backend => new Service(backend)))

  def httpReq[T](request: Request[T, Nothing]): Memologio[Response[T]] =
    ZIO.accessM[HttpClient](_.get.httpReq(request))
}
