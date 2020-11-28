package ru.hardmet.memologio
package http
package routes
package post

import java.time.format.DateTimeFormatter

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object response {

  final case class Post(id: String, url: String, published: String, likes: Int)

  object Post {
    def apply[PostId](pattern: DateTimeFormatter)(existing: domain.posts.Post.Existing[PostId]): Post =
      Post(
        id = existing.id.toString,
        url = existing.data.url,
        published = existing.data.published.format(pattern),
        likes = existing.data.likes
      )

    implicit val encoder: Encoder[Post] = deriveEncoder

    implicit def entityEncoder[F[_]]: EntityEncoder[F, Post] = jsonEncoderOf
  }

}
