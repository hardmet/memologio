package ru.hardmet.memologio.entities

import java.time.OffsetDateTime

import ru.hardmet.memologio.Memologio
import zio.clock._

object Entities {
  def makeEntity[Id, Data](id: Id, data: Data): Memologio[Entity[Id, Data]] =
    currentDateTime.map(dt => Entity(id, data, created = ActionInfo(dt, None)))

  def initEntity[Id, Data](id: Id, data: Data): Entity[Id, Data] =
    Entity(id, data, created = ActionInfo(OffsetDateTime.MIN, None))
}
