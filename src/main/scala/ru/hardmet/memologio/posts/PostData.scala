package ru.hardmet.memologio
package posts

import java.util.UUID

import derevo.circe.codec
import derevo.derive
import derevo.pureconfig.config
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.swagger.{AsOpenApiParam, Swagger}
import tofu.logging.derivation.loggable

@derive(codec, Swagger, config, loggable)
final case class PostData(source: String, tags: List[TagData])

@derive(codec, Swagger, loggable, HttpParam, AsOpenApiParam)
final case class PostId(uuid: UUID)

@derive(codec, Swagger, config, loggable, AsOpenApiParam)
final case class TagData(uuid: Option[UUID] = None, name: String, postId: UUID)

@derive(codec, Swagger, loggable, HttpParam, AsOpenApiParam)
final case class TagId(uuid: UUID)