package ru.hardmet.memologio
package http
package routes
package post

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.server.Router
import ru.hardmet.memologio.services.PostService

class PostRouter[F[_] : Sync, PostId](override val service: PostService[F, PostId])
                                     (implicit parse: Parse[String, PostId]) extends Router[F] {
  override def routes: HttpRoutes[F] = Router(
    "posts" -> new PostEndpoint[F, PostId](service, Pattern).routes
  )
}
