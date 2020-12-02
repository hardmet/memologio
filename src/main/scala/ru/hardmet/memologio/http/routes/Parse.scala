package ru.hardmet.memologio
package http
package routes

import java.util.UUID

import cats.syntax.all._

trait Parse[-From, +To] extends Function1[From, Either[Throwable, To]]

object Parse {
  implicit val parseStringToUUID: Parse[String, UUID] = string =>
    Either.catchNonFatal(UUID.fromString(string))
      .leftMap { t =>
        throw new IllegalArgumentException(s"passed id '$string' is wrong, reason: ${t.getMessage}.", t)
      }

  implicit val parseStringToInt: Parse[String, Int] = string =>
    Either.catchNonFatal(string.toInt).leftMap { cause =>
      new IllegalArgumentException(
        s"""Attempt to convert "$string" to Int failed.""",
        cause
      )
    }
}
