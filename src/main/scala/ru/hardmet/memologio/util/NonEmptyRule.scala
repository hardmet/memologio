package ru.hardmet.memologio
package util

import cats.Applicative
import cats.syntax.applicative._

trait NonEmptyRule[F[_]] {
  def nonEmptyRun(s: String)(fieldName: String): F[Either[String, String]]
}

class NonEmptyRuleInterpreter[F[_] : Applicative] extends NonEmptyRule[F] {

  override def nonEmptyRun(s: String)(fieldName: String): F[Either[String, String]] =
    nonEmpty(s)(fieldName)
      .pure[F]

  private[util] def nonEmpty(s: String)(fieldName: String): Either[String, String] =
    Option(s)
      .toRight(s"input $fieldName can not be null")
      .filterOrElse(_.nonEmpty, s"input $fieldName can not be empty or contains only spaces")
}

object NonEmptyRule {
  implicit def nonEmptyRule[F[_] : Applicative]: NonEmptyRule[F] = new NonEmptyRuleInterpreter[F]
}
