package ru.hardmet.memologio.http.routes

import org.http4s.HttpRoutes
import ru.hardmet.memologio.services.PostService

trait Router[F[_], PostId] {
  val postService: PostService[F, PostId]

  def routes: Seq[(String, HttpRoutes[F])]
}

class BasicRouter[F[_], PostId](override val postService: PostService[F, PostId]) extends Router[F, PostId] {
  override def routes: Seq[(String, HttpRoutes[F])] = ???
}
