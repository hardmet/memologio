package ru.hardmet.memologio
package util

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import cats.syntax.all._

trait Parse[-From, +To] extends Function1[From, Either[String, To]]

object Parse {
  implicit val parseStringToUUID: Parse[String, UUID] = string =>
    Either.catchNonFatal(UUID.fromString(string))
      .leftMap { t =>
        s"passed id '$string' is wrong, reason: ${t.getMessage}."
      }

  implicit val parseStringToInt: Parse[String, Int] = string =>
    Either.catchNonFatal(string.toInt).leftMap { t =>
      s"""Attempt to convert "$string" to Int failed, reason: ${t.getMessage}"""
    }

  implicit val parseStringToLocalDate: Parse[String, LocalDate] = str => DateParser.parseLocalDate(str)

  implicit val parseStringToLocalDateTime: Parse[String, LocalDateTime] = str => DateParser.parseLocalDateTime(str)

}
