package ru.hardmet.memologio
package services

import cats.Applicative
import cats.data.EitherNec
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.syntax.traverse._
import post.domain.{Data, Url}
import util.NonEmptyRule

import java.net.URI
import java.time.{LocalDate, LocalDateTime}


trait PostValidator[F[_]] {

  def validatePostWithUnreliablyPublished(url: String, likes: Int)
                                         (published: Either[String, LocalDateTime]): F[EitherNec[String, Data]]

  def validatePost(post: Data): F[EitherNec[String, Data]]

  def validateURL(url: String): F[Either[String, Url]]

  def validatePublished(published: LocalDateTime): F[Either[String, LocalDateTime]]

  def validatePublishedDate(published: LocalDate): F[Either[String, LocalDate]]

  def validatePublishedDateOrDateTime(published: Either[LocalDate, LocalDateTime]): F[Either[String, Either[LocalDate, LocalDateTime]]]

  def validateLikes(likes: Int): F[Either[String, Int]]
}

class PostValidatorEffectLess {

  private[services] def parPostValidation(errorNecOrURL: EitherNec[String, Url],
                                          errorNecOrPublished: EitherNec[String, LocalDateTime],
                                          errorNecOrLikes: EitherNec[String, Int]): EitherNec[String, Data] =
    (errorNecOrURL, errorNecOrPublished, errorNecOrLikes)
      .parTupled
      .map(Function.tupled(Data.apply))

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

class PostValidatorInterpreter[F[_] : Applicative : NonEmptyRule] extends PostValidatorEffectLess
  with PostValidator[F] {

  override def validatePostWithUnreliablyPublished(url: String, likes: Int)
                                                  (published: Either[String, LocalDateTime]): F[EitherNec[String, Data]] =
    (
      validateURL(url).map(_.toEitherNec),
      published.flatTraverse(validatePublished).map(_.toEitherNec),
      validateLikes(likes).map(_.toEitherNec)
      ).mapN(parPostValidation)

  override def validatePost(post: Data): F[EitherNec[String, Data]] =
    (
      validateURL(post.url).map(_.toEitherNec),
      validatePublished(post.published).map(_.toEitherNec),
      validateLikes(post.likes).map(_.toEitherNec)
      ).mapN(parPostValidation)

  def validateURL(url: Url): F[Either[String, Url]] =
    validateURL(url.value)

  override def validateURL(url: String): F[Either[String, Url]] =
    for {
      errorOrNonEmptyURI <- F.nonEmptyRun(url.trim)("url")
      errorOrValidURL = validateNonEmptyURL(errorOrNonEmptyURI)
      validUrl = errorOrValidURL.map(Url(_))
    } yield validUrl

  private def validateNonEmptyURL(urlOrError: Either[String, String]): Either[String, String] = for {
    nonEmptyURL <- urlOrError
    errorOrValidURL <- isValidURI(nonEmptyURL)
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

  def apply[F[_] : Applicative : NonEmptyRule](): PostValidator[F] =
    new PostValidatorInterpreter[F]()

}
