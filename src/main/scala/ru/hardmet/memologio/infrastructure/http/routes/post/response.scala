package ru.hardmet.memologio
package infrastructure
package http
package routes
package post

import java.time.format.DateTimeFormatter

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import services.post.domain.Existing

object response {

  final case class Post(id: String, url: String, published: String, likes: Int)

  object Post {
    def apply[PostId](pattern: DateTimeFormatter)(existing: Existing): Post =
      Post(
        id = existing.id.toString,
        url = existing.data.url.value,
        published = existing.data.published.format(pattern),
        likes = existing.data.likes
      )

    implicit val encoder: Encoder[Post] = deriveEncoder

    implicit def entityEncoder[F[_]]: EntityEncoder[F, Post] = jsonEncoderOf
  }

}
