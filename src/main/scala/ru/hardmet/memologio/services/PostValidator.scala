package ru.hardmet.memologio
package services

import java.net.URI
import java.time.{LocalDate, LocalDateTime}

import cats.Monad
import cats.data.EitherNec
import cats.implicits._
import domain.posts.Post
import util.{DateParser, Parse}


trait PostValidator[F[_], PostId] {

  def validatePost(post: Post.Data): F[EitherNec[String, Post.Data]]

  def validateURL(url: String): F[Either[String, String]]

  def validatePublished(published: LocalDateTime): F[Either[String, LocalDateTime]]

  def validatePublishedDate(published: LocalDate): F[Either[String, LocalDate]]

  def validatePublishedDateOrDateTime(published: Either[LocalDate, LocalDateTime]): F[Either[String, Either[LocalDate, LocalDateTime]]]

  def validateLikes(likes: Int): F[Either[String, Int]]
}

class PostValidatorInterpreter[F[_]: Monad, PostId]
  extends PostValidator[F, PostId] {

  override def validatePost(post: Post.Data): F[EitherNec[String, Post.Data]] =
    (
      validateURL(post.url).map(_.toEitherNec),
      validatePublished(post.published).map(_.toEitherNec),
      validateLikes(post.likes).map(_.toEitherNec)
      ).mapN{ (validatedURL, validatedPublished, validatedLikes) =>
      (validatedURL, validatedPublished, validatedLikes)
        .parTupled
        .map(Function.tupled(Post.Data.apply))
    }

  override def validateURL(url: String): F[Either[String, String]] =
    Either
      .catchNonFatal{
        URI.create(url)
        url
      }.leftMap(_ => s"$url does not match the URI format.")
      .pure[F]

  def validatePublished(published: LocalDateTime): F[Either[String, LocalDateTime]] =
    Right(published)
      .filterOrElse(p => p.isBefore(LocalDateTime.now()), "Published date can't be after processing time")
      .pure[F]

  def validatePublishedDate(published: LocalDate): F[Either[String, LocalDate]] =
    Right(published)
      .filterOrElse(p => p.isBefore(LocalDate.now()), "Published date can't be after processing date")
      .pure[F]

  override def validatePublishedDateOrDateTime(published: Either[LocalDate, LocalDateTime]): F[Either[String, Either[LocalDate, LocalDateTime]]] =
    published.fold(validatePublishedDate, validatePublished)
      .map(dateOrDateTime =>
        dateOrDateTime.map(_ => published)
      )

  override def validateLikes(likes: Int): F[Either[String, Int]] = {
    Right(likes)
      .filterOrElse(_ >= 0, s"likes value: $likes should be more or equals to zero")
      .traverse(F.pure)
  }
}

object PostValidator {

  private val PublishedDatePattern: String = "yyyy-MM-dd"

  private val PublishedDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZ"

  val dateParser: DateParser = DateParser(PublishedDatePattern, PublishedDateTimePattern)

  def apply[F[_]: Monad, PostId]()(implicit parsePostId: Parse[String, PostId],
                                   parseLocalDateTime: Parse[String, LocalDateTime]): PostValidator[F, PostId] = ???
//    new ValidatorInterpreter()

}
