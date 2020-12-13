package ru.hardmet.memologio
package services

import java.net.URI
import java.time.{LocalDate, LocalDateTime}

import cats.Monad
import cats.data.EitherNec
import cats.implicits._
import domain.posts.Post
import util.{DateParser, NonEmptyRule}


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

class PostValidatorInterpreter[F[_]: Monad: NonEmptyRule, PostId] extends PostValidator[F, PostId] {

  override def validatePostWithUnreliablyPublished(url: String, likes: Int)
                                                  (published: Either[String, LocalDateTime]): F[EitherNec[String, Post.Data]] =
    (
      validateURL(url).map(_.toEitherNec),
      published.flatTraverse(validatePublished).map(_.toEitherNec),
      validateLikes(likes).map(_.toEitherNec)
      ).mapN(parCreatePostData)

  override def validatePost(post: Post.Data): F[EitherNec[String, Post.Data]] =
    (
      validateURL(post.url).map(_.toEitherNec),
      validatePublished(post.published).map(_.toEitherNec),
      validateLikes(post.likes).map(_.toEitherNec)
      ).mapN(parCreatePostData)

  private def parCreatePostData(errorNecOrURL: EitherNec[String,String],
                                errorNecOrPublished: EitherNec[String,LocalDateTime],
                                errorNecOrLikes: EitherNec[String,Int]): EitherNec[String, Post.Data] =
    (errorNecOrURL, errorNecOrPublished, errorNecOrLikes)
      .parTupled
      .map(Function.tupled(Post.Data.apply))

  override def validateURL(url: String): F[Either[String, String]] =
    for {
      errorOrNonEmptyURL <- F.nonEmptyApply(url.trim)("url")
      errorOrValidURL = for {
        nonEmptyURL <- errorOrNonEmptyURL
        errorOrValidURL <- validateNonEmptyURL(nonEmptyURL)
      } yield errorOrValidURL
    } yield errorOrValidURL

  private[services] def validateNonEmptyURL(nonEmptyURL: String): Either[String, String] =
    Either
      .catchNonFatal {
        URI.create(nonEmptyURL)
        nonEmptyURL
      }.leftMap(_ => s"url: '$nonEmptyURL' does not match the URI format.")

  def validatePublished(published: LocalDateTime): F[Either[String, LocalDateTime]] =
    validatePublishedBase(published)
      .pure[F]

  private[services] def validatePublishedBase(published: LocalDateTime): Either[String, LocalDateTime] =
    Right(published)
      .filterOrElse(p => p.isBefore(LocalDateTime.now()), "Published date can't be after processing time")

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
      .filterOrElse(_ >= 0, s"likes: $likes should be more or equals to zero")
      .traverse(F.pure)
  }
}

object PostValidator {

  private val PublishedDatePattern: String = "yyyy-MM-dd"

  private val PublishedDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZ"

  val dateParser: DateParser = DateParser(PublishedDatePattern, PublishedDateTimePattern)

  def apply[F[_] : Monad: NonEmptyRule, PostId](): PostValidator[F, PostId] =
    new PostValidatorInterpreter[F, PostId]()

}
