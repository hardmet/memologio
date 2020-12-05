package ru.hardmet.memologio
package infrastructure
package http

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits.toSemigroupKOps
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.{HttpApp, HttpRoutes}
import config.ServerConfig

import scala.concurrent.ExecutionContext

trait HttpServer[F[_]] {
  def serve: F[Unit]
}

object HttpServer {
  private def createServer[F[_] : ConcurrentEffect : Timer](executionContext: ExecutionContext)
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

  def create[F[_] : ConcurrentEffect : Timer](executionContext: ExecutionContext)
                                                           (config: ServerConfig)
                                                           (routes: HttpRoutes[F]*): F[HttpServer[F]] =
    F.delay(
      createServer(executionContext)(config)(
        Logger.httpApp(logHeaders = true, logBody = true)(
          Router("api" -> routes.reduceLeft(_ <+> _))
            .orNotFound
        )
      )
    )
}
