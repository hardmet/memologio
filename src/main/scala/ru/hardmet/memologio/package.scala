package ru.hardmet

import java.util.UUID

import tofu.logging.Loggable

package object memologio {
  implicit val UUIDLoggable: Loggable[UUID] = Loggable[String].contramap(uuid => uuid.toString)
}
