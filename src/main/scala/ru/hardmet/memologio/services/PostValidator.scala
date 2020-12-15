package ru.hardmet.memologio
package services

import cats.Monad
import cats.data.EitherNec
import cats.implicits._
import domain.posts.Post
import util.NonEmptyRule

import java.net.URI
import java.time.{LocalDate, LocalDateTime}


trait PostValidator[F[_], PostId] {

  def validatePostWithUnreliablyPublished(url: String, likes: Int)
                                         (published: Either[String, LocalDateTime]): F[EitherNec[String, Post.Data]]

  def validatePost(post: Post.Data): F[EitherNec[String, Post.Data]]

  def validateURL(url: String): F[Either[String, String]]

  def validatePublished(published: LocalDateTime): F[Either[String, LocalDateTime]]

  def validatePublishedDate(published: LocalDate): F[Either[String, LocalDate]]

  def validatePublishedDateOrDateTime(published: Either[LocalDate, LocalDateTime]): F[Either[String, Either[LocalDate, LocalDateTime]]]

  def validateLikes(likes: Int): F[Either[String, Int]]
}

class PostValidatorEffectLess {

  private[services] def parPostValidation(errorNecOrURL: EitherNec[String, String],
                                          errorNecOrPublished: EitherNec[String, LocalDateTime],
                                          errorNecOrLikes: EitherNec[String, Int]): EitherNec[String, Post.Data] =
    (errorNecOrURL, errorNecOrPublished, errorNecOrLikes)
      .parTupled
      .map(Function.tupled(Post.Data.apply))

  private[services] def isValidURI(nonEmptyURI: String): Either[String, String] =
    Either
      .catchOnly[IllegalArgumentException] {
        URI.create(nonEmptyURI)
        nonEmptyURI
      }.leftMap(_ => s"uri: '$nonEmptyURI' does not match the URI format.")

  private[services] def isValidPublished(published: LocalDateTime): Either[String, LocalDateTime] =
    Right(published)
      .filterOrElse(p => p.isBefore(LocalDateTime.now()), "Published date can't be after processing time")

  def isValidPublishedDate(published: LocalDate): Either[String, LocalDate] =
    Right(published)
      .filterOrElse(p => p.isBefore(LocalDate.now()), "Published date can't be after processing date")

  private[services] def isValidLikes(likes: Int): Either[String, Int] =
    Right(likes)
      .filterOrElse(_ >= 0, s"likes: $likes should be more or equals to zero")
}

class PostValidatorInterpreter[F[_] : Monad : NonEmptyRule, PostId] extends PostValidatorEffectLess
  with PostValidator[F, PostId] {

  override def validatePostWithUnreliablyPublished(url: String, likes: Int)
                                                  (published: Either[String, LocalDateTime]): F[EitherNec[String, Post.Data]] =
    (
      validateURL(url).map(_.toEitherNec),
      published.flatTraverse(validatePublished).map(_.toEitherNec),
      validateLikes(likes).map(_.toEitherNec)
      ).mapN(parPostValidation)

  override def validatePost(post: Post.Data): F[EitherNec[String, Post.Data]] =
    (
      validateURL(post.url).map(_.toEitherNec),
      validatePublished(post.published).map(_.toEitherNec),
      validateLikes(post.likes).map(_.toEitherNec)
      ).mapN(parPostValidation)

  override def validateURL(url: String): F[Either[String, String]] =
    for {
      errorOrNonEmptyURI <- F.nonEmptyRun(url.trim)("url")
      errorOrValidURL = for {
        nonEmptyURL <- errorOrNonEmptyURI
        errorOrValidURL <- isValidURI(nonEmptyURL)
      } yield errorOrValidURL
    } yield errorOrValidURL

  def validatePublished(published: LocalDateTime): F[Either[String, LocalDateTime]] =
    isValidPublished(published)
      .pure[F]

  def validatePublishedDate(published: LocalDate): F[Either[String, LocalDate]] =
    isValidPublishedDate(published)
      .pure[F]

  override def validatePublishedDateOrDateTime(published: Either[LocalDate, LocalDateTime]): F[Either[String, Either[LocalDate, LocalDateTime]]] =
    published.fold(validatePublishedDate, validatePublished)
      .map(dateOrDateTime =>
        dateOrDateTime.map(_ => published)
      )

  override def validateLikes(likes: Int): F[Either[String, Int]] =
    isValidLikes(likes)
      .pure[F]
}

object PostValidator {

  def apply[F[_] : Monad : NonEmptyRule, PostId](): PostValidator[F, PostId] =
    new PostValidatorInterpreter[F, PostId]()

}
