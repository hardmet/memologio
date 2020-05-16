package ru.hardmet.memologio.entities

import java.time.OffsetDateTime

import derevo.circe.codec
import derevo.derive
import ru.tinkoff.tschema.swagger.Swagger

@derive(codec, Swagger)
final case class Entity[Id, Data](
    id: Id,
    data: Data,
    created: ActionInfo,
    modified: Option[ActionInfo] = None,
)

@derive(codec, Swagger)
final case class ActionInfo(date: OffsetDateTime, user: Option[String])
