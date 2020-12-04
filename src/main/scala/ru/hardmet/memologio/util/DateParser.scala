package ru.hardmet.memologio
package util

import cats.syntax.all._
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

class DateParser(val datePattern: String, val dateTimePattern: String) {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)
  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)

  def parseLocalDate(input: String)(errorMessage: String = ""): Either[String, LocalDate] =
    Either
      .catchNonFatal(LocalDate.parse(input, dateFormatter))
      .leftMap{ _ =>
        val errorMessageConnector = if (errorMessage.nonEmpty) " and" else s"input: $input"
        s"$errorMessage$errorMessageConnector does not match the required format '$datePattern'"
      }

  def parseLocalDateTime(input: String): Either[String, LocalDateTime] =
    Either
      .catchNonFatal(LocalDateTime.parse(input, dateTimeFormatter))
      .leftMap(_ => s"$input does not match the required format '$dateTimePattern'")

}

object DateParser {

  val DefaultLocalDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  def apply(datePattern: String, dateTimePattern: String): DateParser = new DateParser(datePattern, dateTimePattern)

  def parseLocalDateTime(input: String): Either[String, LocalDateTime] =
    Either
      .catchNonFatal(LocalDateTime.parse(input, DefaultLocalDateTimeFormatter))
      .leftMap(_ => s"$input does not match the required format 'yyyy-MM-ddTHH:mm:ssZ'")
}
