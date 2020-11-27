package ru.hardmet.memologio
package domain
package posts

import derevo.cats.show
import derevo.circe.codec
import derevo.derive
import ru.hardmet.memologio.MemologioError
import ru.tinkoff.tschema.custom.derivation._
import ru.tinkoff.tschema.swagger.Swagger

sealed abstract class PostError(message: String) extends MemologioError(message)

@derive(show, Swagger, plainError(409))
final case class AlreadyExists(name: String) extends PostError(s"Post with name $name already exists")

@derive(codec, Swagger, jsonError(404))
final case class NotFound(name: String) extends PostError(s"Post with name $name does not found")

@derive(codec, Swagger)
final case class CreateTableFailed() extends PostError(s"can't create Post table")
