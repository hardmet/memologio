package ru.hardmet.memologio
package util

import cats.Monad
import cats.implicits._

trait NonEmptyRule[F[_]] {
  def nonEmptyRun(s: String)(fieldName: String): F[Either[String, String]]
}

class NonEmptyRuleInterpreter[F[_] : Monad] extends NonEmptyRule[F] {

  override def nonEmptyRun(s: String)(fieldName: String): F[Either[String, String]] =
    nonEmpty(s)(fieldName)
      .pure[F]

  private[util] def nonEmpty(s: String)(fieldName: String): Either[String, String] =
    Option(s)
      .toRight(s"input $fieldName can not be null")
      .filterOrElse(_.nonEmpty, s"input $fieldName can not be empty or contains only spaces")
}

object NonEmptyRule {
  implicit def nonEmptyRule[F[_] : Monad]: NonEmptyRule[F] = new NonEmptyRuleInterpreter[F]

}
