package ru.hardmet.memologio
package infrastructure
package http
package routes

import cats.effect.Sync
import infrastructure.http.routes.post.PostEndpoint
import org.http4s.HttpRoutes
import org.http4s.server.Router
import services.post.PostService
import services.post.domain.PostId
import util.Parse

import java.time.LocalDateTime

class RouterInterpreter[F[_] : Sync](override val service: PostService[F])
                                    (implicit parse: Parse[String, PostId],
                                     localDateTimeParser: Parse[String, LocalDateTime]) extends Router[F] {
  override def routes: HttpRoutes[F] = Router(
    "posts" -> new PostEndpoint[F](service, Pattern).routes
  )
}
