package ru.hardmet.memologio
package http
package routes

import cats.effect.Sync
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}
import ru.hardmet.memologio.services.Service

abstract class Endpoint[F[_]: Sync] extends Http4sDsl[F] {

  val service: Service[F]

  val routMapper: PartialFunction[Request[F], F[Response[F]]]

  val routes: HttpRoutes[F] = HttpRoutes.of[F](routMapper)
}
