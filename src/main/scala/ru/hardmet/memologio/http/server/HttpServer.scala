package ru.hardmet.memologio
package http
package server

import cats.effect
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.{HttpApp, HttpRoutes}
import ru.hardmet.memologio.config.ServerConfig

import scala.concurrent.ExecutionContext

trait HttpServer[F[_]] {
  def serve: F[Unit]
}

object HttpServer {
  private def createServer[F[_] : effect.ConcurrentEffect : effect.Timer](executionContext: ExecutionContext)
                                                                         (config: ServerConfig)
                                                                         (httpApp: HttpApp[F]): HttpServer[F] =
    new HttpServer[F] {
      override def serve: F[Unit] =
        BlazeServerBuilder(executionContext)
          .bindHttp(config.port, config.host)
          .withHttpApp(httpApp)
          .serve
          .compile
          .drain
    }

  def create[F[_] : effect.ConcurrentEffect : effect.Timer](executionContext: ExecutionContext)
                                                           (config: ServerConfig)
                                                           (mappings: (String, HttpRoutes[F])*): F[HttpServer[F]] =
    F.delay(
      createServer(executionContext)(config)(
        Logger.httpApp(logHeaders = true, logBody = true)(
          Router(mappings: _*)
            .orNotFound
        )
      )
    )
}
