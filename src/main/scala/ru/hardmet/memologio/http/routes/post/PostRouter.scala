package ru.hardmet.memologio
package http
package routes
package post

import cats.effect.Sync
import org.http4s.HttpRoutes
import ru.hardmet.memologio.services.PostService

class PostRouter[F[_] : Sync, PostId](override val service: PostService[F, PostId])
                                     (implicit parse: Parse[String, PostId]) extends Router[F] {
  override def routes: Seq[(String, HttpRoutes[F])] = Seq(
    "posts" -> new PostEndpoint[F, PostId](service, Pattern).routes
  )
}
