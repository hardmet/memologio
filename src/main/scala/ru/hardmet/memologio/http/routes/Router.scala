package ru.hardmet.memologio
package http
package routes

import org.http4s.HttpRoutes
import ru.hardmet.memologio.services.Service

trait Router[F[_]] {

  val service: Service[F]

  def routes: Seq[(String, HttpRoutes[F])]
}
