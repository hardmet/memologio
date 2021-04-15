package ru.hardmet.memologio
package util

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import cats.syntax.either._

object DateParser {

  val DefaultDatePattern = "yyyy-MM-dd"
  val DefaultDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"

  val DefaultLocalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DefaultDatePattern)
  val DefaultLocalDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DefaultDateTimePattern)

  def parseLocalDate(input: String): Either[String, LocalDate] =
    parseDateByFormatter(DefaultDatePattern, DefaultLocalDateFormatter)(input)

  def parseLocalDateTime(input: String): Either[String, LocalDateTime] =
    parseDateTimeByFormatter(DefaultDateTimePattern, DefaultLocalDateTimeFormatter)(input)


  private[util] def parseDateByFormatter(pattern: String, formatter: DateTimeFormatter)
                                        (input: String): Either[String, LocalDate] =
    Either
      .catchNonFatal(LocalDate.parse(input, formatter))
      .leftMap(_ => parsingErrorMessage(input, pattern))

  private[util] def parseDateTimeByFormatter(pattern: String, formatter: DateTimeFormatter)
                                            (input: String): Either[String, LocalDateTime] =
    Either
      .catchNonFatal(LocalDateTime.parse(input, formatter))
      .leftMap(_ => parsingErrorMessage(input, pattern))

  private[util] def parsingErrorMessage(input: String, pattern: String) =
    s"input date: $input does not match the required format '$pattern'"

}
