package ru.hardmet.memologio
package infrastructure
package http
package routes
package post

import cats.data.NonEmptyChain
import cats.effect.Sync
import cats.implicits.toFunctorOps
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object request {
  object Post {
    final case class Create(url: String, published: String, likes: Int = 0) extends Update

    object Create {
      implicit val decoder: Decoder[Create] = deriveDecoder

      implicit def entityDecoder[F[_] : Sync]: EntityDecoder[F, Create] = jsonOf
    }

    sealed abstract class Update extends Product with Serializable {

      import Update._

      final def fold[B](ifURL: String => B,
                        ifPublished: String => B,
                        ifLikes: Int => B,
                        ifAllFields: (String, String, Int) => B): B = {
        //format:off
        this match {
          case Update.URL(url) => ifURL(url.trim)
          case Published(published) => ifPublished(published.trim)
          case Likes(likes) => ifLikes(likes)
          case AllFields(url, published, likes) => ifAllFields(url.trim, published.trim, likes)
        }
        //format:on
      }
    }

    object Update {
      final case class URL(url: String) extends Update

      final case class Published(published: String) extends Update

      final case class Likes(likes: Int) extends Update

      final type AllFields = Create
      final val AllFields = Create

      implicit val decoder: Decoder[Update] =
        NonEmptyChain[Decoder[Update]](
          deriveDecoder[AllFields].widen, // order matters
          deriveDecoder[URL].widen,
          deriveDecoder[Published].widen,
          deriveDecoder[Likes].widen
        ).reduceLeft(_ or _)


      implicit def entityDecoder[F[_] : Sync]: EntityDecoder[F, Update] =
        jsonOf
    }
  }
}
