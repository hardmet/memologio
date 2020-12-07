package ru.hardmet.memologio
package services

import java.time.{LocalDate, LocalDateTime}

import cats.Monad
import util.{NonEmptyRuleInterpreter, Parse}

trait PostParser[F[_], PostId] extends NonEmptyRuleInterpreter[F] {

  def parseId(id: String): F[Either[String, PostId]]

  def parsePublished(published: String): F[Either[String, LocalDateTime]]

  def parsePublishedDateOrDateTime(published: String): F[Either[String, Either[LocalDate, LocalDateTime]]]

}

class PostParserInterpreter[F[_]: Monad, PostId](implicit parsePostId: Parse[String, PostId],
                                                 parseLocalDateTime: Parse[String, LocalDateTime])
  extends PostParser[F, PostId] {

  override def parseId(id: String): F[Either[String, PostId]] = ???

  override def parsePublished(published: String): F[Either[String, LocalDateTime]] = ???

  override def parsePublishedDateOrDateTime(published: String): F[Either[String, Either[LocalDate, LocalDateTime]]] = ???

  //  override def parsePublished(published: String): F[Either[String, LocalDateTime]] =
  //    parser.nonEmptyApply(published.trim)("published")
  //      .flatMap { eitherNonEmptyPublished: Either[String, String] =>
  //        eitherNonEmptyPublished.flatTraverse { nonEmptyPublished =>
  //          parser.parsePublished(nonEmptyPublished)
  //        }
  //      }
  private def parseNonEmptyDate(nonEmptyDate: String): Either[String, Either[LocalDate, LocalDateTime]] = ???
//    Validator.dateParser.parseLocalDateTime(nonEmptyDate)
//      .map(Right.apply)
//      .leftFlatMap(dateTimeParsingError =>
//        Validator.dateParser.parseLocalDate(nonEmptyDate)
//          .map(localDate => Left(localDate))
//      )
}

object PostParser {
  def apply[F[_]: Monad, PostId](implicit parsePostId: Parse[String, PostId],
                          parseLocalDateTime: Parse[String, LocalDateTime]): PostParser[F, PostId] = new PostParserInterpreter[F, PostId]()
}
