package ru.hardmet.memologio
package infrastructure
package http
package routes

import org.http4s.HttpRoutes
import services.Service

trait Router[F[_]] {

  val service: Service[F]

  def routes: HttpRoutes[F]
}
