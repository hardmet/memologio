package ru.hardmet.memologio
package infrastructure
package http
package routes
package post

import java.time.LocalDateTime

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.server.Router
import ru.hardmet.memologio.services.PostService
import ru.hardmet.memologio.util.Parse

class PostRouter[F[_] : Sync, PostId](override val service: PostService[F, PostId])
                                     (implicit parse: Parse[String, PostId],
                                      localDateTimeParser: Parse[String, LocalDateTime]) extends Router[F] {
  override def routes: HttpRoutes[F] = Router(
    "posts" -> new PostEndpoint[F, PostId](service, Pattern).routes
  )
}
