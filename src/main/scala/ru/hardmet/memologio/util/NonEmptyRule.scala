package ru.hardmet.memologio
package util

import cats.Monad
import cats.implicits.toTraverseOps

trait NonEmptyRule[F[_]] {
  def nonEmptyApply(s: String)(fieldName: String): F[Either[String, String]]
}

class NonEmptyRuleInterpreter[F[_]: Monad] extends NonEmptyRule[F] {

  override def nonEmptyApply(s: String)(fieldName: String): F[Either[String, String]] =
    Option(s)
      .toRight(s"input $fieldName can not be null")
      .filterOrElse(!_.isEmpty, s"input $fieldName can not be empty or contains only spaces")
      .traverse(F.pure)
}
