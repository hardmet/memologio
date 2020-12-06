package ru.hardmet.memologio
package services

import java.net.URI
import java.time.{LocalDate, LocalDateTime}

import cats.Monad
import cats.data.{EitherNec, EitherT, NonEmptyChain}
import cats.implicits._
import domain.posts.Post
import util.{DateParser, Parse}


trait Validator[F[_], PostId] {

  def validatePost(url: String, parsedPublished: Either[String, LocalDateTime],
                   likes: Int): F[EitherNec[String, Post.Data]]

  def validateId(id: String): F[Either[String, PostId]]

  def validateURL(url: String): F[Either[String, String]]

  def validatePublished(published: String): F[Either[String, LocalDateTime]]

  def validatePublishedDateOrDateTime(published: String): F[Either[String, Either[LocalDate, LocalDateTime]]]

  def validateLikes(likes: Int): F[Either[String, Int]]

  private[services] def nonEmptyCheck(s: String)(fieldName: String): F[Either[String, String]]
}

class ValidatorInterpreter[F[_]: Monad, PostId](implicit parsePostId: Parse[String, PostId],
                                                 parseLocalDateTime: Parse[String, LocalDateTime])
  extends Validator[F, PostId] {

  override def validatePost(url: String,
                            parsedPublished: Either[String, LocalDateTime],
                            likes: Int): F[EitherNec[String, Post.Data]] = {
    (
      validateURL(url).map(_.toEitherNec),
      F.pure(parsedPublished).map(_.toEitherNec),
      validateLikes(likes).map(_.toEitherNec)
      ).mapN { (validatedURL, validatedPublished, validatedLikes) =>
      (validatedURL, validatedPublished, validatedLikes)
        .parTupled
        .map(Function.tupled(Post.Data.apply))
    }
  }

  override def validateId(id: String): F[Either[String, PostId]] =
    nonEmptyCheck(id.trim)("uuid")
      .map { eitherNonEmpty =>
        eitherNonEmpty.flatMap { nonEmptyInput =>
          parsePostId(nonEmptyInput)
        }
      }

  override def validateURL(url: String): F[Either[String, String]] =
    nonEmptyCheck(url.trim)("url")
      .map { eitherNonEmpty =>
        eitherNonEmpty.flatMap { nonEmptyUrl =>
          Either
            .catchNonFatal {
              URI.create(nonEmptyUrl)
              url
            }.leftMap(_ => s"$nonEmptyUrl does not match the URI format.")
        }
      }

  override def validatePublished(published: String): F[Either[String, LocalDateTime]] =
    nonEmptyCheck(published.trim)("published")
      .map { eitherNonEmpty =>
        eitherNonEmpty.flatMap { nonEmptyPublished =>
          parseLocalDateTime(nonEmptyPublished)
        }
      }

  override def validatePublishedDateOrDateTime(published: String): F[Either[String, Either[LocalDate, LocalDateTime]]] =
    nonEmptyCheck(published)("published")
      .map{ eitherNonEmpty =>
        eitherNonEmpty.flatMap(parseNonEmptyDate)
      }

  private def parseNonEmptyDate(nonEmptyDate: String): Either[String, Either[LocalDate, LocalDateTime]] =
    Validator.dateParser.parseLocalDateTime(nonEmptyDate)
      .map(Right.apply)
      .leftFlatMap(dateTimeParsingError =>
        Validator.dateParser.parseLocalDate(nonEmptyDate)
          .map(localDate => Left(localDate))
      )

  override def validateLikes(likes: Int): F[Either[String, Int]] = {
    Right(likes)
      .filterOrElse(_ >= 0, s"likes value: $likes should be more or equals to zero")
      .traverse(F.pure)
  }

  override private[services] def nonEmptyCheck(s: String)(fieldName: String): F[Either[String, String]] =
    Option(s)
      .toRight(s"input $fieldName can not be null")
      .filterOrElse(!_.isEmpty, s"input $fieldName can not be empty or contains only spaces")
      .traverse(F.pure)
}

object Validator {

  private val PublishedDatePattern: String = "yyyy-MM-dd"

  private val PublishedDateTimePattern: String = "yyyy-MM-dd'T'HH:mm:ssZ"

  val dateParser: DateParser = DateParser(PublishedDatePattern, PublishedDateTimePattern)

  def apply[F[_]: Monad, PostId]()(implicit parsePostId: Parse[String, PostId],
                    parseLocalDateTime: Parse[String, LocalDateTime]): Validator[F, PostId] =
    new ValidatorInterpreter()

}
