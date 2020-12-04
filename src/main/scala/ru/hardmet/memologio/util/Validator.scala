package ru.hardmet.memologio
package util

object Validator {

  def nonEmptyCheck(s: String)(fieldName: String): Either[String, String] =
    Option(s)
      .toRight(s"input $fieldName can not be null")
      .filterOrElse(!_.isEmpty, s"input $fieldName can not be empty or contains only spaces")

}
