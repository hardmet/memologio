package ru.hardmet.memologio
package http
package routes

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
}
