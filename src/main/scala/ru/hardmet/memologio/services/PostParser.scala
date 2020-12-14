package ru.hardmet.memologio
package services

import java.time.{LocalDate, LocalDateTime}

import cats.Monad
import cats.implicits._
import util.{NonEmptyRuleInterpreter, Parse}

trait PostParser[F[_], PostId] extends NonEmptyRuleInterpreter[F] {

  def parseId(id: String): F[Either[String, PostId]]

  def parsePublished(published: String): F[Either[String, LocalDateTime]]

  def parsePublishedDateOrDateTime(published: String): F[Either[String, Either[LocalDate, LocalDateTime]]]

}

class PostParserInterpreter[F[_] : Monad, PostId](implicit parsePostId: Parse[String, PostId],
                                                  parseLocalDate: Parse[String, LocalDate],
                                                  parseLocalDateTime: Parse[String, LocalDateTime])
  extends PostParser[F, PostId] {

  override def parseId(id: String): F[Either[String, PostId]] =
    parseWithNonEmpty(id)("postId")(parsePostId)

  override def parsePublished(published: String): F[Either[String, LocalDateTime]] =
    parseWithNonEmpty(published)("published")(parseLocalDateTime)

  override def parsePublishedDateOrDateTime(published: String): F[Either[String, Either[LocalDate, LocalDateTime]]] =
    nonEmptyRun(published)("published").map { errorOrNonEmptyPublished: Either[String, String] =>
      errorOrNonEmptyPublished.flatMap { nonEmptyPublished =>
        parseLocalDateTime(nonEmptyPublished)
          .map(Right.apply)
          .leftFlatMap(_ =>
            parseLocalDate(nonEmptyPublished)
              .map(Left.apply)
          )
      }
    }

  private def parseWithNonEmpty[Output](input: String)(entityName: String = "")
                                       (implicit parse: Parse[String, Output]): F[Either[String, Output]] =
    nonEmptyRun(input.trim)(entityName)
      .map { errorOrNonEmpty: Either[String, String] =>
        errorOrNonEmpty.flatMap { nonEmpty =>
          parse(nonEmpty)
        }
      }
}

object PostParser {
  def apply[F[_] : Monad, PostId](implicit parsePostId: Parse[String, PostId],
                                  parseLocalDateTime: Parse[String, LocalDateTime]): PostParser[F, PostId] =
    new PostParserInterpreter[F, PostId]()
}
